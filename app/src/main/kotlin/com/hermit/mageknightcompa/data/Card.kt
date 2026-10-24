package com.hermit.mageknightcompa.data

/**
 * A card from the catalog in assets/cards.json.
 *
 * Each physical card is its own entry, so two copies of a Unit are two cards with their own
 * [image]. [color] is set for Basic Actions, Advanced Actions and Spells, [elite] only matters
 * for Units and [hero] is the Hero whose starting deck a Basic Action belongs to. Tactics have
 * their [number] (Round Order, 1 to 6) and whether they are [night] Tactics.
 * The catalog only holds cards used in solo games.
 * [image] is a file name inside assets/cards/, or null when the card has no picture yet.
 */
data class Card(
    val id: String,
    val name: String,
    val category: CardCategory,
    val color: ManaColor? = null,
    val elite: Boolean = false,
    val hero: String? = null,
    val number: Int? = null,
    val night: Boolean = false,
    val text: String = "",
    val image: String? = null
)
