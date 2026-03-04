package com.example.rtmp.sdk.models

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
