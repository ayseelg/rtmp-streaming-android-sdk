package com.example.rtmp.sdk.di

import com.example.rtmp.sdk.data.repository.*
import com.example.rtmp.sdk.data.source.FirebaseDataSource
import com.example.rtmp.sdk.domain.repository.*
import com.example.rtmp.sdk.domain.usecase.*

/**
 * Simple Dependency Injection container
 * Provides singleton instances of repositories and use cases
 */
object DependencyContainer {
    
    // ============== Data Sources ==============
    
    private val firebaseDataSource: FirebaseDataSource by lazy {
        FirebaseDataSource()
    }
    
    // ============== Repositories ==============
    
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseDataSource)
    }
    
    val streamRepository: StreamRepository by lazy {
        StreamRepositoryImpl(firebaseDataSource, authRepository)
    }
    
    val viewerRepository: ViewerRepository by lazy {
        ViewerRepositoryImpl(firebaseDataSource, authRepository)
    }
    
    // ============== Use Cases ==============
    
    val registerUserUseCase: RegisterUserUseCase by lazy {
        RegisterUserUseCase(authRepository)
    }
    
    val loginUserUseCase: LoginUserUseCase by lazy {
        LoginUserUseCase(authRepository)
    }
    
    val logoutUserUseCase: LogoutUserUseCase by lazy {
        LogoutUserUseCase(authRepository)
    }

    val resetPasswordUseCase: ResetPasswordUseCase by lazy {
        ResetPasswordUseCase(authRepository)
    }
    
    val createLiveStreamUseCase: CreateLiveStreamUseCase by lazy {
        CreateLiveStreamUseCase(streamRepository)
    }
    
    val endLiveStreamUseCase: EndLiveStreamUseCase by lazy {
        EndLiveStreamUseCase(streamRepository)
    }
    
    val deleteStreamUseCase: DeleteStreamUseCase by lazy {
        DeleteStreamUseCase(streamRepository, authRepository)
    }
    
    val observeAllStreamsUseCase: ObserveAllStreamsUseCase by lazy {
        ObserveAllStreamsUseCase(streamRepository)
    }
    
    val joinStreamUseCase: JoinStreamUseCase by lazy {
        JoinStreamUseCase(viewerRepository)
    }
    
    val leaveStreamUseCase: LeaveStreamUseCase by lazy {
        LeaveStreamUseCase(viewerRepository)
    }
    
    val observeViewerCountUseCase: ObserveViewerCountUseCase by lazy {
        ObserveViewerCountUseCase(viewerRepository)
    }
    
    val observeViewersUseCase: ObserveViewersUseCase by lazy {
        ObserveViewersUseCase(viewerRepository)
    }

    val sendChatMessageUseCase: SendChatMessageUseCase by lazy {
        SendChatMessageUseCase(viewerRepository)
    }

    val observeChatMessagesUseCase: ObserveChatMessagesUseCase by lazy {
        ObserveChatMessagesUseCase(viewerRepository)
    }
}
