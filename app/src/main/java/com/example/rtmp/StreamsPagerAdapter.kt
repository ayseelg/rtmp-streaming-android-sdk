package com.example.rtmp

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class StreamsPagerAdapter(host: Fragment) : FragmentStateAdapter(host) {

    private val pages = listOf(
        StreamsPageFragment.FilterType.LIVE,
        StreamsPageFragment.FilterType.MY_PAST,
        StreamsPageFragment.FilterType.OTHER_PAST
    )

    val tabTitles = listOf("🔴 Canlı", "📼 Geçmişlerim", "📺 Diğerleri")

    override fun getItemCount() = pages.size

    override fun createFragment(position: Int): Fragment =
        StreamsPageFragment.newInstance(pages[position])
}
