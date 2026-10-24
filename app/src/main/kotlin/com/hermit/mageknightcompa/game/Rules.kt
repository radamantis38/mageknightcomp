package com.hermit.mageknightcompa.game

import androidx.annotation.StringRes
import com.hermit.mageknightcompa.R
import com.hermit.mageknightcompa.data.Card
import com.hermit.mageknightcompa.data.CardCategory
import com.hermit.mageknightcompa.data.ManaColor

/** A card in the Dummy player's Deed deck. Only its color matters when it is flipped. */
data class DummyCard(val name: String, val color: ManaColor, val image: String? = null)

/**
 * Fallback starting deck for a Dummy Hero whose 16 Basic Actions are not all in the catalog yet.
 * Written from memory, so the catalog's cards take precedence as soon as they are added.
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

/** The three colored dots at the bottom of each Hero card: the crystals a Dummy Hero starts with. */
val HERO_CRYSTALS: Map<String, List<ManaColor>> = mapOf(
    "Arythea" to listOf(ManaColor.RED, ManaColor.RED, ManaColor.WHITE),
    "Goldyx" to listOf(ManaColor.GREEN, ManaColor.GREEN, ManaColor.BLUE),
    "Norowas" to listOf(ManaColor.WHITE, ManaColor.WHITE, ManaColor.GREEN),
    "Tovak" to listOf(ManaColor.BLUE, ManaColor.BLUE, ManaColor.RED)
)

/**
 * Fallback Tactic cards, used until all 6 Day and 6 Night Tactics are in the catalog. Written
 * from memory. Round Order goes from the lowest number to the highest.
 */
val DEFAULT_TACTICS: List<Card> = listOf(false, true).flatMap { night ->
    val names = if (night) {
        listOf("From the Dusk", "Long Night", "Mana Search", "Midnight Meditation", "Preparation", "Sparing Power")
    } else {
        listOf("Early Bird", "Rethink", "Mana Steal", "Planning", "Great Start", "The Right Moment")
    }
    names.mapIndexed { i, name ->
        Card(name.lowercase().replace(' ', '_'), name, CardCategory.TACTIC, number = i + 1, night = night)
    }
}

const val TACTICS_PER_TIME_OF_DAY = 6

/** What happened during one turn of the Dummy player. */
data class DummyTurn(
    /** The first (up to) three cards flipped. The last one decides the bonus flips. */
    val flipped: List<DummyCard>,
    /** Extra cards flipped for the crystals matching the last of [flipped]. */
    val bonus: List<DummyCard>,
    /** True when the deck was empty and the Dummy announced the End of the Round instead. */
    val announcedEndOfRound: Boolean
)
