package com.example.rtmp.sdk.domain.repository

import com.example.rtmp.sdk.models.LiveStream
import com.example.rtmp.sdk.models.User
import com.example.rtmp.sdk.models.Viewer
import com.example.rtmp.sdk.models.ChatMessage
import com.google.firebase.database.DatabaseReference

interface AuthRepository {
    suspend fun registerUser(phoneNumber: String, password: String, firstName: String, lastName: String): Result<User>
    suspend fun loginUser(phoneNumber: String, password: String): Result<User>
    suspend fun getUserData(userId: String): User?
    fun getCurrentUserId(): String?
    fun isUserLoggedIn(): Boolean
    fun signOut()
    suspend fun resetPassword(phoneNumber: String): Result<Boolean>
}

interface StreamRepository {
    suspend fun createLiveStream(title: String, rtmpUrl: String, streamKey: String): Result<LiveStream>
    suspend fun endLiveStream(streamId: String): Result<Boolean>
    suspend fun deleteStream(streamId: String): Result<Boolean>
    suspend fun getStreamData(streamId: String): LiveStream?
    fun observeAllStreams(callback: (List<LiveStream>) -> Unit): DatabaseReference
    fun observeLiveStreams(callback: (List<LiveStream>) -> Unit): DatabaseReference
}

interface ViewerRepository {
    suspend fun joinStream(streamId: String): Result<Boolean>
    suspend fun leaveStream(streamId: String): Result<Boolean>
    fun observeViewerCount(streamId: String, callback: (Int) -> Unit): DatabaseReference
    fun observeViewers(streamId: String, callback: (List<Viewer>) -> Unit): DatabaseReference
    suspend fun sendChatMessage(streamId: String, text: String): Result<Boolean>
    fun observeChatMessages(streamId: String, callback: (List<ChatMessage>) -> Unit): DatabaseReference
}
