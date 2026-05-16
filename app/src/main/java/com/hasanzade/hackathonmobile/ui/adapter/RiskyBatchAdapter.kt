package com.hasanzade.hackathonmobile.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hasanzade.hackathonmobile.R
import com.hasanzade.hackathonmobile.ui.model.RiskyBatchUiModel

class RiskyBatchAdapter :
    ListAdapter<RiskyBatchUiModel, RiskyBatchAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_risky_batch, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val divider      = view.findViewById<View>(R.id.divider)
        private val tvName       = view.findViewById<TextView>(R.id.tv_product_name)
        private val tvBadge      = view.findViewById<TextView>(R.id.tv_badge)
        private val tvBatchId    = view.findViewById<TextView>(R.id.tv_batch_id)
        private val progressBar  = view.findViewById<ProgressBar>(R.id.progress_batch)
        private val tvTimeLeft   = view.findViewById<TextView>(R.id.tv_time_left)

        fun bind(item: RiskyBatchUiModel, position: Int) {
            divider.visibility = if (position == 0) View.GONE else View.VISIBLE

            tvName.text    = item.productName
            tvBatchId.text = item.batchId
            tvTimeLeft.text = item.timeLeftLabel
            tvTimeLeft.setTextColor(item.timeLeftColor)

            // Badge
            tvBadge.text = item.badgeText
            tvBadge.setBackgroundColor(item.badgeColor)

            // Progress
            progressBar.progress = item.stockPercent
            progressBar.progressTintList =
                ColorStateList.valueOf(item.progressColor)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RiskyBatchUiModel>() {
        override fun areItemsTheSame(a: RiskyBatchUiModel, b: RiskyBatchUiModel) =
            a.batchId == b.batchId
        override fun areContentsTheSame(a: RiskyBatchUiModel, b: RiskyBatchUiModel) =
            a == b
    }
}