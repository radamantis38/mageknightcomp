package com.hermit.mageknightcompa.ui.main

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hermit.mageknightcompa.data.Card
import com.hermit.mageknightcompa.data.ManaColor
import com.hermit.mageknightcompa.databinding.CardGridItemBinding

/** A card shown in an offer, with a short [detail] line (color, position in the offer...). */
data class CardRow(val card: Card, val detail: String)

/** Small [CardRow]s for a grid; tapping one calls [onClick] with its position. */
class CardGridAdapter(private val onClick: (Int) -> Unit) :
    RecyclerView.Adapter<CardGridAdapter.ViewHolder>() {

    var rows: List<CardRow> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ViewHolder(val binding: CardGridItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding = CardGridItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding).also { holder ->
            binding.root.setOnClickListener {
                holder.adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let(onClick)
            }
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val (card, detail) = rows[position]
        with(viewHolder.binding) {
            detailView.text = detail
            detailView.visibility = if (detail.isEmpty()) View.GONE else View.VISIBLE
            imageView.contentDescription = card.name
            if (card.image != null) {
                imageView.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
                imageView.load("file:///android_asset/cards/${card.image}")
            } else {
                imageView.visibility = View.GONE
                placeholder.visibility = View.VISIBLE
                placeholder.text = card.name
                val color = card.color
                placeholder.setBackgroundColor(
                    if (color != null) ContextCompat.getColor(root.context, color.colorRes) else Color.DKGRAY
                )
                placeholder.setTextColor(if (color == ManaColor.WHITE) Color.BLACK else Color.WHITE)
            }
        }
    }

    override fun getItemCount() = rows.size
}
