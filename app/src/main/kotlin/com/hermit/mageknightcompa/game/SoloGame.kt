package com.hermit.mageknightcompa.game

import com.hermit.mageknightcompa.data.Card
import com.hermit.mageknightcompa.data.CardCategory
import com.hermit.mageknightcompa.data.ManaColor
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * State of a solo game: the decks, the offers and the automated Dummy player.
 *
 * Decks and offers hold card ids. Index 0 of a deck is its top card; index 0 of an offer is
 * its top position (where new cards come in) and the last index is its lowest position.
 */
class SoloGame private constructor(
    private val catalog: Map<String, Card>,
    val playerHero: String,
    val dummyHero: String,
    round: Int,
    val aaDeck: MutableList<String>,
    val aaOffer: MutableList<String>,
    val spellDeck: MutableList<String>,
    val spellOffer: MutableList<String>,
    val regularUnitDeck: MutableList<String>,
    val eliteUnitDeck: MutableList<String>,
    val unitOffer: MutableList<String>,
    val artifactDeck: MutableList<String>,
    monasteries: Int,
    coreTileRevealed: Boolean,
    val conqueredCities: MutableSet<City>,
    val dummyDeck: MutableList<DummyCard>,
    val dummyDiscard: MutableList<DummyCard>,
    val dummyCrystals: MutableMap<ManaColor, Int>,
    dummyAnnouncedEnd: Boolean,
    lastDummyTurn: DummyTurn?,
    val removedTactics: MutableSet<Tactic>,
    playerTactic: Tactic?,
    dummyTactic: Tactic?
) {
    var round = round
        private set

    /** Number of monasteries on the map that have not been burned. */
    var monasteries = monasteries
        set(value) {
            field = value.coerceAtLeast(0)
        }

    var coreTileRevealed = coreTileRevealed

    var dummyAnnouncedEnd = dummyAnnouncedEnd
        private set
    var lastDummyTurn = lastDummyTurn
        private set
    var playerTactic = playerTactic
        private set
    var dummyTactic = dummyTactic
        private set

    /** Odd rounds are Day, even rounds are Night. */
    val isNight get() = round % 2 == 0

    fun card(id: String): Card = catalog.getValue(id)

    // --- Offers -------------------------------------------------------------------------------

    /** Takes a card from the Advanced Action offer and replenishes it. */
    fun takeAdvancedAction(index: Int): Card = takeAndReplenish(aaOffer, aaDeck, index)

    /** Takes a card from the Spell offer and replenishes it. */
    fun takeSpell(index: Int): Card = takeAndReplenish(spellOffer, spellDeck, index)

    /** Takes a Unit (or monastery Advanced Action). The offer is not replenished until next Round. */
    fun takeFromUnitOffer(index: Int): Card = card(unitOffer.removeAt(index))

    /** Takes the top card of the Advanced Action deck (Green city), or null if it is empty. */
    fun drawAdvancedActionFromDeck(): Card? {
        check(City.GREEN in conqueredCities) { "The Green city has not been conquered" }
        return aaDeck.removeFirstOrNull()?.let(::card)
    }

    /** Adds one Elite Unit to the Unit offer (White city). Returns false if none are left. */
    fun addEliteUnitToOffer(): Boolean {
        check(City.WHITE in conqueredCities) { "The White city has not been conquered" }
        val unit = eliteUnitDeck.removeFirstOrNull() ?: return false
        unitOffer.add(unit)
        return true
    }

    // --- Artifacts ----------------------------------------------------------------------------

    /**
     * The cards you look at when gaining [count] Artifacts: that many plus one from the top of
     * the deck (fewer if the deck runs low). Artifacts are never displayed in an offer.
     */
    fun peekArtifacts(count: Int): List<Card> = artifactDeck.take(count + 1).map(::card)

    /**
     * Gains [count] Artifacts: of the cards from [peekArtifacts], [returned] goes to the bottom
     * of the deck and you keep the rest. Returns the kept Artifacts.
     */
    fun gainArtifacts(count: Int, returned: String?): List<Card> {
        val drawn = artifactDeck.take(count + 1)
        // Only when there was a card to spare do you have to put one back
        require(if (drawn.size > count) returned in drawn else returned == null)
        repeat(drawn.size) { artifactDeck.removeAt(0) }
        returned?.let(artifactDeck::add)
        return drawn.toMutableList().apply { returned?.let(::remove) }.map(::card)
    }

    private fun takeAndReplenish(offer: MutableList<String>, deck: MutableList<String>, index: Int): Card {
        val taken = offer.removeAt(index)
        deck.removeFirstOrNull()?.let { offer.add(0, it) }
        return card(taken)
    }

    // --- Tactics ------------------------------------------------------------------------------

    /** Tactics of the current Day or Night that can still be picked. */
    fun availableTactics(): List<Tactic> = Tactic.entries.filter {
        it.night == isNight && it !in removedTactics && it != playerTactic && it != dummyTactic
    }

    /** In solo you always pick first; then the Dummy takes a random Tactic from the rest. */
    fun chooseTactic(tactic: Tactic, random: Random = Random) {
        check(playerTactic == null) { "Tactic already chosen this round" }
        require(tactic in availableTactics())
        playerTactic = tactic
        dummyTactic = availableTactics().randomOrNull(random)
    }

    /** True when the player plays before the Dummy this Round. */
    val playerGoesFirst: Boolean?
        get() {
            val mine = playerTactic ?: return null
            val dummy = dummyTactic ?: return true
            return mine.number < dummy.number
        }

    // --- Dummy player -------------------------------------------------------------------------

    /**
     * Plays one Dummy turn: flips three cards, then one more per crystal matching the color of
     * the last flipped card. With an empty deck the Dummy announces the End of the Round instead.
     */
    fun playDummyTurn(): DummyTurn {
        check(!dummyAnnouncedEnd) { "The Dummy already announced the End of the Round" }
        val turn = if (dummyDeck.isEmpty()) {
            dummyAnnouncedEnd = true
            DummyTurn(emptyList(), emptyList(), announcedEndOfRound = true)
        } else {
            val flipped = flip(3)
            val bonus = flip(dummyCrystals[flipped.last().color] ?: 0)
            DummyTurn(flipped, bonus, announcedEndOfRound = false)
        }
        lastDummyTurn = turn
        return turn
    }

    private fun flip(count: Int): List<DummyCard> {
        val cards = dummyDeck.take(count)
        repeat(cards.size) { dummyDeck.removeAt(0) }
        dummyDiscard.addAll(0, cards.reversed())
        return cards
    }

    // --- Round preparation --------------------------------------------------------------------

    /** Prepares the next Round: Unit offer, Advanced Action and Spell offers, Tactics and Dummy deck. */
    fun startNextRound(random: Random = Random) {
        listOfNotNull(playerTactic, dummyTactic).forEach(removedTactics::add)
        playerTactic = null
        dummyTactic = null
        round++

        refreshUnitOffer()

        // The lowest Advanced Action goes to the Dummy's deck instead of the bottom of the AA deck
        aaOffer.removeLastOrNull()?.let { dummyDiscard.add(card(it).toDummyCard()) }
        aaDeck.removeFirstOrNull()?.let { aaOffer.add(0, it) }

        // The lowest Spell goes to the bottom of the Spell deck and gives the Dummy a crystal
        spellOffer.removeLastOrNull()?.let { spell ->
            spellDeck.add(spell)
            val color = card(spell).color!!
            dummyCrystals[color] = (dummyCrystals[color] ?: 0) + 1
        }
        spellDeck.removeFirstOrNull()?.let { spellOffer.add(0, it) }

        dummyDeck.addAll(dummyDiscard)
        dummyDiscard.clear()
        dummyDeck.shuffle(random)
        dummyAnnouncedEnd = false
        lastDummyTurn = null
    }

    private fun refreshUnitOffer() {
        for (id in unitOffer) {
            val card = card(id)
            when {
                card.category == CardCategory.ADVANCED_ACTION -> aaDeck.add(id)
                card.elite -> eliteUnitDeck.add(id)
                else -> regularUnitDeck.add(id)
            }
        }
        unitOffer.clear()
        dealUnits()
        // Monastery cards are dealt before the AA offer is refreshed, so they get priority
        repeat(monasteries) { aaDeck.removeFirstOrNull()?.let(unitOffer::add) }
    }

    private fun dealUnits() {
        repeat(UNITS_IN_OFFER) { i ->
            // Once a Core tile is revealed, alternate Elite, Regular, Elite...
            val decks = if (coreTileRevealed && i % 2 == 0) {
                listOf(eliteUnitDeck, regularUnitDeck)
            } else {
                listOf(regularUnitDeck, eliteUnitDeck)
            }
            decks.firstNotNullOfOrNull { it.removeFirstOrNull() }?.let(unitOffer::add)
        }
    }

    private fun Card.toDummyCard() = DummyCard(name, color!!)

    // --- Saving -------------------------------------------------------------------------------

    fun toJson(): JSONObject = JSONObject()
        .put("playerHero", playerHero)
        .put("dummyHero", dummyHero)
        .put("round", round)
        .put("aaDeck", JSONArray(aaDeck))
        .put("aaOffer", JSONArray(aaOffer))
        .put("spellDeck", JSONArray(spellDeck))
        .put("spellOffer", JSONArray(spellOffer))
        .put("regularUnitDeck", JSONArray(regularUnitDeck))
        .put("eliteUnitDeck", JSONArray(eliteUnitDeck))
        .put("unitOffer", JSONArray(unitOffer))
        .put("artifactDeck", JSONArray(artifactDeck))
        .put("monasteries", monasteries)
        .put("coreTileRevealed", coreTileRevealed)
        .put("conqueredCities", JSONArray(conqueredCities.map { it.name }))
        .put("dummyDeck", dummyDeck.toJson())
        .put("dummyDiscard", dummyDiscard.toJson())
        .put("dummyCrystals", JSONObject().apply { dummyCrystals.forEach { (c, n) -> put(c.name, n) } })
        .put("dummyAnnouncedEnd", dummyAnnouncedEnd)
        .put("lastDummyTurn", lastDummyTurn?.let {
            JSONObject()
                .put("flipped", it.flipped.toJson())
                .put("bonus", it.bonus.toJson())
                .put("announcedEndOfRound", it.announcedEndOfRound)
        })
        .put("removedTactics", JSONArray(removedTactics.map { it.name }))
        .put("playerTactic", playerTactic?.name)
        .put("dummyTactic", dummyTactic?.name)

    companion object {
        /** In a solo game the Unit offer has the number of players plus 2 cards. */
        const val UNITS_IN_OFFER = 3
        private const val CARDS_IN_OFFER = 3

        /** Sets up a new solo game. [dummyCrystals] are the three colored dots on the Dummy's Hero card. */
        fun start(
            catalog: List<Card>,
            playerHero: String,
            dummyHero: String,
            dummyCrystals: List<ManaColor>,
            random: Random = Random
        ): SoloGame {
            fun deck(filter: (Card) -> Boolean) = shuffledDeck(catalog, random, filter)

            val aaDeck = deck { it.category == CardCategory.ADVANCED_ACTION }
            val spellDeck = deck { it.category == CardCategory.SPELL && !it.competitive }
            return SoloGame(
                catalog = catalog.associateBy { it.id },
                playerHero = playerHero,
                dummyHero = dummyHero,
                round = 1,
                aaDeck = aaDeck,
                aaOffer = aaDeck.takeFrom(CARDS_IN_OFFER),
                spellDeck = spellDeck,
                spellOffer = spellDeck.takeFrom(CARDS_IN_OFFER),
                regularUnitDeck = deck { it.category == CardCategory.UNIT && !it.elite },
                eliteUnitDeck = deck { it.category == CardCategory.UNIT && it.elite },
                unitOffer = mutableListOf(),
                artifactDeck = deck { it.category == CardCategory.ARTIFACT },
                monasteries = 0,
                coreTileRevealed = false,
                conqueredCities = mutableSetOf(),
                dummyDeck = BASIC_ACTIONS.shuffled(random).toMutableList(),
                dummyDiscard = mutableListOf(),
                dummyCrystals = dummyCrystals.groupingBy { it }.eachCount().toMutableMap(),
                dummyAnnouncedEnd = false,
                lastDummyTurn = null,
                removedTactics = mutableSetOf(),
                playerTactic = null,
                dummyTactic = null
            ).apply { dealUnits() }
        }

        fun fromJson(json: JSONObject, catalog: List<Card>): SoloGame {
            fun ids(key: String) = json.getJSONArray(key).let { a -> MutableList(a.length()) { a.getString(it) } }
            fun tactic(key: String) = json.optString(key).ifBlank { null }?.let { Tactic.valueOf(it) }
            val crystals = json.getJSONObject("dummyCrystals")
            return SoloGame(
                catalog = catalog.associateBy { it.id },
                playerHero = json.getString("playerHero"),
                dummyHero = json.getString("dummyHero"),
                round = json.getInt("round"),
                aaDeck = ids("aaDeck"),
                aaOffer = ids("aaOffer"),
                spellDeck = ids("spellDeck"),
                spellOffer = ids("spellOffer"),
                regularUnitDeck = ids("regularUnitDeck"),
                eliteUnitDeck = ids("eliteUnitDeck"),
                unitOffer = ids("unitOffer"),
                // Saves from before Artifacts were tracked get a freshly shuffled deck
                artifactDeck = if (json.has("artifactDeck")) ids("artifactDeck")
                else shuffledDeck(catalog, Random) { it.category == CardCategory.ARTIFACT },
                monasteries = json.getInt("monasteries"),
                coreTileRevealed = json.getBoolean("coreTileRevealed"),
                // Older saves may have no such key, or list cities that are no longer tracked
                conqueredCities = json.optJSONArray("conqueredCities")
                    ?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
                    .mapNotNull { name -> City.entries.find { it.name == name } }.toMutableSet(),
                dummyDeck = json.getJSONArray("dummyDeck").toDummyCards(),
                dummyDiscard = json.getJSONArray("dummyDiscard").toDummyCards(),
                dummyCrystals = crystals.keys().asSequence()
                    .associate { ManaColor.valueOf(it) to crystals.getInt(it) }.toMutableMap(),
                dummyAnnouncedEnd = json.getBoolean("dummyAnnouncedEnd"),
                lastDummyTurn = json.optJSONObject("lastDummyTurn")?.let {
                    DummyTurn(
                        it.getJSONArray("flipped").toDummyCards(),
                        it.getJSONArray("bonus").toDummyCards(),
                        it.getBoolean("announcedEndOfRound")
                    )
                },
                removedTactics = ids("removedTactics").map { Tactic.valueOf(it) }.toMutableSet(),
                playerTactic = tactic("playerTactic"),
                dummyTactic = tactic("dummyTactic")
            )
        }

        private fun shuffledDeck(catalog: List<Card>, random: Random, filter: (Card) -> Boolean) =
            catalog.filter(filter).flatMap { card -> List(card.copies) { card.id } }
                .shuffled(random).toMutableList()

        private fun MutableList<String>.takeFrom(count: Int): MutableList<String> =
            MutableList(minOf(count, size)) { removeAt(0) }

        private fun List<DummyCard>.toJson() =
            JSONArray(map { JSONObject().put("name", it.name).put("color", it.color.name) })

        private fun JSONArray.toDummyCards() = MutableList(length()) {
            val item = getJSONObject(it)
            DummyCard(item.getString("name"), ManaColor.valueOf(item.getString("color")))
        }
    }
}
