package com.example.rtmp.sdk.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rtmp.sdk.domain.usecase.*
import com.example.rtmp.sdk.models.*
import com.example.rtmp.sdk.presentation.state.*
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.launch

/**
 * ViewModel for Broadcast screen
 * Handles live streaming business logic and UI state
 */
class BroadcastViewModel(
    private val createLiveStreamUseCase: CreateLiveStreamUseCase,
    private val endLiveStreamUseCase: EndLiveStreamUseCase,
    private val observeViewerCountUseCase: ObserveViewerCountUseCase,
    private val observeViewersUseCase: ObserveViewersUseCase,
    private val observeChatMessagesUseCase: ObserveChatMessagesUseCase
) : ViewModel() {
    
    private val _broadcastState = MutableLiveData<BroadcastState>(BroadcastState.Idle)
    val broadcastState: LiveData<BroadcastState> = _broadcastState
    
    private val _validationState = MutableLiveData<ValidationState>(ValidationState())
    val validationState: LiveData<ValidationState> = _validationState
    
    private val _viewerCount = MutableLiveData<Int>(0)
    val viewerCount: LiveData<Int> = _viewerCount
    
    private val _viewers = MutableLiveData<List<Viewer>>(emptyList())
    val viewers: LiveData<List<Viewer>> = _viewers

    private val _chatMessages = MutableLiveData<List<ChatMessage>>(emptyList())
    val chatMessages: LiveData<List<ChatMessage>> = _chatMessages

    // Oluşturulan yayının ID'si – bağlantı başarılı olunca kullanılır
    private var pendingStreamId: String? = null
    // RTMP bağlantısı Firebase'den önce tamamlanırsa bekletme bayrağı
    private var connectionSucceededPending = false

    private var viewerCountListener: DatabaseReference? = null
    private var viewersListener: DatabaseReference? = null
    private var chatListener: DatabaseReference? = null
    
    /**
     * Validate broadcast form
     */
    fun validateBroadcastForm(title: String, rtmpUrl: String, streamKey: String): Boolean {
        val validation = validateInput(title, rtmpUrl, streamKey)
        _validationState.value = validation
        return validation.isValid()
    }
    
    /**
     * Create and start live stream
     */
    fun createStream(title: String, rtmpUrl: String, streamKey: String) {
        _broadcastState.value = BroadcastState.Preparing
        viewModelScope.launch {
            val result = createLiveStreamUseCase(title, rtmpUrl, streamKey)
            if (result.isSuccess) {
                pendingStreamId = result.getOrThrow().streamId
                // RTMP bağlantısı Firebase'den önce tamamlandıysa şimdi işle
                if (connectionSucceededPending) {
                    connectionSucceededPending = false
                    onConnectionSuccess()
                }
            } else {
                _broadcastState.postValue(
                    BroadcastState.Error(result.exceptionOrNull()?.message ?: "Yayın oluşturulamadı")
                )
            }
        }
    }
    
    /**
     * End current stream
     */
    fun endStream() {
        val streamId = pendingStreamId ?: return
        viewModelScope.launch {
            endLiveStreamUseCase(streamId)
            pendingStreamId = null
            _broadcastState.postValue(BroadcastState.Stopped)
        }
    }
    
    /**
     * Update state when RTMP connection starts
     */
    fun onConnectionStarted() {
        _broadcastState.value = BroadcastState.Connecting
    }
    
    /**
     * Update state when RTMP connection succeeds
     */
    fun onConnectionSuccess() {
        val streamId = pendingStreamId
        if (streamId == null) {
            // Firebase henüz hazır değil, createStream() bitince tekrar çağrılacak
            connectionSucceededPending = true
            return
        }
        _broadcastState.postValue(BroadcastState.Streaming(streamId))
        startObservingViewers(streamId)
    }
    
    /**
     * Update state when RTMP connection fails
     */
    fun onConnectionFailed(reason: String) {
        viewModelScope.launch {
            pendingStreamId?.let { endLiveStreamUseCase(it) }
            pendingStreamId = null
            _broadcastState.postValue(BroadcastState.Error("Bağlantı hatası: $reason"))
        }
    }
    
    /**
     * Start observing viewer count and list
     */
    private fun startObservingViewers(streamId: String) {
        viewerCountListener = observeViewerCountUseCase(streamId) { count ->
            _viewerCount.postValue(count)
        }
        
        viewersListener = observeViewersUseCase(streamId) { viewers ->
            _viewers.postValue(viewers)
        }

        chatListener = observeChatMessagesUseCase(streamId) { messages ->
            _chatMessages.postValue(messages)
        }
    }
    
    /**
     * Stop observing viewers
     */
    private fun stopObservingViewers() {
        // Firebase listeners will be automatically cleaned up when ViewModel is destroyed
        viewerCountListener = null
        viewersListener = null
        chatListener = null
    }
    
    /**
     * Validate broadcast form input
     */
    private fun validateInput(title: String, rtmpUrl: String, streamKey: String): ValidationState {
        val titleError = if (title.isBlank()) "Yayın başlığı gerekli" else null
        val urlError = when {
            rtmpUrl.isBlank() -> "RTMP URL gerekli"
            !rtmpUrl.startsWith("rtmp://") && !rtmpUrl.startsWith("rtmps://") -> 
                "URL 'rtmp://' ile başlamalı"
            else -> null
        }
        val keyError = if (streamKey.isBlank()) "Stream key gerekli" else null
        
        return ValidationState(
            titleError = titleError,
            rtmpUrlError = urlError,
            streamKeyError = keyError
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        stopObservingViewers()
    }
}
