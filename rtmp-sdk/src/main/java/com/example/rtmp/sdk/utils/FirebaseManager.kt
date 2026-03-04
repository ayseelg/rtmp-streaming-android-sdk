package com.example.rtmp.sdk.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.rtmp.sdk.models.User
import com.example.rtmp.sdk.models.LiveStream
import com.example.rtmp.sdk.models.Viewer
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    
    suspend fun registerUser(phoneNumber: String, password: String, firstName: String, lastName: String): Result<User> {
        return try {
            // Firebase Auth için email formatına dönüştür
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
    
    suspend fun getStreamData(streamId: String): LiveStream? {
        return try {
            val snapshot = database.child("streams").child(streamId).get().await()
            snapshot.getValue(LiveStream::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun createLiveStream(title: String, rtmpUrl: String, streamKey: String): Result<LiveStream> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("Kullanıcı bulunamadı")
            val user = getUserData(userId) ?: throw Exception("Kullanıcı bilgileri alınamadı")
            val streamId = database.child("streams").push().key ?: throw Exception("Stream ID oluşturulamadı")
            
            val stream = LiveStream(
                streamId = streamId,
                userId = userId,
                userName = "${user.firstName} ${user.lastName}",
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
                "streams/$streamId/isLive" to false,
                "streams/$streamId/endedAt" to System.currentTimeMillis()
            )
            database.updateChildren(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Tüm yayınları gözlemle (canlı + geçmiş)
    fun observeAllStreams(callback: (List<LiveStream>) -> Unit): DatabaseReference {
        val streamsRef = database.child("streams")
        
        android.util.Log.d("FirebaseManager", "═══════════════════════════════════════")
        android.util.Log.d("FirebaseManager", "🔍 observeAllStreams çağrıldı (canlı + geçmiş)")
        android.util.Log.d("FirebaseManager", "Database ref: ${streamsRef.path}")
        android.util.Log.d("FirebaseManager", "═══════════════════════════════════════")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("FirebaseManager", "─────────────────────────────────────")
                android.util.Log.d("FirebaseManager", "📥 onDataChange tetiklendi")
                android.util.Log.d("FirebaseManager", "Snapshot exists: ${snapshot.exists()}")
                android.util.Log.d("FirebaseManager", "Snapshot childCount: ${snapshot.childrenCount}")
                
                val streams = mutableListOf<LiveStream>()
                var totalCount = 0
                var liveCount = 0
                var pastCount = 0
                
                for (childSnapshot in snapshot.children) {
                    totalCount++
                    android.util.Log.d("FirebaseManager", "  Stream ${totalCount}: key=${childSnapshot.key}")
                    
                    val stream = childSnapshot.getValue(LiveStream::class.java)
                    android.util.Log.d("FirebaseManager", "    Parsed: $stream")
                    
                    if (stream != null) {
                        android.util.Log.d("FirebaseManager", "    isLive: ${stream.isLive}")
                        android.util.Log.d("FirebaseManager", "    title: ${stream.title}")
                        android.util.Log.d("FirebaseManager", "    userName: ${stream.userName}")
                        
                        if (stream.isLive) liveCount++ else pastCount++
                        streams.add(stream)
                        android.util.Log.i("FirebaseManager", "    ✅ Eklendi!")
                    } else {
                        android.util.Log.e("FirebaseManager", "    ❌ Stream null!")
                    }
                }
                
                // Canlı yayınlar önce, sonra geçmiş yayınlar (tarihe göre)
                val sorted = streams.sortedWith(compareByDescending<LiveStream> { it.isLive }.thenByDescending { it.startedAt })
                
                android.util.Log.d("FirebaseManager", "─────────────────────────────────────")
                android.util.Log.i("FirebaseManager", "📊 Sonuç: Toplam=$totalCount, Canlı=$liveCount, Geçmiş=$pastCount")
                android.util.Log.d("FirebaseManager", "─────────────────────────────────────")
                
                callback(sorted)
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseManager", "═══════════════════════════════════════")
                android.util.Log.e("FirebaseManager", "❌ FIREBASE OKUMA HATASI!")
                android.util.Log.e("FirebaseManager", "Error: ${error.message}")
                android.util.Log.e("FirebaseManager", "Code: ${error.code}")
                android.util.Log.e("FirebaseManager", "Details: ${error.details}")
                android.util.Log.e("FirebaseManager", "═══════════════════════════════════════")
                callback(emptyList())
            }
        }
        streamsRef.addValueEventListener(listener)
        return streamsRef
    }
    
    // Sadece canlı yayınları gözlemle (eski fonksiyon)
    fun observeLiveStreams(callback: (List<LiveStream>) -> Unit): DatabaseReference {
        val streamsRef = database.child("streams")
        
        android.util.Log.d("FirebaseManager", "═══════════════════════════════════════")
        android.util.Log.d("FirebaseManager", "🔍 observeLiveStreams çağrıldı")
        android.util.Log.d("FirebaseManager", "Database ref: ${streamsRef.path}")
        android.util.Log.d("FirebaseManager", "═══════════════════════════════════════")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("FirebaseManager", "─────────────────────────────────────")
                android.util.Log.d("FirebaseManager", "📥 onDataChange tetiklendi")
                android.util.Log.d("FirebaseManager", "Snapshot exists: ${snapshot.exists()}")
                android.util.Log.d("FirebaseManager", "Snapshot childCount: ${snapshot.childrenCount}")
                
                val streams = mutableListOf<LiveStream>()
                var totalCount = 0
                var liveCount = 0
                
                for (childSnapshot in snapshot.children) {
                    totalCount++
                    android.util.Log.d("FirebaseManager", "  Stream ${totalCount}: key=${childSnapshot.key}")
                    
                    val stream = childSnapshot.getValue(LiveStream::class.java)
                    android.util.Log.d("FirebaseManager", "    Parsed: $stream")
                    
                    if (stream != null) {
                        android.util.Log.d("FirebaseManager", "    isLive: ${stream.isLive}")
                        android.util.Log.d("FirebaseManager", "    title: ${stream.title}")
                        android.util.Log.d("FirebaseManager", "    userName: ${stream.userName}")
                        
                        if (stream.isLive) {
                            liveCount++
                            streams.add(stream)
                            android.util.Log.i("FirebaseManager", "    ✅ Eklendi!")
                        } else {
                            android.util.Log.w("FirebaseManager", "    ⚠️ isLive=false, atlandı")
                        }
                    } else {
                        android.util.Log.e("FirebaseManager", "    ❌ Stream null!")
                    }
                }
                
                android.util.Log.d("FirebaseManager", "─────────────────────────────────────")
                android.util.Log.i("FirebaseManager", "📊 Sonuç: Toplam=$totalCount, Canlı=$liveCount")
                android.util.Log.d("FirebaseManager", "─────────────────────────────────────")
                
                callback(streams)
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseManager", "═══════════════════════════════════════")
                android.util.Log.e("FirebaseManager", "❌ FIREBASE OKUMA HATASI!")
                android.util.Log.e("FirebaseManager", "Error: ${error.message}")
                android.util.Log.e("FirebaseManager", "Code: ${error.code}")
                android.util.Log.e("FirebaseManager", "Details: ${error.details}")
                android.util.Log.e("FirebaseManager", "═══════════════════════════════════════")
                callback(emptyList())
            }
        }
        streamsRef.addValueEventListener(listener)
        return streamsRef
    }
    
    suspend fun joinStream(streamId: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("Kullanıcı bulunamadı")
            val user = getUserData(userId) ?: throw Exception("Kullanıcı bilgileri alınamadı")
            
            val viewer = Viewer(
                viewerId = userId,
                streamId = streamId,
                userName = "${user.firstName} ${user.lastName}"
            )
            
            database.child("viewers").child(streamId).child(userId).setValue(viewer).await()
            
            // İzleyici sayısını artır
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
    
    suspend fun leaveStream(streamId: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("Kullanıcı bulunamadı")
            
            database.child("viewers").child(streamId).child(userId).removeValue().await()
            
            // İzleyici sayısını azalt
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
    
    // İzleyici sayısını gözlemle (real-time)
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
    
    // İzleyici listesini gözlemle (real-time)
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
    
    // Yayın silme
    suspend fun deleteStream(streamId: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("Kullanıcı bulunamadı")
            
            database.child("streams").child(streamId).removeValue().await()
            database.child("user_streams").child(userId).child(streamId).removeValue().await()
            database.child("viewers").child(streamId).removeValue().await()
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
}
