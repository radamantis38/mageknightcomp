package com.hermit.mageknightcompa.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.hermit.mageknightcompa.R
import com.hermit.mageknightcompa.databinding.FragmentRoundBinding
import com.hermit.mageknightcompa.game.Tactic

/** Tactic selection for the current Round and preparation of the next one. */
class RoundFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRoundBinding.inflate(inflater, container, false)
        val viewModel = ViewModelProvider(requireActivity())[GameViewModel::class.java]

        binding.nextRound.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.next_round)
                .setMessage(R.string.next_round_confirm)
                .setPositiveButton(R.string.next_round) { _, _ -> startNextRound(viewModel) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        viewModel.game.observe(viewLifecycleOwner) { game ->
            binding.root.visibility = if (game == null) View.INVISIBLE else View.VISIBLE
            if (game == null) return@observe
            binding.roundText.text = roundTitle(game.round, game.isNight)

            val choosing = game.playerTactic == null
            binding.tacticPrompt.visibility = if (choosing) View.VISIBLE else View.GONE
            binding.tacticButtons.removeAllViews()
            if (choosing) {
                val tactics = game.availableTactics()
                if (tactics.isEmpty()) binding.tacticPrompt.setText(R.string.no_tactics_left)
                else binding.tacticPrompt.setText(R.string.choose_tactic)
                tactics.forEach { tactic ->
                    binding.tacticButtons.addView(MaterialButton(requireContext()).apply {
                        text = label(tactic)
                        setOnClickListener { viewModel.update { chooseTactic(tactic) } }
                    }, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                binding.tacticsChosen.text = ""
            } else {
                binding.tacticsChosen.text = listOfNotNull(
                    getString(R.string.your_tactic, label(game.playerTactic!!)),
                    game.dummyTactic?.let { getString(R.string.dummy_tactic, label(it)) },
                    getString(if (game.playerGoesFirst == true) R.string.you_go_first else R.string.dummy_goes_first)
                ).joinToString("\n")
            }
        }
        return binding.root
    }

    private fun startNextRound(viewModel: GameViewModel) {
        val game = viewModel.game.value ?: return
        val toDummy = game.aaOffer.lastOrNull()?.let { game.card(it) }
        val crystal = game.spellOffer.lastOrNull()?.let { game.card(it).color }
        viewModel.update { startNextRound() }
        val message = getString(
            R.string.next_round_done, game.round,
            toDummy?.name ?: "—",
            crystal?.let { getString(it.labelRes).lowercase() } ?: "—"
        )
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }
    }

    private fun label(tactic: Tactic) = getString(R.string.tactic_label, tactic.number, tactic.displayName)

    private fun roundTitle(round: Int, night: Boolean) =
        getString(R.string.round_title, round, getString(if (night) R.string.night else R.string.day))
}
