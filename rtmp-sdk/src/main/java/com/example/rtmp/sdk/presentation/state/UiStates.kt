package com.example.rtmp.sdk.presentation.state

import com.example.rtmp.sdk.models.LiveStream
import com.example.rtmp.sdk.models.User

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class ResetPasswordState {
    object Loading : ResetPasswordState()
    object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}

sealed class BroadcastState {
    object Idle : BroadcastState()
    object Preparing : BroadcastState()
    object Connecting : BroadcastState()
    data class Streaming(val streamId: String) : BroadcastState()
    data class Error(val message: String) : BroadcastState()
    object Stopped : BroadcastState()
}

sealed class ViewerState {
    object Idle : ViewerState()
    object Joining : ViewerState()
    object Buffering : ViewerState()
    data class Playing(val viewerCount: Int = 0) : ViewerState()
    data class Error(val message: String) : ViewerState()
    object Ended : ViewerState()
}

sealed class StreamListState {
    object Idle : StreamListState()
    object Loading : StreamListState()
    data class Success(val streams: List<LiveStream>) : StreamListState()
    object Empty : StreamListState()
    data class Error(val message: String) : StreamListState()
}

data class ValidationState(
    val phoneNumberError: String? = null,
    val passwordError: String? = null,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val confirmPasswordError: String? = null,
    val titleError: String? = null,
    val rtmpUrlError: String? = null,
    val streamKeyError: String? = null
) {
    fun hasErrors() = phoneNumberError != null || passwordError != null ||
        firstNameError != null || lastNameError != null ||
        confirmPasswordError != null || titleError != null ||
        rtmpUrlError != null || streamKeyError != null

    fun isValid() = !hasErrors()
}
