package com.example.rtmp.sdk.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.rtmp.sdk.databinding.ActivityViewerBinding
import com.example.rtmp.sdk.utils.FirebaseManager
import kotlinx.coroutines.launch

@UnstableApi
class ViewerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityViewerBinding
    private var player: ExoPlayer? = null
    private var streamId: String? = null
    private var streamUrl: String? = null
    private var streamKey: String? = null
    private var streamTitle: String? = null
    private var streamUser: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityViewerBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Layout hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        try {
            streamId = intent.getStringExtra("STREAM_ID")
            streamUrl = intent.getStringExtra("STREAM_URL")
            streamKey = intent.getStringExtra("STREAM_KEY") ?: "test"
            streamTitle = intent.getStringExtra("STREAM_TITLE")
            streamUser = intent.getStringExtra("STREAM_USER")

            binding.tvStreamTitle.text = streamTitle ?: "Canlı Yayın"
            binding.tvStreamerName.text = streamUser ?: "Bilinmeyen Yayıncı"
            
            streamId?.let { id ->
                FirebaseManager.observeViewerCount(id) { count ->
                    binding.tvViewerCount.text = "$count izleyici"
                }
            }
            
            setupListeners()
            joinStream()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            finish()
        }
    }
    
    private fun joinStream() {
        streamId?.let { id ->
            lifecycleScope.launch {
                val result = FirebaseManager.joinStream(id)
                
                if (result.isSuccess) {
                    initializePlayer()
                } else {
                    showError("Yayına katılınamadı")
                }
            }
        } ?: run {
            showError("Yayın bilgileri bulunamadı")
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
            
            binding.progressBar.visibility = View.VISIBLE
            
            try {
                player = ExoPlayer.Builder(this).build()
                binding.playerView.player = player
            } catch (e: Exception) {
                showError("Player başlatılamadı")
                return
            }
            
            val playbackUrl = if (url.startsWith("rtmp://")) {
                convertRtmpToHls(url, streamKey ?: "test")
            } else {
                url
            }
            
            val mediaItem = MediaItem.fromUri(playbackUrl)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
            
            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.errorCard.visibility = View.GONE
                        }
                        Player.STATE_READY -> {
                            binding.progressBar.visibility = View.GONE
                            binding.errorCard.visibility = View.GONE
                            Toast.makeText(this@ViewerActivity, "Yayın başlatıldı", Toast.LENGTH_SHORT).show()
                        }
                        Player.STATE_ENDED -> {
                            Toast.makeText(this@ViewerActivity, "Yayın sona erdi", Toast.LENGTH_SHORT).show()
                        }
                        Player.STATE_IDLE -> {
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
                
                override fun onPlayerError(error: PlaybackException) {
                    val errorMsg = """
                        HLS Stream Yükleme Hatası
                        
                        Olası sebepler:
                        1. nginx-rtmp HLS hazır değil (5-10 saniye bekleyin)
                        2. nginx-rtmp yapılandırması yok
                        3. Yayıncı henüz yayını başlatmamış
                    """.trimIndent()
                    showError(errorMsg)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {}
            })
            
        } ?: run {
            showError("Yayın URL'si bulunamadı")
        }
    }

    private fun convertRtmpToHls(rtmpUrl: String, streamKey: String): String {
        val parts = rtmpUrl.replace("rtmp://", "").split(":")
        if (parts.size < 2) {
            val ip = parts[0]
            return "http://$ip:8080/hls/$streamKey.m3u8"
        }
        
        val ip = parts[0]
        return "http://$ip:8080/hls/$streamKey.m3u8"
    }
    
    private fun showYouTubeInfo(url: String) {
        binding.progressBar.visibility = View.GONE
        binding.errorCard.visibility = View.VISIBLE
        binding.tvError.text = """
            YouTube Live Yayını
            
            Bu yayın YouTube'a gönderiliyor.
            YouTube uygulamasından izleyebilirsiniz.
        """.trimIndent()
        
        Toast.makeText(this, "YouTube yayınları için YouTube uygulamasını kullanın", Toast.LENGTH_SHORT).show()
    }
    
    private fun showFacebookInfo(url: String) {
        binding.progressBar.visibility = View.GONE
        binding.errorCard.visibility = View.VISIBLE
        binding.tvError.text = """
            Facebook Live Yayını
            
            Bu yayın Facebook'a gönderiliyor.
            Facebook uygulamasından izleyebilirsiniz.
        """.trimIndent()
        
        Toast.makeText(this, "Facebook yayınları için Facebook uygulamasını kullanın", Toast.LENGTH_SHORT).show()
    }
    
    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.errorCard.visibility = View.VISIBLE
        binding.tvError.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun releasePlayer() {
        player?.release()
        player = null
    }
    
    override fun onStart() {
        super.onStart()
        try {
            if (player == null && streamUrl != null) {
                initializePlayer()
            }
        } catch (e: Exception) {}
    }
    
    override fun onStop() {
        super.onStop()
        try {
            releasePlayer()
        } catch (e: Exception) {}
    }
    
    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        
        streamId?.let { id ->
            lifecycleScope.launch {
                FirebaseManager.leaveStream(id)
            }
        }
    }
}
