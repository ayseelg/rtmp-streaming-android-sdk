package com.example.rtmp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rtmp.databinding.ActivityMainBinding
import com.example.rtmp.sdk.adapters.LiveStreamAdapter
import com.example.rtmp.sdk.models.LiveStream
import com.example.rtmp.sdk.ui.BroadcastActivity
import com.example.rtmp.sdk.ui.ViewerActivity
import com.example.rtmp.sdk.utils.FirebaseManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: LiveStreamAdapter
    
    private val PERMISSION_REQUEST_CODE = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        
        setupRecyclerView()
        setupListeners()
        observeLiveStreams()
    }
    
    private fun setupRecyclerView() {
        adapter = LiveStreamAdapter(
            onStreamClick = { stream ->
                openStreamViewer(stream)
            },
            onDeleteClick = { stream ->
                deleteStream(stream)
            }
        )
        
        binding.rvStreams.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }
    
    private fun setupListeners() {
        binding.fabStartBroadcast.setOnClickListener {
            if (checkPermissions()) {
                startBroadcast()
            } else {
                requestPermissions()
            }
        }
    }
    
    private fun observeLiveStreams() {
        binding.progressBar.visibility = View.VISIBLE
        
        android.util.Log.d("MainActivity", "═══════════════════════════════════════")
        android.util.Log.d("MainActivity", "🔍 Tüm yayınlar dinleniyor (canlı + geçmiş)...")
        android.util.Log.d("MainActivity", "═══════════════════════════════════════")
        
        FirebaseManager.observeAllStreams { streams ->
            binding.progressBar.visibility = View.GONE
            
            val liveCount = streams.count { it.isLive }
            val pastCount = streams.size - liveCount
            
            android.util.Log.d("MainActivity", "─────────────────────────────────────")
            android.util.Log.d("MainActivity", "📊 Yayın listesi güncellendi")
            android.util.Log.d("MainActivity", "Canlı: $liveCount, Geçmiş: $pastCount, Toplam: ${streams.size}")
            
            streams.forEachIndexed { index, stream ->
                val status = if (stream.isLive) "🔴 CANLI" else "⏸️ GEÇMİŞ"
                android.util.Log.d("MainActivity", "  ${index + 1}. $status ${stream.title} - ${stream.userName} (${stream.viewerCount} izleyici)")
            }
            android.util.Log.d("MainActivity", "─────────────────────────────────────")
            
            if (streams.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvStreams.visibility = View.GONE
                android.util.Log.i("MainActivity", "ℹ️ Henüz yayın yok")
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvStreams.visibility = View.VISIBLE
                adapter.submitList(streams)
                android.util.Log.i("MainActivity", "✅ ${streams.size} yayın listede gösteriliyor")
            }
        }
    }
    
    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startBroadcast()
            }
        }
    }
    
    private fun startBroadcast() {
        startActivity(Intent(this, BroadcastActivity::class.java))
    }
    
    private fun openStreamViewer(stream: LiveStream) {
        val intent = Intent(this, ViewerActivity::class.java)
        intent.putExtra("STREAM_ID", stream.streamId)
        intent.putExtra("STREAM_URL", stream.rtmpUrl)
        intent.putExtra("STREAM_KEY", stream.streamKey)
        intent.putExtra("STREAM_TITLE", stream.title)
        intent.putExtra("STREAM_USER", stream.userName)
        startActivity(intent)
    }
    
    private fun deleteStream(stream: LiveStream) {
        if (stream.userId != FirebaseManager.getCurrentUserId()) {
            android.widget.Toast.makeText(this, "Sadece kendi yayınlarınızı silebilirsiniz", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Yayını Sil")
            .setMessage("'${stream.title}' yayınını silmek istediğinizden emin misiniz?\n\nBu işlem geri alınamaz.")
            .setPositiveButton("Sil") { _, _ ->
                lifecycleScope.launch {
                    val result = FirebaseManager.deleteStream(stream.streamId)
                    if (result.isSuccess) {
                        android.widget.Toast.makeText(this@MainActivity, "Yayın silindi", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(this@MainActivity, "Silme hatası", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                FirebaseManager.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
