package com.example.rtmp.sdk.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rtmp.sdk.databinding.ItemChatMessageBinding
import com.example.rtmp.sdk.models.ChatMessage

class ChatAdapter(
    private val currentUserId: String
) : ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val isOwn = message.userId == currentUserId

            if (isOwn) {
                // Own message: right-aligned, primary color
                (binding.chatBubble.layoutParams as FrameLayout.LayoutParams).apply {
                    gravity = Gravity.END
                }
                binding.chatBubble.setCardBackgroundColor(Color.parseColor("#6366F1"))
                binding.tvSenderName.visibility = android.view.View.GONE
            } else {
                // Other's message: left-aligned, dark overlay
                (binding.chatBubble.layoutParams as FrameLayout.LayoutParams).apply {
                    gravity = Gravity.START
                }
                binding.chatBubble.setCardBackgroundColor(Color.parseColor("#2D3748"))
                binding.tvSenderName.visibility = android.view.View.VISIBLE
                binding.tvSenderName.text = message.userName
            }

            binding.tvMessageText.text = message.text
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
            oldItem.messageId == newItem.messageId

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
            oldItem == newItem
    }
}
