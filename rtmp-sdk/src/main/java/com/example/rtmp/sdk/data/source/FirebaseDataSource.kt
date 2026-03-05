package com.example.rtmp.sdk.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.rtmp.sdk.models.User
import com.example.rtmp.sdk.models.LiveStream
import com.example.rtmp.sdk.models.Viewer
import com.example.rtmp.sdk.models.ChatMessage
import kotlinx.coroutines.tasks.await

/**
 * Firebase data source implementation
 * Handles all Firebase operations (Auth and Realtime Database)
 */
class FirebaseDataSource {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    
    // ============== Auth Operations ==============
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    
    suspend fun registerUser(phoneNumber: String, password: String, firstName: String, lastName: String): Result<User> {
        return try {
            val email = "$phoneNumber@rtmp.app"
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("Kullanıcı oluşturulamadı")
            
            val user = User(
                userId = userId,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber
            )
            
            database.child("users").child(userId).setValue(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetPassword(phoneNumber: String): Result<Boolean> {
        return try {
            val email = "$phoneNumber@rtmp.app"
            auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(phoneNumber: String, password: String): Result<User> {
        return try {
            val email = "$phoneNumber@rtmp.app"
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("Giriş yapılamadı")
            
            val snapshot = database.child("users").child(userId).get().await()
            val user = snapshot.getValue(User::class.java) ?: throw Exception("Kullanıcı bulunamadı")
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserData(userId: String): User? {
        return try {
            val snapshot = database.child("users").child(userId).get().await()
            snapshot.getValue(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    // ============== Stream Operations ==============
    
    suspend fun getStreamData(streamId: String): LiveStream? {
        return try {
            val snapshot = database.child("streams").child(streamId).get().await()
            snapshot.getValue(LiveStream::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun createLiveStream(title: String, rtmpUrl: String, streamKey: String, userId: String, userName: String): Result<LiveStream> {
        return try {
            val streamId = database.child("streams").push().key ?: throw Exception("Stream ID oluşturulamadı")
            
            val stream = LiveStream(
                streamId = streamId,
                userId = userId,
                userName = userName,
                title = title,
                rtmpUrl = rtmpUrl,
                streamKey = streamKey,
                isLive = true,
                viewerCount = 0
            )
            
            database.child("streams").child(streamId).setValue(stream).await()
            database.child("user_streams").child(userId).child(streamId).setValue(true).await()
            
            Result.success(stream)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun endLiveStream(streamId: String): Result<Boolean> {
        return try {
            val updates = hashMapOf<String, Any>(
                "streams/$streamId/live" to false,
                "streams/$streamId/endedAt" to System.currentTimeMillis()
            )
            database.updateChildren(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeAllStreams(callback: (List<LiveStream>) -> Unit): DatabaseReference {
        val streamsRef = database.child("streams")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val streams = mutableListOf<LiveStream>()
                
                for (childSnapshot in snapshot.children) {
                    val stream = childSnapshot.getValue(LiveStream::class.java)
                    if (stream != null) {
                        streams.add(stream)
                    }
                }
                
                val sorted = streams.sortedWith(
                    compareByDescending<LiveStream> { it.isLive }.thenByDescending { it.startedAt }
                )
                callback(sorted)
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        }
        
        streamsRef.addValueEventListener(listener)
        return streamsRef
    }
    
    fun observeLiveStreams(callback: (List<LiveStream>) -> Unit): DatabaseReference {
        val streamsRef = database.child("streams")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val streams = mutableListOf<LiveStream>()
                
                for (childSnapshot in snapshot.children) {
                    val stream = childSnapshot.getValue(LiveStream::class.java)
                    if (stream != null && stream.isLive) {
                        streams.add(stream)
                    }
                }
                
                callback(streams)
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        }
        
        streamsRef.addValueEventListener(listener)
        return streamsRef
    }
    
    suspend fun deleteStream(streamId: String, userId: String): Result<Boolean> {
        return try {
            database.child("streams").child(streamId).removeValue().await()
            database.child("user_streams").child(userId).child(streamId).removeValue().await()
            database.child("viewers").child(streamId).removeValue().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============== Viewer Operations ==============
    
    suspend fun joinStream(streamId: String, userId: String, userName: String): Result<Boolean> {
        return try {
            val viewer = Viewer(
                viewerId = userId,
                streamId = streamId,
                userName = userName
            )
            
            database.child("viewers").child(streamId).child(userId).setValue(viewer).await()
            
            database.child("streams").child(streamId).child("viewerCount")
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val current = currentData.getValue(Int::class.java) ?: 0
                        currentData.value = current + 1
                        return Transaction.success(currentData)
                    }
                    
                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
                })
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun leaveStream(streamId: String, userId: String): Result<Boolean> {
        return try {
            database.child("viewers").child(streamId).child(userId).removeValue().await()
            
            database.child("streams").child(streamId).child("viewerCount")
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val current = currentData.getValue(Int::class.java) ?: 0
                        currentData.value = maxOf(0, current - 1)
                        return Transaction.success(currentData)
                    }
                    
                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
                })
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeViewerCount(streamId: String, callback: (Int) -> Unit): DatabaseReference {
        val viewerCountRef = database.child("streams").child(streamId).child("viewerCount")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.getValue(Int::class.java) ?: 0
                callback(count)
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(0)
            }
        }
        
        viewerCountRef.addValueEventListener(listener)
        return viewerCountRef
    }
    
    fun observeViewers(streamId: String, callback: (List<Viewer>) -> Unit): DatabaseReference {
        val viewersRef = database.child("viewers").child(streamId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val viewers = mutableListOf<Viewer>()
                
                for (childSnapshot in snapshot.children) {
                    val viewer = childSnapshot.getValue(Viewer::class.java)
                    if (viewer != null) {
                        viewers.add(viewer)
                    }
                }
                
                viewers.sortByDescending { it.joinedAt }
                callback(viewers)
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        }
        
        viewersRef.addValueEventListener(listener)
        return viewersRef
    }

    // ============== Chat Operations ==============

    suspend fun sendChatMessage(streamId: String, userId: String, userName: String, text: String): Result<Boolean> {
        return try {
            val chatRef = database.child("chat").child(streamId)
            val messageId = chatRef.push().key ?: return Result.failure(Exception("Mesaj ID oluşturulamadı"))
            val message = ChatMessage(
                messageId = messageId,
                streamId = streamId,
                userId = userId,
                userName = userName,
                text = text
            )
            chatRef.child(messageId).setValue(message).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeChatMessages(streamId: String, callback: (List<ChatMessage>) -> Unit): DatabaseReference {
        val chatRef = database.child("chat").child(streamId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()
                for (childSnapshot in snapshot.children) {
                    val msg = childSnapshot.getValue(ChatMessage::class.java)
                    if (msg != null) messages.add(msg)
                }
                messages.sortBy { it.sentAt }
                callback(messages)
            }
            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        }

        chatRef.addValueEventListener(listener)
        return chatRef
    }
}
