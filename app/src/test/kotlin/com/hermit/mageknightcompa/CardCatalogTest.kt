package com.hermit.mageknightcompa

import com.hermit.mageknightcompa.data.CardCategory
import com.hermit.mageknightcompa.data.CardRepository
import com.hermit.mageknightcompa.game.HEROES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Checks that assets/cards.json can be loaded and has no obvious mistakes. */
class CardCatalogTest {

    private val cards = CardRepository.parse(File("src/main/assets/cards.json").readText())

    @Test
    fun catalogIsNotEmpty() {
        assertTrue(cards.isNotEmpty())
    }

    @Test
    fun idsAreUnique() {
        val duplicates = cards.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertEquals(emptySet<String>(), duplicates)
    }

    @Test
    fun coloredCardsHaveAColor() {
        val colored = setOf(CardCategory.BASIC_ACTION, CardCategory.ADVANCED_ACTION, CardCategory.SPELL)
        val missing = cards.filter { it.category in colored && it.color == null }.map { it.id }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun basicActionsBelongToABaseGameHero() {
        val wrong = cards.filter { it.category == CardCategory.BASIC_ACTION && it.hero !in HEROES }.map { it.id }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun tacticsHaveARoundOrderNumber() {
        val wrong = cards.filter { it.category == CardCategory.TACTIC && it.number !in 1..6 }.map { it.id }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun imagesExist() {
        // Card pictures are kept out of the repository, so a fresh checkout has none to check
        val images = File("src/main/assets/cards").listFiles { f -> f.extension == "jpg" }.orEmpty()
        assumeTrue("No card images on this machine", images.isNotEmpty())
        val missing = cards.mapNotNull { it.image }.filterNot { File("src/main/assets/cards/$it").exists() }
        assertEquals(emptyList<String>(), missing)
    }
}
