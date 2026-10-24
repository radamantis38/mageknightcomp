package com.hermit.mageknightcompa.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.hermit.mageknightcompa.R
import com.hermit.mageknightcompa.databinding.FragmentRoundBinding

/** The draft, Tactic selection for the current Round and preparation of the next one. */
class RoundFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRoundBinding.inflate(inflater, container, false)
        val viewModel = ViewModelProvider(requireActivity())[GameViewModel::class.java]

        binding.nextRound.setOnClickListener {
            val game = viewModel.game.value ?: return@setOnClickListener
            if (!game.canStartNextRound) {
                val message = if (game.drafting) R.string.finish_draft_first else R.string.choose_tactic_first
                Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (game.mustShuffleInDraftedCard) {
                chooseDraftedCardToShuffleIn(viewModel)
                return@setOnClickListener
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.next_round)
                .setMessage(R.string.next_round_confirm)
                .setPositiveButton(R.string.next_round) { _, _ -> startNextRound(viewModel) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        // While drafting it lists the cards dealt and those set aside; afterwards, those still set aside
        lateinit var draftAdapter: CardGridAdapter
        draftAdapter = CardGridAdapter { index ->
            val game = viewModel.game.value ?: return@CardGridAdapter
            val (card, detail) = draftAdapter.rows[index]
            if (card.id in game.draftHand) {
                showCardPreview(card, getString(R.string.draft_return_message), R.string.shuffle_back) {
                    viewModel.update { returnDraftCard(card.id) }
                }
            } else {
                showCardPreview(card, detail)
            }
        }
        binding.draftList.adapter = draftAdapter

        // While choosing it lists the available Tactics; afterwards, yours and the Dummy's.
        // Tapping one shows it full screen, with a button to choose it while choosing.
        lateinit var tacticAdapter: CardGridAdapter
        tacticAdapter = CardGridAdapter { index ->
            val game = viewModel.game.value ?: return@CardGridAdapter
            val (tactic, detail) = tacticAdapter.rows[index]
            if (game.playerTactic == null) {
                showCardPreview(tactic, detail, R.string.choose) {
                    viewModel.update { chooseTactic(tactic) }
                    announceTurnOrder(viewModel)
                }
            } else {
                showCardPreview(tactic, detail)
            }
        }
        binding.tacticList.adapter = tacticAdapter

        viewModel.game.observe(viewLifecycleOwner) { game ->
            binding.root.visibility = if (game == null) View.INVISIBLE else View.VISIBLE
            if (game == null) return@observe
            binding.roundText.text = roundTitle(game.round, game.isNight)

            val showDraft = game.drafting || game.draftedCards.isNotEmpty()
            listOf(binding.draftHeader, binding.draftPrompt, binding.draftList).forEach {
                it.visibility = if (showDraft) View.VISIBLE else View.GONE
            }
            binding.draftHeader.setText(if (game.drafting) R.string.draft_at_start else R.string.drafted_cards)
            binding.draftPrompt.setText(if (game.drafting) R.string.draft_prompt else R.string.drafted_prompt)
            draftAdapter.rows = game.draftHand.map { CardRow(game.card(it), getString(game.card(it).color!!.labelRes)) } +
                game.draftedCards.map { CardRow(game.card(it), getString(R.string.set_aside_label)) }

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

    /** At the end of a Day: asks which drafted card goes into your deck, then prepares the next Round. */
    private fun chooseDraftedCardToShuffleIn(viewModel: GameViewModel) {
        val game = viewModel.game.value ?: return
        val drafted = game.draftedCards.map(game::card)
        lateinit var dialog: AlertDialog
        val grid = cardGrid(drafted) { i ->
            showCardPreview(drafted[i], getString(R.string.shuffle_in_message), R.string.shuffle_in) {
                dialog.dismiss()
                startNextRound(viewModel, drafted[i].id)
            }
        }
        dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.shuffle_in_title)
            .setMessage(R.string.shuffle_in_prompt)
            .setView(grid)
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startNextRound(viewModel: GameViewModel, shuffledIn: String? = null) {
        val game = viewModel.game.value ?: return
        val toDummy = game.aaOffer.lastOrNull()?.let { game.card(it) }
        val crystal = game.spellOffer.lastOrNull()?.let { game.card(it).color }
        viewModel.update { startNextRound(shuffledIn = shuffledIn) }
        val message = getString(
            R.string.next_round_done, game.round,
            toDummy?.name ?: "—",
            crystal?.let { getString(it.labelRes).lowercase() } ?: "—"
        )
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }
    }

    /** Pops up who starts the Round, and which Tactic the Dummy got. */
    private fun announceTurnOrder(viewModel: GameViewModel) {
        val game = viewModel.game.value ?: return
        val dummy = game.dummyTactic?.let(game::tactic)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (game.playerGoesFirst == true) R.string.you_go_first else R.string.dummy_goes_first)
            .apply { dummy?.let { setMessage(getString(R.string.dummy_took_tactic, it.name, it.number)) } }
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun timeOfDay(night: Boolean) = getString(if (night) R.string.night else R.string.day)

    private fun roundTitle(round: Int, night: Boolean) =
        getString(R.string.round_title, round, timeOfDay(night))
}
