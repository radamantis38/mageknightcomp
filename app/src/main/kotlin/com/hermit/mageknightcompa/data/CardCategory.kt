package com.hermit.mageknightcompa.data

import androidx.annotation.StringRes
import com.hermit.mageknightcompa.R

/** Card categories, each with its own deck. All but Artifacts also have an offer. */
enum class CardCategory(@StringRes val titleRes: Int) {
    ADVANCED_ACTION(R.string.offer_advanced_actions),
    SPELL(R.string.offer_spells),
    UNIT(R.string.offer_units),
    ARTIFACT(R.string.artifacts)
}
