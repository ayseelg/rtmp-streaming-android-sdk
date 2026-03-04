package com.example.rtmp.sdk.models

data class Viewer(
    val viewerId: String = "",
    val streamId: String = "",
    val userName: String = "",
    val joinedAt: Long = System.currentTimeMillis()
)
