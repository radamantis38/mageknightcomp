package com.hermit.mageknightcompa.game

import androidx.annotation.StringRes
import com.hermit.mageknightcompa.R
import com.hermit.mageknightcompa.data.ManaColor

/** A card in the Dummy player's Deed deck. Only its color matters when it is flipped. */
data class DummyCard(val name: String, val color: ManaColor)

/**
 * The 16 Basic Action cards every Hero starts with. Each Hero swaps one of them for a
 * Hero-specific card of the same color, so the colors are the same for every Hero.
 */
val BASIC_ACTIONS: List<DummyCard> = listOf(
    "Rage" to ManaColor.RED, "Rage" to ManaColor.RED,
    "Threaten" to ManaColor.RED, "Improvisation" to ManaColor.RED,
    "Determination" to ManaColor.BLUE, "Stamina" to ManaColor.BLUE,
    "Stamina" to ManaColor.BLUE, "Crystallize" to ManaColor.BLUE,
    "Swiftness" to ManaColor.WHITE, "Swiftness" to ManaColor.WHITE,
    "Promise" to ManaColor.WHITE, "Mana Draw" to ManaColor.WHITE,
    "March" to ManaColor.GREEN, "March" to ManaColor.GREEN,
    "Tranquility" to ManaColor.GREEN, "Concentration" to ManaColor.GREEN
).map { (name, color) -> DummyCard(name, color) }

/**
 * Cities whose interaction options change the offers. You can only interact with a city after
 * it has been conquered.
 */
enum class City(@StringRes val labelRes: Int) {
    WHITE(R.string.city_white),
    GREEN(R.string.city_green)
}

/** Heroes of the base game. */
val HEROES = listOf("Arythea", "Goldyx", "Norowas", "Tovak")

/** Tactic cards. Round Order goes from the lowest [number] to the highest. */
enum class Tactic(val number: Int, val displayName: String, val night: Boolean) {
    EARLY_BIRD(1, "Early Bird", false),
    RETHINK(2, "Rethink", false),
    MANA_STEAL(3, "Mana Steal", false),
    PLANNING(4, "Planning", false),
    GREAT_START(5, "Great Start", false),
    THE_RIGHT_MOMENT(6, "The Right Moment", false),
    FROM_THE_DUSK(1, "From the Dusk", true),
    LONG_NIGHT(2, "Long Night", true),
    MANA_SEARCH(3, "Mana Search", true),
    MIDNIGHT_MEDITATION(4, "Midnight Meditation", true),
    PREPARATION(5, "Preparation", true),
    SPARING_POWER(6, "Sparing Power", true)
}

/** What happened during one turn of the Dummy player. */
data class DummyTurn(
    /** The first (up to) three cards flipped. The last one decides the bonus flips. */
    val flipped: List<DummyCard>,
    /** Extra cards flipped for the crystals matching the last of [flipped]. */
    val bonus: List<DummyCard>,
    /** True when the deck was empty and the Dummy announced the End of the Round instead. */
    val announcedEndOfRound: Boolean
)
