package com.hermit.mageknightcompa.ui.main

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hermit.mageknightcompa.R
import com.hermit.mageknightcompa.databinding.DialogNewGameBinding
import com.hermit.mageknightcompa.game.HEROES

/** Sets up a solo game: random, different Heroes for you and the Dummy, which can be changed. */
class NewGameDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogNewGameBinding.inflate(layoutInflater)
        val context = requireContext()

        fun Spinner.setItems(items: List<String>) {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
        }
        binding.playerHero.setItems(HEROES)
        binding.dummyHero.setItems(HEROES)
        // Both Heroes start out random (and different); either can still be changed
        val (player, dummy) = HEROES.indices.shuffled().take(2)
        binding.playerHero.setSelection(player)
        binding.dummyHero.setSelection(dummy)

        // The Dummy must be a Hero nobody is playing
        binding.playerHero.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (binding.dummyHero.selectedItemPosition == position) {
                    binding.dummyHero.setSelection(HEROES.indices.filter { it != position }.random())
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        isCancelable = ViewModelProvider(requireActivity())[GameViewModel::class.java].game.value != null
        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.new_game)
            .setView(binding.root)
            .setPositiveButton(R.string.start) { _, _ ->
                val playerHero = HEROES[binding.playerHero.selectedItemPosition]
                var dummyHero = HEROES[binding.dummyHero.selectedItemPosition]
                if (dummyHero == playerHero) dummyHero = (HEROES - playerHero).random()
                ViewModelProvider(requireActivity())[GameViewModel::class.java].newGame(playerHero, dummyHero)
            }
            .apply { if (isCancelable) setNegativeButton(R.string.cancel, null) }
            .create()
    }

    companion object {
        const val TAG = "new_game"
    }
}
