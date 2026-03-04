package com.example.rtmp.sdk.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rtmp.sdk.databinding.ItemLiveStreamBinding
import com.example.rtmp.sdk.models.LiveStream
import com.example.rtmp.sdk.utils.FirebaseManager
import java.text.SimpleDateFormat
import java.util.*

class LiveStreamAdapter(
    private val onStreamClick: (LiveStream) -> Unit,
    private val onDeleteClick: (LiveStream) -> Unit
) : ListAdapter<LiveStream, LiveStreamAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLiveStreamBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onStreamClick, onDeleteClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemLiveStreamBinding,
        private val onStreamClick: (LiveStream) -> Unit,
        private val onDeleteClick: (LiveStream) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(stream: LiveStream) {
            binding.apply {
                tvTitle.text = stream.title
                tvUserName.text = "\uD83D\uDC64 ${stream.userName}"
                tvViewerCount.text = "\uD83D\uDC41\uFE0F ${stream.viewerCount} izleyici"
                tvStartTime.text = getTimeAgo(stream.startedAt)
                
                // Canlı/Geçmiş durumuna göre badge ve buton metni
                if (stream.isLive) {
                    // CANLI - Yeşil badge
                    liveBadge.setBackgroundColor(Color.parseColor("#4CAF50"))
                    tvLiveLabel.text = "\uD83D\uDD34 CANLI"
                    tvLiveLabel.setTextColor(Color.parseColor("#4CAF50"))
                    btnJoin.text = "\uD83C\uDFA5 Katıl ve İzle"
                } else {
                    // GEÇMİŞ - Kırmızı badge
                    liveBadge.setBackgroundColor(Color.parseColor("#F44336"))
                    tvLiveLabel.text = "\u23f8\uFE0F GEÇMİŞ"
                    tvLiveLabel.setTextColor(Color.parseColor("#F44336"))
                    btnJoin.text = "\uD83D\uDCF9 Kaydı İzle"
                }
                
                // Hepsi aynı yeşil tema
                cardView.setCardBackgroundColor(Color.parseColor("#F1F8F4"))
                btnJoin.isEnabled = true
                btnJoin.setBackgroundColor(Color.parseColor("#4CAF50"))
                
                // Sil butonu - sadece kendi yayınları için göster
                val currentUserId = FirebaseManager.getCurrentUserId()
                if (stream.userId == currentUserId) {
                    btnDelete.visibility = View.VISIBLE
                    btnDelete.setOnClickListener {
                        onDeleteClick(stream)
                    }
                } else {
                    btnDelete.visibility = View.GONE
                }
                
                // Kartın kendisine tıklama
                root.setOnClickListener {
                    handleStreamClick(stream)
                }
                
                // Katıl butonuna tıklama
                btnJoin.setOnClickListener {
                    handleStreamClick(stream)
                }
            }
        }
        
        private fun handleStreamClick(stream: LiveStream) {
            if (stream.isLive) {
                onStreamClick(stream)
            } else {
                android.app.AlertDialog.Builder(binding.root.context)
                    .setTitle("📹 Yayın Sona Erdi")
                    .setMessage("'${stream.title}' yayını ${getTimeAgo(stream.startedAt)} yapılmıştı.\n\nYayın süresi dolduğu için artık izleyemezsiniz.")
                    .setPositiveButton("Tamam", null)
                    .show()
            }
        }
        
        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60000 -> "Az önce"
                diff < 3600000 -> "${diff / 60000} dakika önce"
                diff < 86400000 -> "${diff / 3600000} saat önce"
                else -> SimpleDateFormat("dd MMM", Locale("tr")).format(Date(timestamp))
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<LiveStream>() {
        override fun areItemsTheSame(oldItem: LiveStream, newItem: LiveStream): Boolean {
            return oldItem.streamId == newItem.streamId
        }
        
        override fun areContentsTheSame(oldItem: LiveStream, newItem: LiveStream): Boolean {
            return oldItem == newItem
        }
    }
}
