package com.btx.tvlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val fixedWidthDp: Int?,
    private val onClick: (AppEntry) -> Unit,
    private val onLongClick: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.Holder>() {

    private var items: List<AppEntry> = emptyList()

    fun submit(list: List<AppEntry>) {
        items = list
        notifyDataSetChanged()
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.app_image)
        val label: TextView = view.findViewById(R.id.app_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        fixedWidthDp?.let {
            val density = parent.resources.displayMetrics.density
            view.layoutParams.width = (it * density).toInt()
        }
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = items[position]
        holder.label.text = entry.label

        val banner = entry.banner
        if (banner != null) {
            holder.image.setPadding(0, 0, 0, 0)
            holder.image.setImageDrawable(banner)
        } else {
            val pad = (12 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.image.setPadding(pad, pad, pad, pad)
            holder.image.setImageDrawable(entry.icon)
        }

        holder.itemView.setOnClickListener { onClick(entry) }
        holder.itemView.setOnLongClickListener {
            onLongClick(entry)
            true
        }
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            val scale = if (hasFocus) 1.08f else 1f
            v.animate().scaleX(scale).scaleY(scale).setDuration(120).start()
        }
    }

    override fun getItemCount(): Int = items.size
}
