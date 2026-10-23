package com.hermit.mageknightcompa.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/** One page per [Section]. */
class SectionsPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun createFragment(position: Int): Fragment = Section.entries[position].create()

    override fun getItemCount(): Int = Section.entries.size
}
