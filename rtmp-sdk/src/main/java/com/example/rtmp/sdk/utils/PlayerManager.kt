package com.example.rtmp.sdk.utils

import android.content.Context
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.PlaybackException
import androidx.media3.ui.PlayerView

class PlayerManager(private val context: Context) {

    private var player: ExoPlayer? = null
    private var attachedView: PlayerView? = null

    fun initializePlayer(url: String, streamKey: String, playerView: PlayerView, onStateChanged: (Int) -> Unit) {
        val playbackUrl = if (url.startsWith("rtmp://")) {
            convertRtmpToHls(url, streamKey)
        } else {
            url
        }

        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(playbackUrl))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    onStateChanged(playbackState)
                }

                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(context, "Hata: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
        // Video yüzeyine bağla
        attachedView = playerView
        playerView.player = player
        playerView.useController = false
    }

    fun releasePlayer() {
        attachedView?.player = null
        attachedView = null
        player?.release()
        player = null
    }

    private fun convertRtmpToHls(rtmpUrl: String, streamKey: String): String {
        val parts = rtmpUrl.replace("rtmp://", "").split(":")
        val ip = parts[0]
        return "http://$ip:8080/hls/$streamKey.m3u8"
    }
}