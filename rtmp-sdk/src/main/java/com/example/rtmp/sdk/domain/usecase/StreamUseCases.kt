package com.example.rtmp.sdk.domain.usecase

import com.example.rtmp.sdk.domain.repository.*
import com.example.rtmp.sdk.models.*
import com.google.firebase.database.DatabaseReference

class CreateLiveStreamUseCase(private val streamRepository: StreamRepository) {
    suspend operator fun invoke(title: String, rtmpUrl: String, streamKey: String): Result<LiveStream> {
        if (title.isBlank()) return Result.failure(IllegalArgumentException("Yayın başlığı gerekli"))
        if (rtmpUrl.isBlank()) return Result.failure(IllegalArgumentException("RTMP URL gerekli"))
        if (!rtmpUrl.startsWith("rtmp://") && !rtmpUrl.startsWith("rtmps://"))
            return Result.failure(IllegalArgumentException("URL 'rtmp://' ile başlamalı"))
        if (streamKey.isBlank()) return Result.failure(IllegalArgumentException("Stream key gerekli"))
        return streamRepository.createLiveStream(title, rtmpUrl, streamKey)
    }
}

class EndLiveStreamUseCase(private val streamRepository: StreamRepository) {
    suspend operator fun invoke(streamId: String): Result<Boolean> {
        if (streamId.isBlank()) return Result.failure(IllegalArgumentException("Stream ID gerekli"))
        return streamRepository.endLiveStream(streamId)
    }
}

class DeleteStreamUseCase(
    private val streamRepository: StreamRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(streamId: String, streamUserId: String): Result<Boolean> {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId != streamUserId)
            return Result.failure(IllegalStateException("Sadece kendi yayınlarınızı silebilirsiniz"))
        return streamRepository.deleteStream(streamId)
    }
}

class ObserveAllStreamsUseCase(private val streamRepository: StreamRepository) {
    operator fun invoke(callback: (List<LiveStream>) -> Unit): DatabaseReference {
        return streamRepository.observeAllStreams(callback)
    }
}
