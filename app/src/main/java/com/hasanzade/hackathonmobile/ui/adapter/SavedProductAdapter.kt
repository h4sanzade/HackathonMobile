package com.hasanzade.hackathonmobile.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hasanzade.hackathonmobile.R
import com.hasanzade.hackathonmobile.ui.SavedProductEntry

class SavedProductAdapter :
    ListAdapter<SavedProductEntry, SavedProductAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName   = view.findViewById<TextView>(R.id.tv_saved_product_name)
        private val tvDetail = view.findViewById<TextView>(R.id.tv_saved_product_detail)
        private val tvBatch  = view.findViewById<TextView>(R.id.tv_saved_batch_code)
        private val tvTime   = view.findViewById<TextView>(R.id.tv_time)

        fun bind(item: SavedProductEntry) {
            tvName.text   = item.productName
            tvDetail.text = "${item.category} · Qty: ${
                String.format("%.0f", item.quantity)}"
            tvBatch.text  = "Batch: ${item.batchCode}"
            tvTime.text   = item.savedAt
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SavedProductEntry>() {
        override fun areItemsTheSame(a: SavedProductEntry, b: SavedProductEntry) =
            a.batchCode == b.batchCode
        override fun areContentsTheSame(a: SavedProductEntry, b: SavedProductEntry) =
            a == b
    }
}