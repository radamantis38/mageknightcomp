package com.hermit.mageknightcompa.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hermit.mageknightcompa.data.CardRepository
import com.hermit.mageknightcompa.game.SoloGame
import org.json.JSONObject
import java.io.File

/**
 * Holds the current [SoloGame], shared by every tab. The game is saved to a file after each
 * change so it survives closing the app, and earlier states are kept for [undo].
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val catalog = CardRepository.get(application).cards
    private val saveFile = File(application.filesDir, SAVE_FILE)
    private val history = ArrayDeque<String>()

    private val _game = MutableLiveData<SoloGame?>(load())
    val game: LiveData<SoloGame?> = _game

    val canUndo get() = history.isNotEmpty()

    fun newGame(playerHero: String, dummyHero: String) {
        history.clear()
        publish(SoloGame.start(catalog, playerHero, dummyHero))
    }

    /** Runs [action] on the current game, keeping the previous state for [undo]. */
    fun <T> update(action: SoloGame.() -> T): T? {
        val game = _game.value ?: return null
        history.addLast(game.toJson().toString())
        if (history.size > MAX_UNDO) history.removeFirst()
        val result = game.action()
        publish(game)
        return result
    }

    fun undo(): Boolean {
        val previous = history.removeLastOrNull() ?: return false
        publish(SoloGame.fromJson(JSONObject(previous), catalog))
        return true
    }

    private fun publish(game: SoloGame) {
        saveFile.writeText(game.toJson().toString())
        _game.value = game
    }

    private fun load(): SoloGame? = try {
        if (saveFile.exists()) SoloGame.fromJson(JSONObject(saveFile.readText()), catalog) else null
    } catch (e: Exception) {
        // A save from an older version of the app: start over instead of crashing
        null
    }

    companion object {
        private const val SAVE_FILE = "solo_game.json"
        private const val MAX_UNDO = 50
    }
}
