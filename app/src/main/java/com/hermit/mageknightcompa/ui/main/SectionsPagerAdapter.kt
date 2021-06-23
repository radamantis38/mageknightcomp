package com.hermit.mageknightcompa.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hermit.mageknightcompa.R


/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(fa: FragmentActivity)
    : FragmentStateAdapter(fa) {



    override fun createFragment(position: Int ): Fragment {

        var cardList = ArrayList<CardModel>()
        val carta = CardModel("pokémon","pikachu")
        val carta2 = CardModel("yu-gi-oh","yugi")
        val carta3 = CardModel("Magic the gathering","magic")
        val carta4 = CardModel("keyforge","keyforge")

        cardList.add(carta)
        cardList.add(carta2)
        cardList.add(carta3)
        cardList.add(carta4)


        cardList.shuffle()
        return CardListFragment(cardList)
    }


    override fun getItemCount(): Int {
        // Show 2 total pages.
        return 4
    }
}