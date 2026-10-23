package com.hermit.mageknightcompa

import com.hermit.mageknightcompa.data.CardCategory
import com.hermit.mageknightcompa.data.CardRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun advancedActionsAndSpellsHaveAColor() {
        val colored = setOf(CardCategory.ADVANCED_ACTION, CardCategory.SPELL)
        val missing = cards.filter { it.category in colored && it.color == null }.map { it.id }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun imagesExist() {
        val missing = cards.mapNotNull { it.image }.filterNot { File("src/main/assets/cards/$it").exists() }
        assertEquals(emptyList<String>(), missing)
    }
}
