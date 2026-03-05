package com.example.rtmp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.rtmp.databinding.FragmentMainBinding
import com.example.rtmp.sdk.models.LiveStream
import com.example.rtmp.sdk.presentation.state.StreamListState
import com.example.rtmp.sdk.presentation.viewmodel.MainViewModel
import com.example.rtmp.sdk.presentation.viewmodel.ViewModelFactory
import com.example.rtmp.sdk.ui.BroadcastFragment
import com.example.rtmp.sdk.ui.ViewerFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    lateinit var viewModel: MainViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) startBroadcast()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this, ViewModelFactory())[MainViewModel::class.java]

        setupViewPager()
        setupListeners()
        observeViewModel()
        setHasOptionsMenu(true)

        viewModel.observeStreams()
    }

    override fun onResume() {
        super.onResume()
        viewModel.observeStreams()
    }

    private fun setupViewPager() {
        val pagerAdapter = StreamsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 2

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pagerAdapter.tabTitles[position]
        }.attach()
    }

    private fun setupListeners() {
        binding.fabStartBroadcast.setOnClickListener {
            if (checkPermissions()) {
                startBroadcast()
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
            }
        }
    }

    private fun observeViewModel() {
        viewModel.streamListState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is StreamListState.Idle -> binding.progressBar.visibility = View.GONE
                is StreamListState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                }
                is StreamListState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE
                    binding.viewPager.visibility = View.VISIBLE
                }
                is StreamListState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.viewPager.visibility = View.VISIBLE
                }
                is StreamListState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                Toast.makeText(requireContext(), "Yayın silindi", Toast.LENGTH_SHORT).show()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Silme hatası"
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.logoutTriggered.observe(viewLifecycleOwner) { triggered ->
            if (triggered) {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, LoginFragment.newInstance())
                    .commit()
            }
        }
    }


    private fun checkPermissions(): Boolean {
        return listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startBroadcast() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, BroadcastFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    private fun openStreamViewer(stream: LiveStream) {
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
            .setMessage("'${stream.title}' yayınını silmek istediğinizden emin misiniz?\n\nBu işlem geri alınamaz.")
            .setPositiveButton("Sil") { _, _ -> viewModel.deleteStream(stream) }
            .setNegativeButton("İptal", null)
            .show()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                viewModel.logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopObserving()
        _binding = null
    }

    companion object {
        fun newInstance(): MainFragment = MainFragment()
    }
}
