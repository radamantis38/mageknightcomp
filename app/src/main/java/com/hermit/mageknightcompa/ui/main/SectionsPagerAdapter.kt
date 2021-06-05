package com.hermit.mageknightcompa.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter


/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(fa: FragmentActivity)
    : FragmentStateAdapter(fa) {

    override fun createFragment(position: Int): Fragment {
        return CardListFragment.newInstance(position + 1)
    }


    override fun getItemCount(): Int {
        // Show 2 total pages.
        return 4
    }
}