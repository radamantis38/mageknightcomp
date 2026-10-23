package com.hermit.mageknightcompa

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.hermit.mageknightcompa.databinding.ActivityMainBinding
import com.hermit.mageknightcompa.ui.main.GameViewModel
import com.hermit.mageknightcompa.ui.main.NewGameDialogFragment
import com.hermit.mageknightcompa.ui.main.Section
import com.hermit.mageknightcompa.ui.main.SectionsPagerAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val viewModel = ViewModelProvider(this)[GameViewModel::class.java]

        binding.viewPager.adapter = SectionsPagerAdapter(this)
        // Navigation is done only through the tabs at the top, not by swiping
        binding.viewPager.isUserInputEnabled = false
        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = getString(Section.entries[position].titleRes)
        }.attach()

        binding.toolbar.inflateMenu(R.menu.main)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_undo -> {
                    if (!viewModel.undo()) {
                        Snackbar.make(binding.root, R.string.nothing_to_undo, Snackbar.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.action_new_game -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.new_game)
                        .setMessage(R.string.new_game_confirm)
                        .setPositiveButton(R.string.new_game) { _, _ -> showNewGameDialog() }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                    true
                }
                else -> false
            }
        }

        viewModel.game.observe(this) { game ->
            binding.noGame.visibility = if (game == null) View.VISIBLE else View.GONE
            binding.toolbar.subtitle = game?.let {
                getString(R.string.round_title, it.round, getString(if (it.isNight) R.string.night else R.string.day))
            }
            if (game == null) showNewGameDialog()
        }
    }

    private fun showNewGameDialog() {
        if (supportFragmentManager.findFragmentByTag(NewGameDialogFragment.TAG) == null) {
            NewGameDialogFragment().show(supportFragmentManager, NewGameDialogFragment.TAG)
        }
    }
}
