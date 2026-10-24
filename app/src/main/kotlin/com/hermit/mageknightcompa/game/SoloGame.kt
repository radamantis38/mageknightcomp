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
    /** Ids of the Tactics already used, which are out of the game. */
    val removedTactics: MutableSet<String>,
    playerTactic: String?,
    dummyTactic: String?,
    /** Drafting at the Start: Advanced Actions dealt to you that you have not set aside yet. */
    val draftHand: MutableList<String>,
    /** Drafted Advanced Actions set aside; one is shuffled into your deck at the end of each Day. */
    val draftedCards: MutableList<String>
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
    /** Id of the Tactic you picked this Round. */
    var playerTactic = playerTactic
        private set
    /** Id of the Tactic the Dummy got this Round. */
    var dummyTactic = dummyTactic
        private set

    /** The catalog's Tactics once all 12 are there, [DEFAULT_TACTICS] until then. */
    private val tactics: List<Card> = catalog.values.filter { it.category == CardCategory.TACTIC }
        .takeIf { list -> listOf(false, true).all { night -> list.count { it.night == night } == TACTICS_PER_TIME_OF_DAY } }
        ?: DEFAULT_TACTICS

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

    // --- Drafting at the Start --------------------------------------------------------------

    /** True until you have set aside your drafted cards; the Advanced Action offer waits for it. */
    val drafting get() = draftHand.isNotEmpty()

    /**
     * Ends the draft: [returned] is shuffled back into the Advanced Action deck, you set aside the
     * rest of [draftHand], and the Advanced Action offer is revealed.
     */
    fun returnDraftCard(returned: String, random: Random = Random) {
        require(draftHand.remove(returned)) { "Card $returned is not in the draft" }
        draftedCards += draftHand
        draftHand.clear()
        aaDeck += returned
        aaDeck.shuffle(random)
        aaOffer += aaDeck.takeFrom(CARDS_IN_OFFER)
    }

    /** At the end of each Day you shuffle one of your drafted cards into your deck. */
    val mustShuffleInDraftedCard get() = !isNight && draftedCards.isNotEmpty()

    // --- Tactics ------------------------------------------------------------------------------

    /** The Tactic card with the given id. */
    fun tactic(id: String): Card =
        tactics.find { it.id == id } ?: DEFAULT_TACTICS.find { it.id == id } ?: error("Unknown Tactic $id")

    /** Tactics of the current Day or Night that can still be picked. */
    fun availableTactics(): List<Card> = tactics.filter {
        it.night == isNight && it.id !in removedTactics && it.id != playerTactic && it.id != dummyTactic
    }

    /** In solo you always pick first; then the Dummy takes a random Tactic from the rest. */
    fun chooseTactic(tactic: Card, random: Random = Random) {
        check(playerTactic == null) { "Tactic already chosen this round" }
        require(tactic in availableTactics())
        playerTactic = tactic.id
        dummyTactic = availableTactics().randomOrNull(random)?.id
    }

    /** True when the player plays before the Dummy this Round. */
    val playerGoesFirst: Boolean?
        get() {
            val mine = playerTactic ?: return null
            val dummy = dummyTactic ?: return true
            return tactic(mine).number!! < tactic(dummy).number!!
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

    /**
     * A new Round needs the draft to be over and this Round's Tactic, unless there were none
     * left to choose.
     */
    val canStartNextRound get() = !drafting && (playerTactic != null || availableTactics().isEmpty())

    /**
     * Prepares the next Round: Unit offer, Advanced Action and Spell offers, Tactics and Dummy deck.
     * At the end of a Day, [shuffledIn] is the drafted card you shuffle into your deck.
     */
    fun startNextRound(random: Random = Random, shuffledIn: String? = null) {
        check(canStartNextRound) { "Finish the draft and choose a Tactic before starting the next Round" }
        if (mustShuffleInDraftedCard) {
            require(draftedCards.remove(shuffledIn)) { "Choose a drafted card to shuffle into your deck" }
        } else {
            require(shuffledIn == null)
        }
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

    private fun Card.toDummyCard() = DummyCard(name, color!!, image)

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
        .put("removedTactics", JSONArray(removedTactics))
        .put("playerTactic", playerTactic)
        .put("dummyTactic", dummyTactic)
        .put("draftHand", JSONArray(draftHand))
        .put("draftedCards", JSONArray(draftedCards))

    companion object {
        /** In a solo game the Unit offer has the number of players plus 2 cards. */
        const val UNITS_IN_OFFER = 3
        private const val CARDS_IN_OFFER = 3
        /** Drafting at the Start, in solo: four Advanced Actions are dealt and you set aside three. */
        const val DRAFT_DEALT = 4

        /**
         * Sets up a new solo game. The Dummy starts with the crystals shown on its Hero card. With
         * [draft], your draft cards are dealt before the Advanced Action offer is revealed.
         */
        fun start(
            catalog: List<Card>,
            playerHero: String,
            dummyHero: String,
            random: Random = Random,
            draft: Boolean = false
        ): SoloGame {
            fun deck(filter: (Card) -> Boolean) = shuffledDeck(catalog, random, filter)

            val aaDeck = deck { it.category == CardCategory.ADVANCED_ACTION }
            val spellDeck = deck { it.category == CardCategory.SPELL }
            val draftHand = if (draft) aaDeck.takeFrom(DRAFT_DEALT) else mutableListOf()
            return SoloGame(
                catalog = catalog.associateBy { it.id },
                playerHero = playerHero,
                dummyHero = dummyHero,
                round = 1,
                aaDeck = aaDeck,
                aaOffer = if (draft) mutableListOf() else aaDeck.takeFrom(CARDS_IN_OFFER),
                spellDeck = spellDeck,
                spellOffer = spellDeck.takeFrom(CARDS_IN_OFFER),
                regularUnitDeck = deck { it.category == CardCategory.UNIT && !it.elite },
                eliteUnitDeck = deck { it.category == CardCategory.UNIT && it.elite },
                unitOffer = mutableListOf(),
                artifactDeck = deck { it.category == CardCategory.ARTIFACT },
                monasteries = 0,
                coreTileRevealed = false,
                conqueredCities = mutableSetOf(),
                dummyDeck = startingDeck(catalog, dummyHero).shuffled(random).toMutableList(),
                dummyDiscard = mutableListOf(),
                dummyCrystals = HERO_CRYSTALS.getValue(dummyHero).groupingBy { it }.eachCount().toMutableMap(),
                dummyAnnouncedEnd = false,
                lastDummyTurn = null,
                removedTactics = mutableSetOf(),
                playerTactic = null,
                dummyTactic = null,
                draftHand = draftHand,
                draftedCards = mutableListOf()
            ).apply { dealUnits() }
        }

        fun fromJson(json: JSONObject, catalog: List<Card>): SoloGame {
            fun ids(key: String) = json.getJSONArray(key).let { a -> MutableList(a.length()) { a.getString(it) } }
            val known = catalog.mapTo(HashSet()) { it.id }
            // A card that has since been removed from the catalog makes the save unusable
            fun cards(key: String) = ids(key).onEach { require(it in known) { "Card $it is no longer in the catalog" } }
            // Saves from before drafting was added have no draft
            fun optCards(key: String) = if (json.has(key)) cards(key) else mutableListOf()
            // Older saves stored Tactics as names like "PLANNING"; the id is the same in lowercase
            fun tactic(key: String) = json.optString(key).ifBlank { null }?.lowercase()
            val crystals = json.getJSONObject("dummyCrystals")
            return SoloGame(
                catalog = catalog.associateBy { it.id },
                playerHero = json.getString("playerHero"),
                dummyHero = json.getString("dummyHero"),
                round = json.getInt("round"),
                aaDeck = cards("aaDeck"),
                aaOffer = cards("aaOffer"),
                spellDeck = cards("spellDeck"),
                spellOffer = cards("spellOffer"),
                regularUnitDeck = cards("regularUnitDeck"),
                eliteUnitDeck = cards("eliteUnitDeck"),
                unitOffer = cards("unitOffer"),
                // Saves from before Artifacts were tracked get a freshly shuffled deck
                artifactDeck = if (json.has("artifactDeck")) cards("artifactDeck")
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
                removedTactics = ids("removedTactics").map { it.lowercase() }.toMutableSet(),
                playerTactic = tactic("playerTactic"),
                dummyTactic = tactic("dummyTactic"),
                draftHand = optCards("draftHand"),
                draftedCards = optCards("draftedCards")
            )
        }

        /**
         * The Hero's Basic Actions from the catalog, or [BASIC_ACTIONS] while that Hero's deck
         * has not been fully added yet.
         */
        fun startingDeck(catalog: List<Card>, hero: String): List<DummyCard> =
            catalog.filter { it.category == CardCategory.BASIC_ACTION && it.hero == hero }
                .map { DummyCard(it.name, it.color!!, it.image) }
                .takeIf { it.size == BASIC_ACTIONS.size } ?: BASIC_ACTIONS

        private fun shuffledDeck(catalog: List<Card>, random: Random, filter: (Card) -> Boolean) =
            catalog.filter(filter).map { it.id }.shuffled(random).toMutableList()

        private fun MutableList<String>.takeFrom(count: Int): MutableList<String> =
            MutableList(minOf(count, size)) { removeAt(0) }

        private fun List<DummyCard>.toJson() =
            JSONArray(map {
                JSONObject().put("name", it.name).put("color", it.color.name).put("image", it.image ?: JSONObject.NULL)
            })

        private fun JSONArray.toDummyCards() = MutableList(length()) {
            val item = getJSONObject(it)
            val image = if (item.isNull("image")) null else item.getString("image")
            DummyCard(item.getString("name"), ManaColor.valueOf(item.getString("color")), image)
        }
    }
}
