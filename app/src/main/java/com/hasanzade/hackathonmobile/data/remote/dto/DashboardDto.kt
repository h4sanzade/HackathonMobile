package com.hasanzade.hackathonmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DashboardDto(
    @SerializedName("departmentName")   val departmentName: String?,
    @SerializedName("storeName")        val storeName: String?,
    @SerializedName("wasteAmount")      val wasteAmount: Double?,
    @SerializedName("wasteTrend")       val wasteTrend: Double?,
    @SerializedName("stockHealth")      val stockHealth: Int?,
    @SerializedName("stockHealthLabel") val stockHealthLabel: String?,
    @SerializedName("criticalCount")    val criticalCount: Int?,
    @SerializedName("totalRevenue")     val totalRevenue: Double?,
    @SerializedName("riskyBatches")     val riskyBatches: List<ReminderDto>?
)