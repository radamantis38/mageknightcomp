package com.hermit.mageknightcompa.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hermit.mageknightcompa.data.Card
import com.hermit.mageknightcompa.databinding.TextRowItemBinding

/** A card shown in an offer, with a short [detail] line (color, position in the offer...). */
data class CardRow(val card: Card, val detail: String)

/** Lists [CardRow]s; tapping one calls [onClick] with its position. */
class CardRowAdapter(private val onClick: (Int) -> Unit) :
    RecyclerView.Adapter<CardRowAdapter.ViewHolder>() {

    var rows: List<CardRow> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ViewHolder(val binding: TextRowItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding = TextRowItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding).also { holder ->
            binding.root.setOnClickListener {
                holder.adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let(onClick)
            }
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val (card, detail) = rows[position]
        with(viewHolder.binding) {
            nameView.text = card.name
            textView.text = detail
            colorSwatch.visibility = if (card.color != null) View.VISIBLE else View.INVISIBLE
            card.color?.let { colorSwatch.setBackgroundColor(ContextCompat.getColor(root.context, it.colorRes)) }
            imageView.contentDescription = card.name
            if (card.image != null) {
                imageView.visibility = View.VISIBLE
                imageView.load("file:///android_asset/cards/${card.image}")
            } else {
                imageView.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = rows.size
}
