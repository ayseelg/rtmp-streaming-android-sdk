package com.example.rtmp.sdk.ui

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rtmp.sdk.databinding.ActivityBroadcastBinding
import com.example.rtmp.sdk.utils.FirebaseManager
import com.pedro.common.ConnectChecker
import com.pedro.library.rtmp.RtmpCamera1
import kotlinx.coroutines.launch

class BroadcastActivity : AppCompatActivity(), ConnectChecker, SurfaceHolder.Callback {

    private lateinit var binding: ActivityBroadcastBinding
    private var rtmpCamera: RtmpCamera1? = null
    private var isStreaming = false
    private var currentStreamId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBroadcastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SurfaceView oluştur
        val surfaceView = SurfaceView(this)
        binding.cameraPreview.addView(surfaceView)
        surfaceView.holder.addCallback(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnStartStop.setOnClickListener {
            if (!isStreaming) {
                startStreaming()
            } else {
                stopStreaming()
            }
        }

        binding.btnSwitchCamera.setOnClickListener {
            rtmpCamera?.switchCamera()
        }

        binding.btnClose.setOnClickListener {
            if (isStreaming) {
                showExitConfirmationDialog()
            } else {
                finish()
            }
        }
    }

    private fun startCameraPreview() {
        try {
            rtmpCamera?.let { camera ->
                if (camera.prepareVideo(640, 480, 30, 1200 * 1024, 0) && camera.prepareAudio()) {
                    camera.startPreview()
                } else {
                    Toast.makeText(this, "Kamera başlatılamadı", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Kamera hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startStreaming() {
        val title = binding.etStreamTitle.text.toString().trim()
        val rtmpUrl = binding.etRtmpUrl.text.toString().trim()
        val streamKey = binding.etStreamKey.text.toString().trim()

        if (title.isEmpty()) {
            binding.tilStreamTitle.error = "Yayın başlığı gerekli"
            return
        }
        binding.tilStreamTitle.error = null

        if (rtmpUrl.isEmpty()) {
            binding.tilRtmpUrl.error = "RTMP URL gerekli"
            return
        }
        
        if (!rtmpUrl.startsWith("rtmp://") && !rtmpUrl.startsWith("rtmps://")) {
            binding.tilRtmpUrl.error = "URL 'rtmp://' ile başlamalı\nÖrnek: rtmp://192.168.1.100:1935/live"
            Toast.makeText(this, "⚠️ Geçersiz RTMP URL!\n\nDoğru format:\nrtmp://IP_ADRESI:1935/live\n\nÖrnek:\nrtmp://192.168.1.100:1935/live", Toast.LENGTH_LONG).show()
            return
        }
        binding.tilRtmpUrl.error = null

        if (streamKey.isEmpty()) {
            binding.tilStreamKey.error = "Stream key gerekli"
            return
        }
        binding.tilStreamKey.error = null

        val adjustedRtmpUrl = adjustRtmpUrlForEmulator(rtmpUrl)
        val fullUrl = "$adjustedRtmpUrl/$streamKey"
        
        Toast.makeText(this, "🔄 Yayın hazırlanıyor...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val result = FirebaseManager.createLiveStream(title, rtmpUrl, streamKey)

                if (result.isSuccess) {
                    val stream = result.getOrNull()
                    currentStreamId = stream?.streamId
                    
                    currentStreamId?.let { streamId ->
                        FirebaseManager.observeViewerCount(streamId) { count ->
                            runOnUiThread {
                                binding.tvViewerCount.text = "👁️ İzleyici: $count"
                            }
                        }
                    }
                    
                    currentStreamId?.let { streamId ->
                        FirebaseManager.observeViewers(streamId) { viewers ->
                            runOnUiThread {
                                if (viewers.isEmpty()) {
                                    binding.tvViewerList.text = "Henüz kimse katılmadı"
                                } else {
                                    val viewerNames = viewers.joinToString("\n") { "• ${it.userName}" }
                                    binding.tvViewerList.text = viewerNames
                                }
                            }
                        }
                    }

                rtmpCamera?.let { camera ->
                    try {
                        if (camera.isStreaming) {
                            camera.stopStream()
                            Thread.sleep(500)
                        }

                        val videoReady = camera.prepareVideo(640, 480, 30, 1200 * 1024, 0)
                        val audioReady = camera.prepareAudio()
                        
                        if (videoReady && audioReady) {
                            camera.startStream(fullUrl)
                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    this@BroadcastActivity,
                                    "Kamera/Mikrofon hazırlanamadı",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this@BroadcastActivity,
                                "Yayın başlatma hatası: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } ?: run {
                    runOnUiThread {
                        Toast.makeText(
                            this@BroadcastActivity,
                            "Kamera başlatılamadı",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } else {
                runOnUiThread {
                    Toast.makeText(
                        this@BroadcastActivity,
                        "Firebase hatası. Yayın listede görünmeyebilir.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                startRtmpStreamWithoutFirebase(fullUrl)
            }
            
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@BroadcastActivity,
                        "Hata: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startRtmpStreamWithoutFirebase(fullUrl: String) {
        rtmpCamera?.let { camera ->
            try {
                if (camera.prepareVideo(640, 480, 30, 1200 * 1024, 0) && camera.prepareAudio()) {
                    camera.startStream(fullUrl)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "RTMP başlatma hatası", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopStreaming() {
        rtmpCamera?.let { camera ->
            if (camera.isStreaming) {
                camera.stopStream()
            }
        }

        currentStreamId?.let { streamId ->
            lifecycleScope.launch {
                FirebaseManager.endLiveStream(streamId)
                currentStreamId = null
            }
        }
        
        // İzleyici listesini temizle
        runOnUiThread {
            binding.tvViewerList.text = ""
            binding.viewerListCard.visibility = View.GONE
        }

        updateStreamingUI(false)
    }

    private fun updateStreamingUI(streaming: Boolean) {
        isStreaming = streaming

        runOnUiThread {
            if (streaming) {
                binding.btnStartStop.text = "Yayını Durdur"
                binding.btnStartStop.backgroundTintList =
                    getColorStateList(android.R.color.holo_green_dark)
                binding.liveStatusCard.visibility = View.VISIBLE
                binding.viewerCountCard.visibility = View.VISIBLE
                binding.viewerListCard.visibility = View.VISIBLE
                binding.controlsContainer.visibility = View.GONE
            } else {
                binding.btnStartStop.text = "Yayını Başlat"
                binding.btnStartStop.backgroundTintList =
                    getColorStateList(android.R.color.holo_red_dark)
                binding.liveStatusCard.visibility = View.GONE
                binding.viewerCountCard.visibility = View.GONE
                binding.viewerListCard.visibility = View.GONE
                binding.controlsContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Yayını Sonlandır")
            .setMessage("Yayını sonlandırmak istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                stopStreaming()
                finish()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    // SurfaceHolder.Callback
    override fun surfaceCreated(holder: SurfaceHolder) {
        val surfaceView = binding.cameraPreview.getChildAt(0) as? SurfaceView
        if (surfaceView != null) {
            rtmpCamera = RtmpCamera1(surfaceView, this)
            startCameraPreview()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        rtmpCamera?.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        rtmpCamera?.let { camera ->
            if (camera.isStreaming) {
                camera.stopStream()
            }
            camera.stopPreview()
        }
        rtmpCamera = null
    }

    // ConnectChecker interface
    override fun onConnectionStarted(url: String) {
        runOnUiThread {
            Toast.makeText(this, "RTMP sunucusuna bağlanıyor...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionSuccess() {
        runOnUiThread {
            Toast.makeText(this, "Yayın başlatıldı", Toast.LENGTH_SHORT).show()
            updateStreamingUI(true)
        }
    }

    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            Toast.makeText(this, "Bağlantı hatası: $reason", Toast.LENGTH_SHORT).show()
            updateStreamingUI(false)

            currentStreamId?.let { streamId ->
                lifecycleScope.launch {
                    FirebaseManager.endLiveStream(streamId)
                    currentStreamId = null
                }
            }
        }
    }

    override fun onNewBitrate(bitrate: Long) {}


    override fun onDisconnect() {
        runOnUiThread {
            Toast.makeText(this, "Bağlantı kesildi", Toast.LENGTH_SHORT).show()
            updateStreamingUI(false)
        }
    }

    override fun onAuthError() {
        runOnUiThread {
            Toast.makeText(this, "Kimlik doğrulama hatası", Toast.LENGTH_SHORT).show()
            updateStreamingUI(false)
        }
    }

    override fun onAuthSuccess() {}


    override fun onDestroy() {
        super.onDestroy()
        rtmpCamera?.let { camera ->
            if (camera.isStreaming) {
                camera.stopStream()
            }
            camera.stopPreview()
        }
        rtmpCamera = null
    }

    override fun onBackPressed() {
        if (isStreaming) {
            showExitConfirmationDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun adjustRtmpUrlForEmulator(rtmpUrl: String): String {
        if (!isRunningOnEmulator()) {
            return rtmpUrl
        }

        val regex = """rtmp[s]?://([^:]+)(:.*)""".toRegex()
        val match = regex.find(rtmpUrl)
        
        return if (match != null) {
            val host = match.groupValues[1]
            val rest = match.groupValues[2]
            
            if (host.startsWith("192.168.") || host.startsWith("10.0.") || 
                host == "localhost" || host == "127.0.0.1") {
                "rtmp://10.0.2.2$rest"
            } else {
                rtmpUrl
            }
        } else {
            rtmpUrl
        }
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
}
