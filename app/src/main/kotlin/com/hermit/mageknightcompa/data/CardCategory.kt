package com.hermit.mageknightcompa.data

import androidx.annotation.StringRes
import com.hermit.mageknightcompa.R

/**
 * Card categories. Basic Actions make up each Hero's starting deck; the others each have their
 * own deck, and all but Artifacts also have an offer.
 */
enum class CardCategory(@StringRes val titleRes: Int) {
    BASIC_ACTION(R.string.basic_actions),
    ADVANCED_ACTION(R.string.offer_advanced_actions),
    SPELL(R.string.offer_spells),
    UNIT(R.string.offer_units),
    ARTIFACT(R.string.artifacts),
    TACTIC(R.string.tactics)
}
