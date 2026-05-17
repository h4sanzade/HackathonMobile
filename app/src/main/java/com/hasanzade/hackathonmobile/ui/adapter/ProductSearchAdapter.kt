package com.hasanzade.hackathonmobile.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hasanzade.hackathonmobile.R
import com.hasanzade.hackathonmobile.data.remote.dto.StockDto

class ProductSearchAdapter(
    private val onItemClick: (StockDto) -> Unit
) : ListAdapter<StockDto, ProductSearchAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName   = view.findViewById<TextView>(R.id.tv_item_product_name)
        private val tvDetail = view.findViewById<TextView>(R.id.tv_item_product_detail)

        fun bind(item: StockDto) {
            tvName.text   = item.productName ?: "--"
            tvDetail.text = "Barcode: ${item.barcode ?: "--"} | Stock: ${
                String.format("%.0f", item.totalStock ?: 0.0)} ${item.category ?: ""}"
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<StockDto>() {
        override fun areItemsTheSame(a: StockDto, b: StockDto) = a.productId == b.productId
        override fun areContentsTheSame(a: StockDto, b: StockDto) = a == b
    }
}