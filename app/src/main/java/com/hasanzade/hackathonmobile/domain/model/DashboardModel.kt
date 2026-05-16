package com.hasanzade.hackathonmobile.domain.model

data class DashboardModel(
    val departmentName: String,
    val storeName: String,
    val wasteAmount: Double,
    val wasteTrend: Double,
    val stockHealth: Int,
    val stockHealthLabel: String,
    val criticalCount: Int,
    val totalRevenue: Double,
    val riskyBatches: List<ReminderModel>
)

data class ReminderModel(
    val batchId: Long,
    val batchCode: String,
    val productName: String,
    val departmentName: String,
    val storeName: String,
    val quantity: Double,
    val daysLeft: Int,
    val urgency: String
)

data class ProductModel(
    val id: Long,
    val name: String,
    val barcode: String,
    val category: String,
    val departmentName: String,
    val storeName: String,
    val unit: String,
    val sellPrice: Double
)

data class AiAnalysisModel(
    val riskScore: Int,
    val riskLevel: String,
    val recommendations: List<String>,
    val summary: String
)