package com.hermit.mageknightcompa.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.hermit.mageknightcompa.R
import com.hermit.mageknightcompa.databinding.FragmentRoundBinding

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

        // While choosing it lists the available Tactics; afterwards, yours and the Dummy's
        val tacticAdapter = CardRowAdapter { index ->
            val game = viewModel.game.value ?: return@CardRowAdapter
            if (game.playerTactic == null) {
                val tactic = game.availableTactics()[index]
                viewModel.update { chooseTactic(tactic) }
            }
        }
        binding.tacticList.adapter = tacticAdapter

        viewModel.game.observe(viewLifecycleOwner) { game ->
            binding.root.visibility = if (game == null) View.INVISIBLE else View.VISIBLE
            if (game == null) return@observe
            binding.roundText.text = roundTitle(game.round, game.isNight)

            val choosing = game.playerTactic == null
            binding.tacticPrompt.visibility = if (choosing) View.VISIBLE else View.GONE
            if (choosing) {
                val tactics = game.availableTactics()
                binding.tacticPrompt.setText(if (tactics.isEmpty()) R.string.no_tactics_left else R.string.choose_tactic)
                tacticAdapter.rows = tactics.map { tactic ->
                    CardRow(tactic, getString(R.string.tactic_label, tactic.number, timeOfDay(tactic.night)))
                }
                binding.tacticsChosen.text = ""
            } else {
                val mine = game.tactic(game.playerTactic!!)
                val dummy = game.dummyTactic?.let(game::tactic)
                tacticAdapter.rows = listOfNotNull(
                    CardRow(mine, getString(R.string.your_tactic, mine.number)),
                    dummy?.let { CardRow(it, getString(R.string.dummy_tactic, it.number)) }
                )
                binding.tacticsChosen.text =
                    getString(if (game.playerGoesFirst == true) R.string.you_go_first else R.string.dummy_goes_first)
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

    private fun timeOfDay(night: Boolean) = getString(if (night) R.string.night else R.string.day)

    private fun roundTitle(round: Int, night: Boolean) =
        getString(R.string.round_title, round, timeOfDay(night))
}
