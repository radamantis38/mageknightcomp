package com.hermit.mageknightcompa.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Reads the card catalog from assets/cards.json once and keeps it in memory. */
class CardRepository private constructor(context: Context) {

    val cards: List<Card> by lazy {
        val json = context.assets.open(CATALOG_FILE).bufferedReader().use { it.readText() }
        // Builds without card pictures still list them in the catalog: show those cards without one
        val images = context.assets.list(IMAGE_DIR).orEmpty().toSet()
        parse(json).map { if (it.image in images) it else it.copy(image = null) }
    }

    companion object {
        private const val CATALOG_FILE = "cards.json"
        private const val IMAGE_DIR = "cards"

        @Volatile
        private var instance: CardRepository? = null

        fun get(context: Context): CardRepository =
            instance ?: synchronized(this) {
                instance ?: CardRepository(context.applicationContext).also { instance = it }
            }

        fun parse(json: String): List<Card> {
            val array = JSONArray(json)
            return List(array.length()) { i ->
                val item = array.getJSONObject(i)
                Card(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    category = CardCategory.valueOf(item.getString("category")),
                    color = item.optStringOrNull("color")?.let { ManaColor.valueOf(it) },
                    elite = item.optBoolean("elite"),
                    hero = item.optStringOrNull("hero"),
                    number = if (item.isNull("number")) null else item.optInt("number"),
                    night = item.optBoolean("night"),
                    text = item.optString("text"),
                    image = item.optStringOrNull("image")
                )
            }
        }

        /** optString() turns a JSON null into the text "null", so check for it first. */
        private fun JSONObject.optStringOrNull(key: String): String? =
            if (isNull(key)) null else optString(key).ifBlank { null }
    }
}
