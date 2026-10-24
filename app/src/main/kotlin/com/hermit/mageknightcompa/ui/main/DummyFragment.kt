package com.hermit.mageknightcompa.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.hermit.mageknightcompa.R
import com.hermit.mageknightcompa.data.ManaColor
import com.hermit.mageknightcompa.databinding.FragmentDummyBinding
import com.hermit.mageknightcompa.game.DummyCard
import com.hermit.mageknightcompa.game.SoloGame

/** The automated Dummy player: its crystals, its deck and a button to play its turn. */
class DummyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDummyBinding.inflate(inflater, container, false)
        val viewModel = ViewModelProvider(requireActivity())[GameViewModel::class.java]

        binding.turnButton.setOnClickListener { viewModel.update { playDummyTurn() } }

        viewModel.game.observe(viewLifecycleOwner) { game ->
            binding.root.visibility = if (game == null) View.INVISIBLE else View.VISIBLE
            if (game == null) return@observe
            binding.heroText.text = getString(R.string.dummy_hero, game.dummyHero)
            binding.deckText.text = getString(R.string.dummy_deck, game.dummyDeck.size, game.dummyDiscard.size)
            showCrystals(binding.crystals, game)
            binding.turnButton.isEnabled = !game.dummyAnnouncedEnd
            showLastTurn(binding, game)
        }
        return binding.root
    }

    private fun showCrystals(container: LinearLayout, game: SoloGame) {
        container.removeAllViews()
        for (color in ManaColor.entries) {
            container.addView(colorChip(color, "${getString(color.labelRes)}\n${game.dummyCrystals[color] ?: 0}"),
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 })
        }
    }

    private fun showLastTurn(binding: FragmentDummyBinding, game: SoloGame) {
        binding.flippedCards.removeAllViews()
        val turn = game.lastDummyTurn
        binding.turnResult.text = when {
            turn == null -> getString(R.string.dummy_waiting)
            turn.announcedEndOfRound -> getString(R.string.dummy_ended_round)
            else -> {
                val decider = turn.flipped.last().color
                val crystals = game.dummyCrystals[decider] ?: 0
                val colorName = getString(decider.labelRes).lowercase()
                val lines = mutableListOf(getString(R.string.dummy_flipped, turn.flipped.names()))
                lines += if (turn.bonus.isEmpty() && crystals == 0) {
                    getString(R.string.dummy_no_bonus, colorName)
                } else {
                    getString(R.string.dummy_bonus, crystals, colorName, turn.bonus.size, turn.bonus.names())
                }
                if (game.dummyDeck.isEmpty()) lines += getString(R.string.dummy_last_cards)
                lines.joinToString("\n\n")
            }
        }
        val cards = turn?.let { it.flipped + it.bonus }.orEmpty()
        if (cards.all { it.image != null }) {
            showThumbnails(binding.flippedCards, cards)
        } else {
            cards.forEach { card ->
                binding.flippedCards.addView(colorChip(card.color, card.name),
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        .apply { bottomMargin = 8 })
            }
        }
    }

    /** Small card pictures, [THUMBNAILS_PER_ROW] to a row: just for show, the text above says it all. */
    private fun showThumbnails(container: LinearLayout, cards: List<DummyCard>) {
        val gap = resources.getDimensionPixelSize(R.dimen.thumbnail_gap)
        for (rowCards in cards.chunked(THUMBNAILS_PER_ROW)) {
            val row = LinearLayout(requireContext())
            for (index in 0 until THUMBNAILS_PER_ROW) {
                val card = rowCards.getOrNull(index)
                val thumbnail = ImageView(requireContext()).apply {
                    adjustViewBounds = true
                    contentDescription = card?.name
                    card?.let { load("file:///android_asset/cards/${it.image}") }
                }
                // Empty slots keep the last row's cards the same size as the others
                row.addView(thumbnail, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { if (index > 0) marginStart = gap })
            }
            container.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = gap })
        }
    }

    private fun List<DummyCard>.names() = joinToString { it.name }.ifEmpty { "—" }

    private fun colorChip(color: ManaColor, text: String) = TextView(requireContext()).apply {
        this.text = text
        gravity = Gravity.CENTER
        setPadding(16, 16, 16, 16)
        setBackgroundColor(ContextCompat.getColor(context, color.colorRes))
        setTextColor(if (color == ManaColor.WHITE) Color.BLACK else Color.WHITE)
    }

    private companion object {
        const val THUMBNAILS_PER_ROW = 5
    }
}
