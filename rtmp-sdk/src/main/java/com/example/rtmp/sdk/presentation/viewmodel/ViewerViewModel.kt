package com.example.rtmp.sdk.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rtmp.sdk.domain.usecase.*
import com.example.rtmp.sdk.models.ChatMessage
import com.example.rtmp.sdk.presentation.state.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

/**
 * ViewModel for Viewer screen
 * Handles stream viewing business logic and UI state
 */
class ViewerViewModel(
    private val joinStreamUseCase: JoinStreamUseCase,
    private val leaveStreamUseCase: LeaveStreamUseCase,
    private val observeViewerCountUseCase: ObserveViewerCountUseCase,
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val observeChatMessagesUseCase: ObserveChatMessagesUseCase
) : ViewModel() {
    
    private val _viewerState = MutableLiveData<ViewerState>(ViewerState.Idle)
    val viewerState: LiveData<ViewerState> = _viewerState
    
    private val _viewerCount = MutableLiveData<Int>(0)
    val viewerCount: LiveData<Int> = _viewerCount

    private val _chatMessages = MutableLiveData<List<ChatMessage>>(emptyList())
    val chatMessages: LiveData<List<ChatMessage>> = _chatMessages
    
    private var viewerCountListener: DatabaseReference? = null
    private var chatListener: DatabaseReference? = null
    private var streamStatusListener: DatabaseReference? = null
    private var currentStreamId: String? = null
    
    /**
     * Join a stream and start viewing
     */
    fun joinStream(streamId: String) {
        currentStreamId = streamId
        _viewerState.value = ViewerState.Joining
        
        viewModelScope.launch {
            val result = joinStreamUseCase(streamId)
            
            if (result.isSuccess) {
                startObservingViewerCount(streamId)
                _viewerState.value = ViewerState.Buffering
            } else {
                val error = result.exceptionOrNull()?.message ?: "Yayına katılınamadı"
                _viewerState.value = ViewerState.Error(error)
            }
        }
    }
    
    /**
     * Leave current stream
     */
    fun leaveStream() {
        currentStreamId?.let { streamId ->
            viewModelScope.launch {
                leaveStreamUseCase(streamId)
                stopObservingViewerCount()
                currentStreamId = null
            }
        }
    }
    
    /**
     * Update state when playback is buffering
     */
    fun onBuffering() {
        _viewerState.value = ViewerState.Buffering
    }
    
    /**
     * Update state when playback starts
     */
    fun onPlaying() {
        _viewerState.value = ViewerState.Playing(_viewerCount.value ?: 0)
    }
    
    /**
     * Update state when playback ends
     */
    fun onEnded() {
        _viewerState.value = ViewerState.Ended
        leaveStream()
    }
    
    /**
     * Update state when playback fails
     */
    fun onError(message: String) {
        _viewerState.value = ViewerState.Error(message)
        leaveStream()
    }
    
    /**
     * Start observing viewer count
     */
    private fun startObservingViewerCount(streamId: String) {
        viewerCountListener = observeViewerCountUseCase(streamId) { count ->
            _viewerCount.postValue(count)
            val currentState = _viewerState.value
            if (currentState is ViewerState.Playing) {
                _viewerState.postValue(ViewerState.Playing(count))
            }
        }
        chatListener = observeChatMessagesUseCase(streamId) { messages ->
            _chatMessages.postValue(messages)
        }
        // Yayın bitince otomatik çıkış
        val streamRef = FirebaseDatabase.getInstance().getReference("streams/$streamId/endedAt")
        streamStatusListener = streamRef
        streamRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val endedAt = snapshot.getValue(Long::class.java) ?: 0L
                if (endedAt > 0L) {
                    val state = _viewerState.value
                    if (state !is ViewerState.Ended) {
                        onEnded()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    
    /**
     * Stop observing viewer count
     */
    private fun stopObservingViewerCount() {
        viewerCountListener = null
        chatListener = null
        streamStatusListener = null
    }

    /**
     * Send a chat message
     */
    fun sendMessage(text: String) {
        val streamId = currentStreamId ?: return
        viewModelScope.launch {
            sendChatMessageUseCase(streamId, text)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        leaveStream()
    }

    fun handleError(message: String) {
        _viewerState.postValue(ViewerState.Error(message))
    }

    fun updateState(newState: ViewerState) {
        _viewerState.postValue(newState)
    }
}
