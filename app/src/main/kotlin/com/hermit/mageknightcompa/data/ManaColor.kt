package com.hermit.mageknightcompa.data

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.hermit.mageknightcompa.R

/** The four basic mana colors. Action cards, Spells and crystals each have one. */
enum class ManaColor(@StringRes val labelRes: Int, @ColorRes val colorRes: Int) {
    RED(R.string.color_red, R.color.mana_red),
    BLUE(R.string.color_blue, R.color.mana_blue),
    WHITE(R.string.color_white, R.color.mana_white),
    GREEN(R.string.color_green, R.color.mana_green)
}
