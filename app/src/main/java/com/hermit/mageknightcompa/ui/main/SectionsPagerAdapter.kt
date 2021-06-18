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
         var prueba :Array<String>
        var imagenes :Array<Int>
        when(position){
            1 -> {
                 prueba  =  arrayOf("test","test2","prueba3","prueba4")
                 imagenes  =  arrayOf(
                    R.drawable.zelda1,
                    R.drawable.zelda2,
                    R.drawable.zelda3,
                    R.drawable.zelda4)
            }

            2 -> {
                prueba  =  arrayOf("test2","test3","prueba4","prueba5")
                imagenes  =  arrayOf(
                    R.drawable.zelda2,
                    R.drawable.zelda3,
                    R.drawable.zelda4 ,
                    R.drawable.zelda1,)
            }

            3 -> {
                prueba  =  arrayOf("test26556","test65465","prueba45665","pruebaddd")
                imagenes  =  arrayOf(
                    R.drawable.zelda3,
                    R.drawable.zelda4 ,
                    R.drawable.zelda1,
                    R.drawable.zelda2,)
            }

            4 -> {
                prueba  =  arrayOf("test2","test3","prueba4","prueba5")
                imagenes  =  arrayOf(
                    R.drawable.zelda4,
                    R.drawable.zelda2,
                    R.drawable.zelda3,
                     R.drawable.zelda1,)
            }

            else -> {
                prueba  =  arrayOf("test2","test3","prueba4","prueba5")
                imagenes  =  arrayOf(
                    R.drawable.zelda4,
                    R.drawable.zelda2,
                    R.drawable.zelda3,
                    R.drawable.zelda1,)
            }
        }

        return CardListFragment(prueba,imagenes)
    }


    override fun getItemCount(): Int {
        // Show 2 total pages.
        return 4
    }
}