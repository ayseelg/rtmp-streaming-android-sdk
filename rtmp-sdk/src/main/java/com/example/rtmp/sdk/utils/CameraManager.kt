package com.example.rtmp.sdk.utils

import android.content.Context
import android.view.SurfaceView
import android.widget.Toast
import com.pedro.common.ConnectChecker
import com.pedro.library.rtmp.RtmpCamera1

class CameraManager(
    private val context: Context,
    private val surfaceView: SurfaceView,
    private val connectChecker: ConnectChecker
) {

    private var rtmpCamera: RtmpCamera1? = null

    init {
        rtmpCamera = RtmpCamera1(surfaceView, connectChecker)
    }

    fun startStream(url: String, callback: (Boolean, String?) -> Unit) {
        try {
            if (rtmpCamera?.isStreaming == true) {
                rtmpCamera?.stopStream()
                Thread.sleep(500)
            }

            val videoReady = rtmpCamera?.prepareVideo(640, 480, 30, 1200 * 1024, 0) ?: false
            val audioReady = rtmpCamera?.prepareAudio() ?: false

            if (videoReady && audioReady) {
                rtmpCamera?.startStream(url)
                callback(true, null)
            } else {
                callback(false, "Kamera/Mikrofon hazırlanamadı")
            }
        } catch (e: Exception) {
            callback(false, e.message)
        }
    }

    fun stopStream() {
        rtmpCamera?.stopStream()
    }

    fun switchCamera() {
        rtmpCamera?.switchCamera()
    }

    fun startPreview() {
        try {
            if (rtmpCamera?.isOnPreview == false) {
                if (rtmpCamera?.prepareVideo(640, 480, 30, 1200 * 1024, 0) == true &&
                    rtmpCamera?.prepareAudio() == true) {
                    rtmpCamera?.startPreview()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Kamera önizleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun release() {
        rtmpCamera?.stopStream()
        rtmpCamera?.stopPreview()
    }
}