package com.example.rtmp.sdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rtmp.sdk.adapters.ChatAdapter
import com.example.rtmp.sdk.databinding.FragmentBroadcastBinding
import com.example.rtmp.sdk.di.DependencyContainer
import com.example.rtmp.sdk.presentation.state.*
import com.example.rtmp.sdk.presentation.viewmodel.*
import com.example.rtmp.sdk.utils.CameraManager
import com.pedro.common.ConnectChecker

class BroadcastFragment : Fragment(), ConnectChecker, SurfaceHolder.Callback {

    private var _binding: FragmentBroadcastBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BroadcastViewModel
    private lateinit var chatAdapter: ChatAdapter
    private var cameraManager: CameraManager? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBroadcastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, ViewModelFactory())[BroadcastViewModel::class.java]

        // SurfaceView oluştur
        val surfaceView = SurfaceView(requireContext())
        binding.cameraPreview.addView(surfaceView)
        surfaceView.holder.addCallback(this)

        setupListeners()
        observeViewModel()
        setupIncomingChat()
        handleBackPress()
    }

    private fun setupIncomingChat() {
        val currentUserId = DependencyContainer.authRepository.getCurrentUserId() ?: ""
        chatAdapter = ChatAdapter(currentUserId)
        binding.rvIncomingChat.apply {
            layoutManager = LinearLayoutManager(requireContext()).also { it.stackFromEnd = true }
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.btnStartStop.setOnClickListener {
            val state = viewModel.broadcastState.value
            if (state is BroadcastState.Streaming) {
                stopStreaming()
            } else {
                startStreaming()
            }
        }

        binding.btnSwitchCamera.setOnClickListener {
            cameraManager?.switchCamera()
        }

        binding.btnEndStream.setOnClickListener {
            showExitConfirmationDialog()
        }

        binding.btnClose.setOnClickListener {
            val state = viewModel.broadcastState.value
            if (state is BroadcastState.Streaming) {
                showExitConfirmationDialog()
            } else {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.broadcastState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BroadcastState.Idle -> updateStreamingUI(false)
                is BroadcastState.Preparing -> {
                    Toast.makeText(requireContext(), "Yayın hazırlanıyor...", Toast.LENGTH_SHORT).show()
                }
                is BroadcastState.Connecting -> {
                    // UI already updated by ConnectChecker callbacks
                }
                is BroadcastState.Streaming -> {
                    updateStreamingUI(true)
                }
                is BroadcastState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    updateStreamingUI(false)
                }
                is BroadcastState.Stopped -> {
                    updateStreamingUI(false)
                }
            }
        }

        viewModel.validationState.observe(viewLifecycleOwner) { validation ->
            binding.tilStreamTitle.error = validation.titleError
            binding.tilRtmpUrl.error = validation.rtmpUrlError
            binding.tilStreamKey.error = validation.streamKeyError

            if (validation.rtmpUrlError != null && validation.rtmpUrlError.contains("rtmp://")) {
                Toast.makeText(
                    requireContext(),
                    "⚠️ Geçersiz RTMP URL!\n\nDoğru format:\nrtmp://IP_ADRESI:1935/live\n\nÖrnek:\nrtmp://192.168.1.100:1935/live",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        viewModel.viewerCount.observe(viewLifecycleOwner) { count ->
            binding.tvViewerCount.text = "👁️ İzleyici: $count"
        }

        viewModel.viewers.observe(viewLifecycleOwner) { viewers ->
            if (viewers.isEmpty()) {
                binding.tvViewerList.text = "Henüz kimse katılmadı"
            } else {
                val viewerNames = viewers.joinToString("\n") { "• ${it.userName}" }
                binding.tvViewerList.text = viewerNames
            }
        }

        viewModel.chatMessages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.rvIncomingChat.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val state = viewModel.broadcastState.value
                    if (state is BroadcastState.Streaming) {
                        showExitConfirmationDialog()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun startCameraPreview() {
        cameraManager?.startPreview()
            ?: Toast.makeText(requireContext(), "Kamera başlatılamadı", Toast.LENGTH_SHORT).show()
    }

    private fun startStreaming() {
        val title = binding.etStreamTitle.text.toString().trim()
        val rtmpUrl = binding.etRtmpUrl.text.toString().trim()
        val streamKey = binding.etStreamKey.text.toString().trim()

        if (!viewModel.validateBroadcastForm(title, rtmpUrl, streamKey)) return

        val fullUrl = adjustRtmpUrlForEmulator("$rtmpUrl/$streamKey")
        viewModel.createStream(title, rtmpUrl, streamKey)

        cameraManager?.startStream(fullUrl) { success, error ->
            if (!success) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopStreaming() {
        cameraManager?.stopStream()
        viewModel.endStream()
    }

    private fun updateStreamingUI(streaming: Boolean) {
        activity?.runOnUiThread {
            if (streaming) {
                binding.liveStatusCard.visibility = View.VISIBLE
                binding.viewerCountCard.visibility = View.VISIBLE
                binding.viewerListCard.visibility = View.VISIBLE
                binding.rvIncomingChat.visibility = View.VISIBLE
                binding.btnEndStream.visibility = View.VISIBLE
                binding.controlsContainer.visibility = View.GONE
            } else {
                binding.liveStatusCard.visibility = View.GONE
                binding.viewerCountCard.visibility = View.GONE
                binding.viewerListCard.visibility = View.GONE
                binding.rvIncomingChat.visibility = View.GONE
                binding.btnEndStream.visibility = View.GONE
                binding.controlsContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Yayını Sonlandır")
            .setMessage("Yayını sonlandırmak istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                stopStreaming()
                parentFragmentManager.popBackStack()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    // SurfaceHolder.Callback
    override fun surfaceCreated(holder: SurfaceHolder) {
        val surfaceView = binding.cameraPreview.getChildAt(0) as? SurfaceView ?: return
        cameraManager = CameraManager(requireActivity(), surfaceView, this)
        startCameraPreview()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        cameraManager?.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        cameraManager?.release()
        cameraManager = null
    }

    // ConnectChecker
    override fun onConnectionStarted(url: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), "RTMP sunucusuna bağlanıyor...", Toast.LENGTH_SHORT).show()
            viewModel.onConnectionStarted()
        }
    }

    override fun onConnectionSuccess() {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), "Yayın başlatıldı", Toast.LENGTH_SHORT).show()
            viewModel.onConnectionSuccess()
        }
    }

    override fun onConnectionFailed(reason: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), "Bağlantı hatası: $reason", Toast.LENGTH_SHORT).show()
            viewModel.onConnectionFailed(reason)
        }
    }

    override fun onNewBitrate(bitrate: Long) {}

    override fun onDisconnect() {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), "Bağlantı kesildi", Toast.LENGTH_SHORT).show()
            viewModel.endStream()
        }
    }

    override fun onAuthError() {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), "Kimlik doğrulama hatası", Toast.LENGTH_SHORT).show()
            viewModel.onConnectionFailed("Auth error")
        }
    }

    override fun onAuthSuccess() {}

    override fun onDestroyView() {
        super.onDestroyView()
        cameraManager?.release()
        cameraManager = null
        _binding = null
    }

    private fun adjustRtmpUrlForEmulator(rtmpUrl: String): String {
        if (!isRunningOnEmulator()) return rtmpUrl

        val regex = """rtmp[s]?://([^:]+)(:.*)""".toRegex()
        val match = regex.find(rtmpUrl)

        return if (match != null) {
            val host = match.groupValues[1]
            val rest = match.groupValues[2]
            if (host.startsWith("192.168.") || host.startsWith("10.0.") ||
                host == "localhost" || host == "127.0.0.1"
            ) "rtmp://10.0.2.2$rest"
            else rtmpUrl
        } else rtmpUrl
    }

    private fun isRunningOnEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == android.os.Build.PRODUCT)
    }

    companion object {
        fun newInstance(): BroadcastFragment = BroadcastFragment()
    }
}
