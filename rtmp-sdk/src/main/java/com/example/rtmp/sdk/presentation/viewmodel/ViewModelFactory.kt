package com.example.rtmp.sdk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.rtmp.sdk.di.DependencyContainer


class ViewModelFactory : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(
                    DependencyContainer.loginUserUseCase,
                    DependencyContainer.resetPasswordUseCase
                ) as T
            }
            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                RegisterViewModel(
                    DependencyContainer.registerUserUseCase
                ) as T
            }
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(
                    DependencyContainer.observeAllStreamsUseCase,
                    DependencyContainer.deleteStreamUseCase,
                    DependencyContainer.logoutUserUseCase
                ) as T
            }
            modelClass.isAssignableFrom(BroadcastViewModel::class.java) -> {
                BroadcastViewModel(
                    DependencyContainer.createLiveStreamUseCase,
                    DependencyContainer.endLiveStreamUseCase,
                    DependencyContainer.observeViewerCountUseCase,
                    DependencyContainer.observeViewersUseCase,
                    DependencyContainer.observeChatMessagesUseCase
                ) as T
            }
            modelClass.isAssignableFrom(ViewerViewModel::class.java) -> {
                ViewerViewModel(
                    DependencyContainer.joinStreamUseCase,
                    DependencyContainer.leaveStreamUseCase,
                    DependencyContainer.observeViewerCountUseCase,
                    DependencyContainer.sendChatMessageUseCase,
                    DependencyContainer.observeChatMessagesUseCase
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
