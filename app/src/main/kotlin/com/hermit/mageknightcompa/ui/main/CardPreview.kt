package com.hermit.mageknightcompa.ui.main

import android.app.Dialog
import android.graphics.Color
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hermit.mageknightcompa.R
import com.hermit.mageknightcompa.data.Card
import com.hermit.mageknightcompa.databinding.DialogCardPreviewBinding

/**
 * Shows [card] full screen with a [message]. With an [action], a button labelled
 * [actionLabel] runs it; otherwise the card can only be closed.
 */
fun Fragment.showCardPreview(
    card: Card,
    message: String,
    @StringRes actionLabel: Int? = null,
    action: (() -> Unit)? = null
) {
    val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    val binding = DialogCardPreviewBinding.inflate(layoutInflater)
    with(binding) {
        nameView.text = card.name
        messageView.text = message
        imageView.contentDescription = card.name
        if (card.image != null) {
            imageView.load("file:///android_asset/cards/${card.image}")
        } else {
            // Without a picture the card's color fills its place, keeping the buttons within reach
            imageView.setBackgroundColor(card.color?.let { ContextCompat.getColor(requireContext(), it.colorRes) } ?: Color.DKGRAY)
        }
        imageView.setOnClickListener { dialog.dismiss() }
        closeButton.setOnClickListener { dialog.dismiss() }
        if (action != null && actionLabel != null) {
            actionButton.setText(actionLabel)
            actionButton.setOnClickListener {
                dialog.dismiss()
                action()
            }
        } else {
            actionButton.visibility = View.GONE
        }
    }
    dialog.setContentView(binding.root)
    dialog.show()
}

/** Small pictures of the [cards], three to a row, for a dialog. */
fun Fragment.cardGrid(cards: List<Card>, onClick: (Int) -> Unit) = RecyclerView(requireContext()).apply {
    val padding = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
    setPadding(padding, padding / 2, padding, 0)
    layoutManager = GridLayoutManager(context, 3)
    adapter = CardGridAdapter(onClick).apply { rows = cards.map { CardRow(it, "") } }
}
