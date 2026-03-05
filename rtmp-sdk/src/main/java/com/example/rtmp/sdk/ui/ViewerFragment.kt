package com.example.rtmp.sdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rtmp.sdk.adapters.ChatAdapter
import com.example.rtmp.sdk.databinding.FragmentViewerBinding
import com.example.rtmp.sdk.di.DependencyContainer
import com.example.rtmp.sdk.presentation.state.*
import com.example.rtmp.sdk.presentation.viewmodel.*
import com.example.rtmp.sdk.utils.PlayerManager

@UnstableApi
class ViewerFragment : Fragment() {

    private var _binding: FragmentViewerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ViewerViewModel
    private var playerManager: PlayerManager? = null
    private lateinit var chatAdapter: ChatAdapter

    private val streamId get() = arguments?.getString(ARG_STREAM_ID)
    private val streamUrl get() = arguments?.getString(ARG_STREAM_URL)
    private val streamKey get() = arguments?.getString(ARG_STREAM_KEY) ?: "test"
    private val streamTitle get() = arguments?.getString(ARG_STREAM_TITLE)
    private val streamUser get() = arguments?.getString(ARG_STREAM_USER)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, ViewModelFactory())[ViewerViewModel::class.java]

        binding.tvStreamTitle.text = streamTitle ?: "Canlı Yayın"
        binding.tvStreamerName.text = streamUser ?: "Bilinmeyen Yayıncı"

        setupChat()
        setupListeners()
        observeViewModel()

        streamId?.let { viewModel.joinStream(it) }
    }

    private fun setupChat() {
        val currentUserId = DependencyContainer.authRepository.getCurrentUserId() ?: ""
        chatAdapter = ChatAdapter(currentUserId)
        binding.rvChatMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).also { it.stackFromEnd = true }
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSendChat.setOnClickListener { sendChatMessage() }

        binding.etChatMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendChatMessage()
                true
            } else false
        }
    }

    private fun sendChatMessage() {
        val text = binding.etChatMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            viewModel.sendMessage(text)
            binding.etChatMessage.setText("")
        }
    }

    private fun observeViewModel() {
        viewModel.viewerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ViewerState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
                is ViewerState.Joining -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.errorCard.visibility = View.GONE
                }
                is ViewerState.Buffering -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.errorCard.visibility = View.GONE
                    if (playerManager == null) {
                        initializePlayer()
                    }
                }
                is ViewerState.Playing -> {
                    binding.progressBar.visibility = View.GONE
                    binding.errorCard.visibility = View.GONE
                }
                is ViewerState.Error -> {
                    showError(state.message)
                }
                is ViewerState.Ended -> {
                    Toast.makeText(requireContext(), "Yayın sona erdi", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }

        viewModel.viewerCount.observe(viewLifecycleOwner) { count ->
            binding.tvViewerCount.text = "$count izleyici"
        }

        viewModel.chatMessages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.rvChatMessages.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun initializePlayer() {
        streamUrl?.let { url ->
            if (url.contains("youtube.com") || url.contains("rtmp://a.rtmp.youtube.com")) {
                showYouTubeInfo(url)
                return
            }
            if (url.contains("facebook.com") || url.contains("rtmps://live-api")) {
                showFacebookInfo(url)
                return
            }

            try {
                playerManager = PlayerManager(requireContext())
                playerManager?.initializePlayer(url, streamKey, binding.playerView) { playbackState ->
                    when (playbackState) {
                        Player.STATE_BUFFERING -> viewModel.onBuffering()
                        Player.STATE_READY -> {
                            viewModel.onPlaying()
                            Toast.makeText(requireContext(), "Yayın başlatıldı", Toast.LENGTH_SHORT).show()
                        }
                        Player.STATE_ENDED -> viewModel.onEnded()
                        Player.STATE_IDLE -> {}
                    }
                }
            } catch (e: Exception) {
                viewModel.onError("Player başlatılamadı")
            }
        } ?: run {
            viewModel.onError("Yayın URL'si bulunamadı")
        }
    }

    private fun releasePlayer() {
        playerManager?.releasePlayer()
        playerManager = null
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.errorCard.visibility = View.VISIBLE
        binding.tvError.text = message
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showYouTubeInfo(@Suppress("UNUSED_PARAMETER") url: String) {
        binding.progressBar.visibility = View.GONE
        binding.errorCard.visibility = View.VISIBLE
        binding.tvError.text = "YouTube Live Yayını\n\nBu yayın YouTube'a gönderiliyor.\nYouTube uygulamasından izleyebilirsiniz."
        Toast.makeText(requireContext(), "YouTube yayınları için YouTube uygulamasını kullanın", Toast.LENGTH_SHORT).show()
    }

    private fun showFacebookInfo(@Suppress("UNUSED_PARAMETER") url: String) {
        binding.progressBar.visibility = View.GONE
        binding.errorCard.visibility = View.VISIBLE
        binding.tvError.text = "Facebook Live Yayını\n\nBu yayın Facebook'a gönderiliyor.\nFacebook uygulamasından izleyebilirsiniz."
        Toast.makeText(requireContext(), "Facebook yayınları için Facebook uygulamasını kullanın", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        viewModel.leaveStream()
        _binding = null
    }

    companion object {
        private const val ARG_STREAM_ID = "STREAM_ID"
        private const val ARG_STREAM_URL = "STREAM_URL"
        private const val ARG_STREAM_KEY = "STREAM_KEY"
        private const val ARG_STREAM_TITLE = "STREAM_TITLE"
        private const val ARG_STREAM_USER = "STREAM_USER"

        fun newInstance(
            streamId: String,
            streamUrl: String,
            streamKey: String?,
            streamTitle: String?,
            streamUser: String?
        ): ViewerFragment = ViewerFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_STREAM_ID, streamId)
                putString(ARG_STREAM_URL, streamUrl)
                putString(ARG_STREAM_KEY, streamKey)
                putString(ARG_STREAM_TITLE, streamTitle)
                putString(ARG_STREAM_USER, streamUser)
            }
        }
    }
}
