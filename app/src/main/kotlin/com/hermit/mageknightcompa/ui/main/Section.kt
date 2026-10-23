package com.hermit.mageknightcompa.ui.main

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.hermit.mageknightcompa.R

/** The tabs at the top of the screen, in order. */
enum class Section(@StringRes val titleRes: Int, val create: () -> Fragment) {
    OFFERS(R.string.tab_offers, ::OffersFragment),
    DUMMY(R.string.tab_dummy, ::DummyFragment),
    ROUND(R.string.tab_round, ::RoundFragment)
}
