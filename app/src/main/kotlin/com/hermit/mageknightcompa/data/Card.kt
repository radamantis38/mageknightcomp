package com.hermit.mageknightcompa.data

/**
 * A card from the catalog in assets/cards.json.
 *
 * [color] is set for Advanced Actions and Spells. [elite] and [copies] only matter for Units,
 * [competitive] only for Spells (competitive Spells are removed in solo games).
 * [image] is a file name inside assets/cards/, or null when the card has no picture yet.
 */
data class Card(
    val id: String,
    val name: String,
    val category: CardCategory,
    val color: ManaColor? = null,
    val elite: Boolean = false,
    val competitive: Boolean = false,
    val copies: Int = 1,
    val text: String = "",
    val image: String? = null
)
