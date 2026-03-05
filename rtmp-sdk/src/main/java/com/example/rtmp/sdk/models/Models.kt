package com.example.rtmp.sdk.models

data class User(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Viewer(
    val viewerId: String = "",
    val streamId: String = "",
    val userName: String = "",
    val joinedAt: Long = System.currentTimeMillis()
)

data class LiveStream(
    var streamId: String = "",
    var userId: String = "",
    var userName: String = "",
    var title: String = "",
    var rtmpUrl: String = "",
    var streamKey: String = "",
    var isLive: Boolean = true,
    var viewerCount: Int = 0,
    var startedAt: Long = System.currentTimeMillis(),
    var endedAt: Long = 0
)

data class ChatMessage(
    val messageId: String = "",
    val streamId: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val sentAt: Long = System.currentTimeMillis()
)
