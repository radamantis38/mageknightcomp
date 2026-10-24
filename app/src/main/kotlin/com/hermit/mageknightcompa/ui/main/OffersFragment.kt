package com.hermit.mageknightcompa.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.hermit.mageknightcompa.R
import com.hermit.mageknightcompa.data.Card
import com.hermit.mageknightcompa.data.CardCategory
import com.hermit.mageknightcompa.databinding.FragmentOffersBinding
import com.hermit.mageknightcompa.game.City
import com.hermit.mageknightcompa.game.SoloGame

/** Shows the Advanced Action, Spell and Unit offers. Tapping a card takes it. */
class OffersFragment : Fragment() {

    private lateinit var viewModel: GameViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentOffersBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[GameViewModel::class.java]

        val aaAdapter = CardGridAdapter { index ->
            confirmTake(viewModel.game.value?.aaOffer?.get(index), R.string.take_aa_message) {
                takeAdvancedAction(index)
            }
        }
        val spellAdapter = CardGridAdapter { index ->
            confirmTake(viewModel.game.value?.spellOffer?.get(index), R.string.take_spell_message) {
                takeSpell(index)
            }
        }
        val unitAdapter = CardGridAdapter { index ->
            val id = viewModel.game.value?.unitOffer?.get(index)
            val isMonasteryCard = id != null && viewModel.game.value!!.card(id).category == CardCategory.ADVANCED_ACTION
            val message = if (isMonasteryCard) R.string.take_monastery_message else R.string.take_unit_message
            confirmTake(id, message) { takeFromUnitOffer(index) }
        }
        binding.aaList.adapter = aaAdapter
        binding.spellList.adapter = spellAdapter
        binding.unitList.adapter = unitAdapter

        binding.monasteryMinus.setOnClickListener { viewModel.update { monasteries-- } }
        binding.monasteryPlus.setOnClickListener { viewModel.update { monasteries++ } }
        binding.coreSwitch.setOnCheckedChangeListener { _, checked ->
            if (viewModel.game.value?.coreTileRevealed != checked) {
                viewModel.update { coreTileRevealed = checked }
            }
        }
        binding.gainArtifact.setOnClickListener { chooseArtifactCount() }

        val citySwitches =City.entries.associateWith { city ->
            SwitchMaterial(requireContext()).apply {
                setText(city.labelRes)
                setOnCheckedChangeListener { _, checked ->
                    val game = viewModel.game.value
                    if (game != null && (city in game.conqueredCities) != checked) {
                        viewModel.update { if (checked) conqueredCities += city else conqueredCities -= city }
                    }
                }
                binding.citySwitches.addView(this)
            }
        }
        binding.addElite.setOnClickListener {
            if (viewModel.update { addEliteUnitToOffer() } == false) {
                Snackbar.make(it, R.string.no_elite_units, Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.drawAa.setOnClickListener {
            val card = viewModel.update { drawAdvancedActionFromDeck() }
            val text = card?.let { c -> getString(R.string.taken, c.name) } ?: getString(R.string.aa_deck_empty)
            Snackbar.make(it, text, Snackbar.LENGTH_LONG).show()
        }

        viewModel.game.observe(viewLifecycleOwner) { game ->
            binding.root.visibility = if (game == null) View.INVISIBLE else View.VISIBLE
            if (game == null) return@observe
            binding.aaHeader.text = header(R.string.offer_advanced_actions, game.aaDeck.size)
                .let { if (game.drafting) "$it · ${getString(R.string.drafting_in_round_tab)}" else it }
            binding.spellHeader.text = header(R.string.offer_spells, game.spellDeck.size)
            binding.unitHeader.text = getString(R.string.offer_units)
            aaAdapter.rows = rankedRows(game, game.aaOffer)
            spellAdapter.rows = rankedRows(game, game.spellOffer)
            unitAdapter.rows = game.unitOffer.map { id ->
                val card = game.card(id)
                val detail = when {
                    card.category == CardCategory.ADVANCED_ACTION ->
                        "${getString(R.string.offer_monastery)} · ${getString(card.color!!.labelRes)}"
                    card.elite -> getString(R.string.offer_elite)
                    else -> getString(R.string.offer_regular)
                }
                CardRow(card, detail)
            }
            binding.artifactHeader.text = header(R.string.artifacts, game.artifactDeck.size)
            binding.gainArtifact.isEnabled = game.artifactDeck.isNotEmpty()
            binding.monasteriesText.text =getString(R.string.monasteries, game.monasteries)
            binding.coreSwitch.isChecked = game.coreTileRevealed
            citySwitches.forEach { (city, switch) -> switch.isChecked = city in game.conqueredCities }
            // You can only interact with a city once it has been conquered
            binding.addElite.visibility = if (City.WHITE in game.conqueredCities) View.VISIBLE else View.GONE
            binding.drawAa.visibility = if (City.GREEN in game.conqueredCities) View.VISIBLE else View.GONE
        }
        return binding.root
    }

    private fun header(@StringRes title: Int, deckSize: Int) =
        "${getString(title)} · ${getString(R.string.deck_count, deckSize)}"

    /** Rows for an offer whose position matters: the top one and the lowest one are marked. */
    private fun rankedRows(game: SoloGame, offer: List<String>) = offer.mapIndexed { i, id ->
        val card = game.card(id)
        val position = when (i) {
            offer.lastIndex -> getString(R.string.offer_lowest)
            0 -> getString(R.string.offer_top)
            else -> null
        }
        CardRow(card, listOfNotNull(getString(card.color!!.labelRes), position).joinToString(" · "))
    }

    private fun chooseArtifactCount() {
        val counts = (1..MAX_ARTIFACTS_AT_ONCE).map { it.toString() }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.how_many_artifacts)
            .setItems(counts) { _, i -> gainArtifacts(i + 1) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Draws [count] + 1 Artifacts, asks which one goes back to the bottom and keeps the rest. */
    private fun gainArtifacts(count: Int) {
        val drawn = viewModel.game.value?.peekArtifacts(count) ?: return
        if (drawn.size <= count) {
            showKeptArtifacts(viewModel.update { gainArtifacts(count, null) }.orEmpty())
            return
        }
        lateinit var dialog: AlertDialog
        val grid = cardGrid(drawn) { i ->
            showCardPreview(drawn[i], getString(R.string.artifact_return_message), R.string.return_to_bottom) {
                dialog.dismiss()
                showKeptArtifacts(viewModel.update { gainArtifacts(count, drawn[i].id) }.orEmpty())
            }
        }
        dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_artifact_to_return)
            .setView(grid)
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showKeptArtifacts(kept: List<Card>) {
        MaterialAlertDialogBuilder(requireContext())
            .apply {
                if (kept.isEmpty()) {
                    setMessage(R.string.artifact_deck_empty)
                } else {
                    setTitle(R.string.artifacts_kept_title)
                    setView(cardGrid(kept) { i -> showCardPreview(kept[i], getString(R.string.artifacts_kept_title)) })
                }
            }
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    /** Shows the card full screen, with what taking it does and a button to take it. */
    private fun confirmTake(id: String?, @StringRes message: Int, take: SoloGame.() -> Card) {
        val game = viewModel.game.value ?: return
        val card = game.card(id ?: return)
        showCardPreview(card, getString(message), R.string.take) {
            viewModel.update(take)
            view?.let { Snackbar.make(it, getString(R.string.taken, card.name), Snackbar.LENGTH_SHORT).show() }
        }
    }

    companion object {
        private const val MAX_ARTIFACTS_AT_ONCE = 3
    }
}
