package com.hermit.mageknightcompa

import com.hermit.mageknightcompa.data.Card
import com.hermit.mageknightcompa.data.CardCategory
import com.hermit.mageknightcompa.data.CardRepository
import com.hermit.mageknightcompa.data.ManaColor
import com.hermit.mageknightcompa.game.BASIC_ACTIONS
import com.hermit.mageknightcompa.game.City
import com.hermit.mageknightcompa.game.DEFAULT_TACTICS
import com.hermit.mageknightcompa.game.DummyCard
import com.hermit.mageknightcompa.game.SoloGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class SoloGameTest {

    private val catalog = CardRepository.parse(File("src/main/assets/cards.json").readText())

    private fun newGame() = SoloGame.start(catalog, "Tovak", "Goldyx", Random(42))

    /** Picks a Tactic if this Round still needs one, then starts the next Round. */
    private fun SoloGame.nextRound(random: Random) {
        if (!canStartNextRound) chooseTactic(availableTactics().first(), random)
        startNextRound(random)
    }

    @Test
    fun setupDealsThreeCardsToEachOffer() {
        val game = newGame()
        assertEquals(3, game.aaOffer.size)
        assertEquals(3, game.spellOffer.size)
        assertEquals(3, game.unitOffer.size)
        assertTrue(game.unitOffer.none { game.card(it).elite })
        assertEquals(16, game.dummyDeck.size)
        assertEquals(mapOf(ManaColor.GREEN to 2, ManaColor.BLUE to 1), game.dummyCrystals)
    }

    @Test
    fun dummyStartsWithTheCrystalsOnItsHeroCard() {
        val crystals = { hero: String -> SoloGame.start(catalog, "Goldyx", hero, Random(1)).dummyCrystals }
        assertEquals(mapOf(ManaColor.RED to 2, ManaColor.WHITE to 1), crystals("Arythea"))
        assertEquals(mapOf(ManaColor.WHITE to 2, ManaColor.GREEN to 1), crystals("Norowas"))
        assertEquals(mapOf(ManaColor.BLUE to 2, ManaColor.RED to 1), crystals("Tovak"))
    }

    @Test
    fun dummyDeckUsesItsHerosBasicActionsFromTheCatalog() {
        val goldyx = List(16) { i ->
            Card("goldyx_card_$i", "Goldyx card $i", CardCategory.BASIC_ACTION, ManaColor.entries[i % 4], hero = "Goldyx")
        }
        val tovak = Card("tovak_card", "Tovak card", CardCategory.BASIC_ACTION, ManaColor.RED, hero = "Tovak")
        val withoutBasics = catalog.filter { it.category != CardCategory.BASIC_ACTION }
        val game = SoloGame.start(withoutBasics + goldyx + tovak, "Tovak", "Goldyx", Random(1))
        assertEquals(goldyx.map { it.name }.toSet(), game.dummyDeck.map { it.name }.toSet())
    }

    @Test
    fun dummyDeckFallsBackToDefaultBasicActionsUntilTheHerosDeckIsComplete() {
        val partial = Card("goldyx_rage", "Rage", CardCategory.BASIC_ACTION, ManaColor.RED, hero = "Goldyx")
        val withoutBasics = catalog.filter { it.category != CardCategory.BASIC_ACTION }
        val game = SoloGame.start(withoutBasics + partial, "Tovak", "Goldyx", Random(1))
        assertEquals(BASIC_ACTIONS.sortedBy { it.name }, game.dummyDeck.sortedBy { it.name })
    }

    @Test
    fun takingFromAnOfferPutsTheNewCardOnTop() {
        val game = newGame()
        val nextCard = game.aaDeck.first()
        val lowest = game.aaOffer.last()
        game.takeAdvancedAction(1)
        assertEquals(listOf(nextCard), game.aaOffer.take(1))
        assertEquals(lowest, game.aaOffer.last())
        assertEquals(3, game.aaOffer.size)
    }

    @Test
    fun unitOfferIsNotReplenishedUntilNextRound() {
        val game = newGame()
        game.takeFromUnitOffer(0)
        assertEquals(2, game.unitOffer.size)
        game.nextRound(Random(1))
        assertEquals(3, game.unitOffer.size)
    }

    @Test
    fun dummyFlipsBonusCardsForMatchingCrystals() {
        val game = newGame()
        game.dummyDeck.clear()
        game.dummyDeck += listOf(
            DummyCard("a", ManaColor.RED), DummyCard("b", ManaColor.RED), DummyCard("c", ManaColor.GREEN),
            DummyCard("d", ManaColor.WHITE), DummyCard("e", ManaColor.WHITE), DummyCard("f", ManaColor.WHITE)
        )
        val turn = game.playDummyTurn()
        assertEquals(listOf("a", "b", "c"), turn.flipped.map { it.name })
        assertEquals(listOf("d", "e"), turn.bonus.map { it.name })
        assertEquals(1, game.dummyDeck.size)
        assertEquals("e", game.dummyDiscard.first().name)
    }

    @Test
    fun dummyWithoutMatchingCrystalsFlipsOnlyThree() {
        val game = newGame()
        game.dummyDeck.clear()
        game.dummyDeck += List(5) { DummyCard("r$it", ManaColor.RED) }
        val turn = game.playDummyTurn()
        assertEquals(3, turn.flipped.size)
        assertTrue(turn.bonus.isEmpty())
    }

    @Test
    fun dummyAnnouncesEndOfRoundWhenDeckIsEmpty() {
        val game = newGame()
        var turns = 0
        while (!game.dummyAnnouncedEnd) {
            game.playDummyTurn()
            turns++
        }
        assertTrue(game.lastDummyTurn!!.announcedEndOfRound)
        assertTrue(game.dummyDeck.isEmpty())
        assertEquals(16, game.dummyDiscard.size)
        assertTrue(turns in 2..7)
    }

    @Test
    fun newRoundGivesLowestAdvancedActionToDummyAndCrystalForLowestSpell() {
        val game = newGame()
        val lowestAa = game.card(game.aaOffer.last())
        val lowestSpell = game.card(game.spellOffer.last())
        val crystalsBefore = game.dummyCrystals[lowestSpell.color] ?: 0
        game.playDummyTurn()

        game.nextRound(Random(1))

        assertEquals(17, game.dummyDeck.size)
        assertTrue(game.dummyDiscard.isEmpty())
        assertTrue(game.dummyDeck.any { it.name == lowestAa.name && it.color == lowestAa.color })
        assertFalse(lowestAa.id in game.aaOffer)
        assertEquals(lowestSpell.id, game.spellDeck.last())
        assertEquals(crystalsBefore + 1, game.dummyCrystals[lowestSpell.color])
        assertEquals(3, game.aaOffer.size)
        assertEquals(3, game.spellOffer.size)
        assertTrue(game.isNight)
    }

    @Test
    fun coreTileAlternatesEliteAndRegularUnits() {
        val game = newGame()
        game.coreTileRevealed = true
        game.nextRound(Random(1))
        assertEquals(listOf(true, false, true), game.unitOffer.map { game.card(it).elite })
    }

    @Test
    fun monasteriesAddAdvancedActionsToUnitOffer() {
        val game = newGame()
        game.monasteries = 2
        game.nextRound(Random(1))
        assertEquals(5, game.unitOffer.size)
        assertEquals(2, game.unitOffer.count { game.card(it).category == CardCategory.ADVANCED_ACTION })
        // Next round they go back to the bottom of the AA deck
        val monasteryCards = game.unitOffer.filter { game.card(it).category == CardCategory.ADVANCED_ACTION }
        game.monasteries = 0
        game.nextRound(Random(2))
        assertTrue(monasteryCards.all { it in game.aaDeck })
    }

    @Test(expected = IllegalStateException::class)
    fun eliteUnitsNeedTheWhiteCity() {
        newGame().addEliteUnitToOffer()
    }

    @Test(expected = IllegalStateException::class)
    fun drawingFromAdvancedActionDeckNeedsTheGreenCity() {
        newGame().drawAdvancedActionFromDeck()
    }

    @Test
    fun conqueredCitiesUnlockTheirOptions() {
        val game = newGame()
        game.conqueredCities += City.WHITE
        game.conqueredCities += City.GREEN
        assertTrue(game.addEliteUnitToOffer())
        assertTrue(game.card(game.unitOffer.last()).elite)
        val top = game.aaDeck.first()
        assertEquals(top, game.drawAdvancedActionFromDeck()!!.id)
    }

    @Test
    fun loadsSavesWithoutConqueredCities() {
        val json = newGame().toJson().apply { remove("conqueredCities") }
        assertTrue(SoloGame.fromJson(json, catalog).conqueredCities.isEmpty())
    }

    @Test
    fun ignoresCitiesThatAreNoLongerTracked() {
        val json = newGame().toJson().put("conqueredCities", org.json.JSONArray(listOf("RED", "GREEN")))
        assertEquals(setOf(City.GREEN), SoloGame.fromJson(json, catalog).conqueredCities)
    }

    @Test
    fun aNewRoundNeedsATacticFirst() {
        val game = newGame()
        assertFalse(game.canStartNextRound)
        assertThrows(IllegalStateException::class.java) { game.startNextRound(Random(1)) }
        assertEquals(1, game.round)
        game.chooseTactic(game.availableTactics().first(), Random(1))
        assertTrue(game.canStartNextRound)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSavesWithCardsNoLongerInTheCatalog() {
        val game = newGame()
        SoloGame.fromJson(game.toJson(), catalog.filter { it.id != game.spellOffer.first() })
    }

    @Test
    fun gainingAnArtifactDrawsTwoAndReturnsOneToTheBottom() {
        val game = newGame()
        val deckSize = game.artifactDeck.size
        val drawn = game.peekArtifacts(1)
        assertEquals(2, drawn.size)
        val kept = game.gainArtifacts(1, drawn[0].id)
        assertEquals(listOf(drawn[1]), kept)
        assertEquals(drawn[0].id, game.artifactDeck.last())
        assertEquals(deckSize - 1, game.artifactDeck.size)
    }

    @Test
    fun gainingArtifactsKeepsAllWhenTheDeckRunsLow() {
        val game = newGame()
        while (game.artifactDeck.size > 1) game.artifactDeck.removeAt(0)
        val last = game.artifactDeck.single()
        assertEquals(listOf(last), game.gainArtifacts(1, null).map { it.id })
        assertTrue(game.artifactDeck.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun mustReturnOneOfTheDrawnArtifacts() {
        val game = newGame()
        game.gainArtifacts(1, game.artifactDeck.last())
    }

    @Test
    fun loadsSavesWithoutArtifactDeck() {
        val json = newGame().toJson().apply { remove("artifactDeck") }
        val expected = catalog.count { it.category == CardCategory.ARTIFACT }
        assertEquals(expected, SoloGame.fromJson(json, catalog).artifactDeck.size)
    }

    @Test
    fun eachTacticIsPickedOncePerGame() {
        val game = newGame()
        repeat(3) {
            game.chooseTactic(game.availableTactics().first(), Random(it))
            assertNotEquals(game.playerTactic, game.dummyTactic)
            game.nextRound(Random(it)) // Night
            game.chooseTactic(game.availableTactics().first(), Random(it))
            game.nextRound(Random(it)) // Day
        }
        // 6 Day and 6 Night Tactics, each used once, whether they come from the catalog or not
        assertEquals(12, game.removedTactics.size)
        assertTrue(game.availableTactics().isEmpty())
    }

    @Test
    fun usesTheCatalogsTacticsOnceAllTwelveAreThere() {
        val own = DEFAULT_TACTICS.map { it.copy(id = "my_${it.id}", image = "${it.id}.jpg") }
        val withoutTactics = catalog.filter { it.category != CardCategory.TACTIC }
        val complete = SoloGame.start(withoutTactics + own, "Tovak", "Goldyx", Random(1))
        assertTrue(complete.availableTactics().all { it.id.startsWith("my_") && it.image != null })
        val partial = SoloGame.start(withoutTactics + own.take(5), "Tovak", "Goldyx", Random(1))
        assertEquals(DEFAULT_TACTICS.filter { !it.night }, partial.availableTactics())
    }

    @Test
    fun playerGoesFirstWithTheLowerTacticNumber() {
        val game = newGame()
        game.chooseTactic(game.availableTactics().first { it.number == 1 }, Random(1))
        assertEquals(true, game.playerGoesFirst)
    }

    @Test
    fun loadsTacticsSavedByOlderVersions() {
        val json = newGame().toJson()
            .put("playerTactic", "PLANNING")
            .put("removedTactics", org.json.JSONArray(listOf("EARLY_BIRD")))
        val loaded = SoloGame.fromJson(json, catalog)
        assertEquals("Planning", loaded.tactic(loaded.playerTactic!!).name)
        assertEquals(setOf("early_bird"), loaded.removedTactics)
    }

    @Test
    fun cardsAreConservedAcrossRounds() {
        val game = newGame()
        val aaTotal = catalog.count { it.category == CardCategory.ADVANCED_ACTION }
        game.monasteries = 1
        game.coreTileRevealed = true
        repeat(4) { game.nextRound(Random(it)) }
        val aaInDummy = game.dummyDeck.size - 16
        assertEquals(4, aaInDummy)
        val aaOnTable = (game.aaDeck + game.aaOffer + game.unitOffer).count {
            game.card(it).category == CardCategory.ADVANCED_ACTION
        }
        assertEquals(aaTotal, aaOnTable + aaInDummy)
    }

    @Test
    fun survivesSaveAndLoad() {
        val game = newGame()
        game.chooseTactic(game.availableTactics().first { it.name == "Planning" }, Random(3))
        game.playDummyTurn()
        game.monasteries = 1
        game.conqueredCities += City.WHITE
        val json = game.toJson().toString()
        val loaded = SoloGame.fromJson(org.json.JSONObject(json), catalog)
        assertEquals(json, loaded.toJson().toString())
    }
}
