package com.hasanzade.hackathonmobile.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hasanzade.hackathonmobile.R
import com.hasanzade.hackathonmobile.ui.BatchDisplayItem

class BatchAdapter : ListAdapter<BatchDisplayItem, BatchAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_batch, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName      = view.findViewById<TextView>(R.id.tv_batch_product_name)
        private val tvCategory  = view.findViewById<TextView>(R.id.tv_batch_department)
        private val tvUrgency   = view.findViewById<TextView>(R.id.tv_batch_urgency)
        private val tvArrival   = view.findViewById<TextView>(R.id.tv_batch_delivery_date)
        private val tvRemoval   = view.findViewById<TextView>(R.id.tv_batch_removal_date)
        private val tvExpiry    = view.findViewById<TextView>(R.id.tv_batch_expiry)
        private val tvQuantity  = view.findViewById<TextView>(R.id.tv_batch_quantity)
        private val tvValueAzn  = view.findViewById<TextView>(R.id.tv_batch_value_azn)
        private val tvDaysLeft  = view.findViewById<TextView>(R.id.tv_batch_days_left)

        fun bind(item: BatchDisplayItem) {
            tvName.text     = item.productName
            tvCategory.text = item.category.ifEmpty { item.batchCode }
            tvArrival.text  = item.arrivalDate
            tvRemoval.text  = item.removalDate
            tvExpiry.text   = item.expiryDate.ifEmpty { item.removalDate }
            tvQuantity.text = "${String.format("%.0f", item.quantity)} ${item.unit}"

            // AZN dəyəri
            val azn = if (item.sellPrice > 0)
                item.quantity * item.sellPrice
            else item.totalValueAzn
            tvValueAzn.text = "${String.format("%.2f", azn)} AZN"

            // Urgency badge
            when (item.urgency) {
                "CRITICAL" -> {
                    tvUrgency.text = "⚠ Kritik"
                    tvUrgency.setBackgroundColor(Color.parseColor("#DC2626"))
                }
                "WARNING" -> {
                    tvUrgency.text = "⚡ Xəbərdarlıq"
                    tvUrgency.setBackgroundColor(Color.parseColor("#D97706"))
                }
                else -> {
                    tvUrgency.text = "✓ Normal"
                    tvUrgency.setBackgroundColor(Color.parseColor("#16A34A"))
                }
            }

            // Gün qalıb
            tvDaysLeft.text = when {
                item.daysLeft <= 0 -> "Vaxtı keçib"
                item.daysLeft == 1 -> "1 gün qalıb"
                else               -> "${item.daysLeft} gün qalıb"
            }
            tvDaysLeft.setTextColor(
                when {
                    item.daysLeft <= 1 -> Color.parseColor("#DC2626")
                    item.daysLeft <= 3 -> Color.parseColor("#F97316")
                    else               -> Color.parseColor("#16A34A")
                }
            )
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BatchDisplayItem>() {
        override fun areItemsTheSame(a: BatchDisplayItem, b: BatchDisplayItem) =
            a.batchCode == b.batchCode
        override fun areContentsTheSame(a: BatchDisplayItem, b: BatchDisplayItem) =
            a == b
    }
}