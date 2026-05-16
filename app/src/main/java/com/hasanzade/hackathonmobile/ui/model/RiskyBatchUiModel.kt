package com.hasanzade.hackathonmobile.ui.model

import android.graphics.Color

data class RiskyBatchUiModel(
    val productName: String,
    val batchId: String,
    val badgeText: String,
    val badgeColor: Int,
    val timeLeftLabel: String,
    val timeLeftColor: Int,
    val stockPercent: Int,
    val progressColor: Int
) {
    companion object {

        // Placeholder — API gələnə qədər
        fun placeholder() = listOf(
            RiskyBatchUiModel(
                productName   = "--",
                batchId       = "--",
                badgeText     = "--",
                badgeColor    = Color.parseColor("#9CA3AF"),
                timeLeftLabel = "--",
                timeLeftColor = Color.parseColor("#9CA3AF"),
                stockPercent  = 0,
                progressColor = Color.parseColor("#9CA3AF")
            )
        )

        // Backend response-dan map et
        fun fromReminder(
            productName: String,
            batchCode: String,
            daysLeft: Int,
            urgency: String,
            quantity: Double,
            originalQuantity: Double
        ): RiskyBatchUiModel {

            val (badgeText, badgeColor) = when (urgency) {
                "CRITICAL" -> "Expiring Soon" to Color.parseColor("#DC2626")
                "WARNING"  -> "Low Stock"     to Color.parseColor("#16A34A")
                else       -> "Quality Alert" to Color.parseColor("#D97706")
            }

            val timeLeftLabel = when {
                daysLeft <= 0 -> "Bu gün!"
                daysLeft == 1 -> "18 Hours Left"
                else          -> "$daysLeft Days Left"
            }

            val timeLeftColor = when {
                daysLeft <= 1 -> Color.parseColor("#F97316")
                daysLeft <= 2 -> Color.parseColor("#16A34A")
                else          -> Color.parseColor("#6B7280")
            }

            val stockPct = if (originalQuantity > 0)
                ((quantity / originalQuantity) * 100).toInt().coerceIn(0, 100)
            else 0

            val progressColor = when {
                stockPct < 20 -> Color.parseColor("#DC2626")
                stockPct < 50 -> Color.parseColor("#F97316")
                else          -> Color.parseColor("#16A34A")
            }

            return RiskyBatchUiModel(
                productName   = productName,
                batchId       = "#${batchCode.takeLast(7)}",
                badgeText     = badgeText,
                badgeColor    = badgeColor,
                timeLeftLabel = timeLeftLabel,
                timeLeftColor = timeLeftColor,
                stockPercent  = stockPct,
                progressColor = progressColor
            )
        }
    }
}