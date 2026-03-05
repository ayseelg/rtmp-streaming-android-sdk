package com.example.rtmp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rtmp.databinding.FragmentStreamsPageBinding
import com.example.rtmp.sdk.adapters.LiveStreamAdapter
import com.example.rtmp.sdk.adapters.PastStreamAdapter
import com.example.rtmp.sdk.di.DependencyContainer
import com.example.rtmp.sdk.models.LiveStream
import com.example.rtmp.sdk.presentation.state.StreamListState
import com.example.rtmp.sdk.presentation.viewmodel.MainViewModel
import com.example.rtmp.sdk.presentation.viewmodel.ViewModelFactory
import com.example.rtmp.sdk.ui.ViewerFragment

class StreamsPageFragment : Fragment() {

    enum class FilterType(val arg: String) {
        LIVE("LIVE"),
        MY_PAST("MY_PAST"),
        OTHER_PAST("OTHER_PAST")
    }

    private var _binding: FragmentStreamsPageBinding? = null
    private val binding get() = _binding!!

    private val filterType: FilterType by lazy {
        FilterType.valueOf(arguments?.getString(ARG_FILTER) ?: FilterType.LIVE.arg)
    }

    // Share the MainViewModel from the parent MainFragment
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(requireParentFragment(), ViewModelFactory())[MainViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStreamsPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        val currentUserId = DependencyContainer.authRepository.getCurrentUserId()

        when (filterType) {
            FilterType.LIVE -> {
                val adapter = LiveStreamAdapter(
                    onStreamClick = { stream -> openViewer(stream) },
                    onDeleteClick = { stream -> deleteStream(stream) }
                )
                binding.rvStreams.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    this.adapter = adapter
                }
                viewModel.streamListState.observe(viewLifecycleOwner) { state ->
                    when (state) {
                        is StreamListState.Loading -> binding.pageProgressBar.visibility = View.VISIBLE
                        is StreamListState.Success -> {
                            binding.pageProgressBar.visibility = View.GONE
                            val list = state.streams.filter { it.endedAt == 0L }
                            adapter.submitList(list)
                            showEmpty(list.isEmpty(), "📡", "Şu an canlı yayın yok\nİlk yayını sen başlat!")
                        }
                        is StreamListState.Empty -> {
                            binding.pageProgressBar.visibility = View.GONE
                            adapter.submitList(emptyList())
                            showEmpty(true, "📡", "Şu an canlı yayın yok\nİlk yayını sen başlat!")
                        }
                        else -> binding.pageProgressBar.visibility = View.GONE
                    }
                }
            }

            FilterType.MY_PAST -> {
                val adapter = PastStreamAdapter(
                    onDeleteClick = { stream -> deleteStream(stream) }
                )
                binding.rvStreams.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    this.adapter = adapter
                }
                viewModel.streamListState.observe(viewLifecycleOwner) { state ->
                    when (state) {
                        is StreamListState.Loading -> binding.pageProgressBar.visibility = View.VISIBLE
                        is StreamListState.Success -> {
                            binding.pageProgressBar.visibility = View.GONE
                            val list = state.streams.filter {
                                it.endedAt > 0L && it.userId == currentUserId
                            }
                            adapter.submitList(list)
                            showEmpty(list.isEmpty(), "📼", "Henüz geçmiş yayınınız yok")
                        }
                        is StreamListState.Empty -> {
                            binding.pageProgressBar.visibility = View.GONE
                            adapter.submitList(emptyList())
                            showEmpty(true, "📼", "Henüz geçmiş yayınınız yok")
                        }
                        else -> binding.pageProgressBar.visibility = View.GONE
                    }
                }
            }

            FilterType.OTHER_PAST -> {
                val adapter = PastStreamAdapter(
                    onDeleteClick = { stream -> deleteStream(stream) }
                )
                binding.rvStreams.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    this.adapter = adapter
                }
                viewModel.streamListState.observe(viewLifecycleOwner) { state ->
                    when (state) {
                        is StreamListState.Loading -> binding.pageProgressBar.visibility = View.VISIBLE
                        is StreamListState.Success -> {
                            binding.pageProgressBar.visibility = View.GONE
                            val list = state.streams.filter {
                                it.endedAt > 0L && it.userId != currentUserId
                            }
                            adapter.submitList(list)
                            showEmpty(list.isEmpty(), "📺", "Diğer kullanıcıların\ngeçmiş yayını yok")
                        }
                        is StreamListState.Empty -> {
                            binding.pageProgressBar.visibility = View.GONE
                            adapter.submitList(emptyList())
                            showEmpty(true, "📺", "Diğer kullanıcıların\ngeçmiş yayını yok")
                        }
                        else -> binding.pageProgressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        // Delete result toast is handled in MainFragment
    }

    private fun showEmpty(show: Boolean, icon: String, text: String) {
        binding.layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.tvEmptyIcon.text = icon
            binding.tvEmptyText.text = text
        }
    }

    private fun openViewer(stream: LiveStream) {
        val fragment = ViewerFragment.newInstance(
            streamId = stream.streamId,
            streamUrl = stream.rtmpUrl,
            streamKey = stream.streamKey,
            streamTitle = stream.title,
            streamUser = stream.userName
        )
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun deleteStream(stream: LiveStream) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Yayını Sil")
            .setMessage("'${stream.title}' yayınını silmek istiyor musunuz?")
            .setPositiveButton("Sil") { _, _ -> viewModel.deleteStream(stream) }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_FILTER = "filter_type"

        fun newInstance(type: FilterType) = StreamsPageFragment().apply {
            arguments = Bundle().apply { putString(ARG_FILTER, type.arg) }
        }
    }
}
