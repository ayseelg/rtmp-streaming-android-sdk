package com.example.rtmp.sdk.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rtmp.sdk.databinding.ItemPastStreamBinding
import com.example.rtmp.sdk.di.DependencyContainer
import com.example.rtmp.sdk.models.LiveStream
import java.text.SimpleDateFormat
import java.util.*

class PastStreamAdapter(
    private val onDeleteClick: (LiveStream) -> Unit
) : ListAdapter<LiveStream, PastStreamAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPastStreamBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemPastStreamBinding,
        private val onDeleteClick: (LiveStream) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stream: LiveStream) {
            binding.tvPastTitle.text = stream.title
            binding.tvPastUser.text = "👤 ${stream.userName}"
            binding.tvPastTime.text = getTimeAgo(stream.startedAt)
            binding.tvPastViewers.text = "👁 ${stream.viewerCount}"

            // Show delete button only for own streams
            val currentUserId = DependencyContainer.authRepository.getCurrentUserId()
            if (stream.userId == currentUserId) {
                binding.btnPastDelete.visibility = View.VISIBLE
                binding.btnPastDelete.setOnClickListener { onDeleteClick(stream) }
            } else {
                binding.btnPastDelete.visibility = View.GONE
            }

            // Tap card → show detail dialog
            binding.cardPast.setOnClickListener {
                showDetailDialog(stream)
            }
        }

        private fun showDetailDialog(stream: LiveStream) {
            val ctx = binding.root.context
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr"))

            val duration = if (stream.endedAt > 0) {
                val diffMs = stream.endedAt - stream.startedAt
                val mins = (diffMs / 60000).toInt()
                val hours = mins / 60
                if (hours > 0) "${hours} saat ${mins % 60} dakika"
                else "$mins dakika"
            } else {
                "—"
            }

            val details = buildString {
                appendLine("📋 Yayın Bilgileri")
                appendLine()
                appendLine("🎬 Başlık:  ${stream.title}")
                appendLine("👤 Yayıncı:  ${stream.userName}")
                appendLine("📅 Başlangıç:  ${dateFormat.format(Date(stream.startedAt))}")
                if (stream.endedAt > 0) {
                    appendLine("🏁 Bitiş:  ${dateFormat.format(Date(stream.endedAt))}")
                }
                appendLine("⏱ Süre:  $duration")
                appendLine("👁 İzleyici:  ${stream.viewerCount}")
            }

            AlertDialog.Builder(ctx)
                .setTitle("📼 Geçmiş Yayın")
                .setMessage(details.trimEnd())
                .setPositiveButton("Tamam", null)
                .show()
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < 60_000L -> "Az önce"
                diff < 3_600_000L -> "${diff / 60_000} dakika önce"
                diff < 86_400_000L -> "${diff / 3_600_000} saat önce"
                else -> SimpleDateFormat("dd MMM", Locale("tr")).format(Date(timestamp))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LiveStream>() {
        override fun areItemsTheSame(oldItem: LiveStream, newItem: LiveStream): Boolean =
            oldItem.streamId == newItem.streamId

        override fun areContentsTheSame(oldItem: LiveStream, newItem: LiveStream): Boolean =
            oldItem == newItem
    }
}
