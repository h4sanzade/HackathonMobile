package com.hasanzade.hackathonmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WasteEstimateRequestDto(
    @SerializedName("productId") val productId: Long,
    @SerializedName("quantity")  val quantity: Double
)

data class WasteEstimateDto(
    @SerializedName("productName")    val productName: String?,
    @SerializedName("quantity")       val quantity: Double?,
    @SerializedName("estimatedLoss")  val estimatedLoss: Double?,
    @SerializedName("recommendation") val recommendation: String?
)

data class WasteLogRequestDto(
    @SerializedName("productId") val productId: Long,
    @SerializedName("quantity")  val quantity: Double,
    @SerializedName("reason")    val reason: String,
    @SerializedName("batchId")   val batchId: Long?
)

data class WasteLogResponseDto(
    @SerializedName("id")             val id: Long?,
    @SerializedName("productName")    val productName: String?,
    @SerializedName("quantity")       val quantity: Double?,
    @SerializedName("totalLoss")      val totalLoss: Double?,
    @SerializedName("reason")         val reason: String?,
    @SerializedName("departmentName") val departmentName: String?,
    @SerializedName("wasteDate")      val wasteDate: String?
)