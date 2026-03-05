package com.example.rtmp.sdk.domain.usecase

import com.example.rtmp.sdk.domain.repository.*
import com.example.rtmp.sdk.models.*
import com.google.firebase.database.DatabaseReference

class JoinStreamUseCase(private val viewerRepository: ViewerRepository) {
    suspend operator fun invoke(streamId: String): Result<Boolean> {
        if (streamId.isBlank()) return Result.failure(IllegalArgumentException("Stream ID gerekli"))
        return viewerRepository.joinStream(streamId)
    }
}

class LeaveStreamUseCase(private val viewerRepository: ViewerRepository) {
    suspend operator fun invoke(streamId: String): Result<Boolean> {
        if (streamId.isBlank()) return Result.failure(IllegalArgumentException("Stream ID gerekli"))
        return viewerRepository.leaveStream(streamId)
    }
}

class ObserveViewerCountUseCase(private val viewerRepository: ViewerRepository) {
    operator fun invoke(streamId: String, callback: (Int) -> Unit): DatabaseReference {
        return viewerRepository.observeViewerCount(streamId, callback)
    }
}

class ObserveViewersUseCase(private val viewerRepository: ViewerRepository) {
    operator fun invoke(streamId: String, callback: (List<Viewer>) -> Unit): DatabaseReference {
        return viewerRepository.observeViewers(streamId, callback)
    }
}

class SendChatMessageUseCase(private val viewerRepository: ViewerRepository) {
    suspend operator fun invoke(streamId: String, text: String): Result<Boolean> {
        if (streamId.isBlank()) return Result.failure(IllegalArgumentException("Stream ID gerekli"))
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Mesaj boş olamaz"))
        return viewerRepository.sendChatMessage(streamId, text)
    }
}

class ObserveChatMessagesUseCase(private val viewerRepository: ViewerRepository) {
    operator fun invoke(streamId: String, callback: (List<ChatMessage>) -> Unit): DatabaseReference {
        return viewerRepository.observeChatMessages(streamId, callback)
    }
}
