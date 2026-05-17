package com.hasanzade.hackathonmobile.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hasanzade.hackathonmobile.R
import com.hasanzade.hackathonmobile.domain.model.WasteLogModel

class WasteLogAdapter :
    ListAdapter<WasteLogModel, WasteLogAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_waste_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName   = view.findViewById<TextView>(R.id.tv_waste_product_name)
        private val tvDetail = view.findViewById<TextView>(R.id.tv_waste_detail)
        private val tvLoss   = view.findViewById<TextView>(R.id.tv_waste_loss)

        fun bind(item: WasteLogModel) {
            tvName.text   = item.productName
            tvDetail.text = "${item.departmentName} · ${
                item.reason.lowercase().replaceFirstChar { it.uppercase() }
            } · ${String.format("%.1f", item.quantity)} ədəd"
            tvLoss.text   = "-${String.format("%.2f", item.totalLoss)} AZN"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WasteLogModel>() {
        override fun areItemsTheSame(a: WasteLogModel, b: WasteLogModel) =
            a.productName == b.productName && a.wasteDate == b.wasteDate
        override fun areContentsTheSame(a: WasteLogModel, b: WasteLogModel) = a == b
    }
}