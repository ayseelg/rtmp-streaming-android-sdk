package com.example.rtmp.sdk.data.repository

import com.example.rtmp.sdk.data.source.FirebaseDataSource
import com.example.rtmp.sdk.domain.repository.*
import com.example.rtmp.sdk.models.*
import com.google.firebase.database.DatabaseReference

class AuthRepositoryImpl(
    private val firebaseDataSource: FirebaseDataSource
) : AuthRepository {
    override suspend fun registerUser(phoneNumber: String, password: String, firstName: String, lastName: String): Result<User> =
        firebaseDataSource.registerUser(phoneNumber, password, firstName, lastName)
    override suspend fun loginUser(phoneNumber: String, password: String): Result<User> =
        firebaseDataSource.loginUser(phoneNumber, password)
    override suspend fun getUserData(userId: String): User? =
        firebaseDataSource.getUserData(userId)
    override fun getCurrentUserId(): String? = firebaseDataSource.getCurrentUserId()
    override fun isUserLoggedIn(): Boolean = firebaseDataSource.isUserLoggedIn()
    override fun signOut() = firebaseDataSource.signOut()
    override suspend fun resetPassword(phoneNumber: String): Result<Boolean> =
        firebaseDataSource.resetPassword(phoneNumber)
}

class StreamRepositoryImpl(
    private val firebaseDataSource: FirebaseDataSource,
    private val authRepository: AuthRepository
) : StreamRepository {
    override suspend fun createLiveStream(title: String, rtmpUrl: String, streamKey: String): Result<LiveStream> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("Kullanıcı bulunamadı"))
        val user = authRepository.getUserData(userId)
            ?: return Result.failure(Exception("Kullanıcı bilgileri alınamadı"))
        return firebaseDataSource.createLiveStream(title, rtmpUrl, streamKey, userId, "${user.firstName} ${user.lastName}")
    }
    override suspend fun endLiveStream(streamId: String): Result<Boolean> =
        firebaseDataSource.endLiveStream(streamId)
    override suspend fun deleteStream(streamId: String): Result<Boolean> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("Kullanıcı bulunamadı"))
        return firebaseDataSource.deleteStream(streamId, userId)
    }
    override suspend fun getStreamData(streamId: String): LiveStream? =
        firebaseDataSource.getStreamData(streamId)
    override fun observeAllStreams(callback: (List<LiveStream>) -> Unit): DatabaseReference =
        firebaseDataSource.observeAllStreams(callback)
    override fun observeLiveStreams(callback: (List<LiveStream>) -> Unit): DatabaseReference =
        firebaseDataSource.observeLiveStreams(callback)
}

class ViewerRepositoryImpl(
    private val firebaseDataSource: FirebaseDataSource,
    private val authRepository: AuthRepository
) : ViewerRepository {
    override suspend fun joinStream(streamId: String): Result<Boolean> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("Kullanıcı bulunamadı"))
        val user = authRepository.getUserData(userId)
            ?: return Result.failure(Exception("Kullanıcı bilgileri alınamadı"))
        return firebaseDataSource.joinStream(streamId, userId, "${user.firstName} ${user.lastName}")
    }
    override suspend fun leaveStream(streamId: String): Result<Boolean> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("Kullanıcı bulunamadı"))
        return firebaseDataSource.leaveStream(streamId, userId)
    }
    override fun observeViewerCount(streamId: String, callback: (Int) -> Unit): DatabaseReference =
        firebaseDataSource.observeViewerCount(streamId, callback)
    override fun observeViewers(streamId: String, callback: (List<Viewer>) -> Unit): DatabaseReference =
        firebaseDataSource.observeViewers(streamId, callback)
    override suspend fun sendChatMessage(streamId: String, text: String): Result<Boolean> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("Kullanıcı bulunamadı"))
        val user = authRepository.getUserData(userId)
            ?: return Result.failure(Exception("Kullanıcı bilgileri alınamadı"))
        return firebaseDataSource.sendChatMessage(streamId, userId, "${user.firstName} ${user.lastName}", text)
    }
    override fun observeChatMessages(streamId: String, callback: (List<ChatMessage>) -> Unit): DatabaseReference =
        firebaseDataSource.observeChatMessages(streamId, callback)
}
