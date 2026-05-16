package com.hasanzade.hackathonmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ReminderDto(
    @SerializedName("batchId")        val batchId: Long?,
    @SerializedName("batchCode")      val batchCode: String?,
    @SerializedName("productName")    val productName: String?,
    @SerializedName("departmentName") val departmentName: String?,
    @SerializedName("storeName")      val storeName: String?,
    @SerializedName("quantity")       val quantity: Double?,
    @SerializedName("deliveryDate")   val deliveryDate: String?,
    @SerializedName("removalDate")    val removalDate: String?,
    @SerializedName("daysLeft")       val daysLeft: Int?,
    @SerializedName("urgency")        val urgency: String?,
    @SerializedName("notified2Day")   val notified2Day: Boolean?,
    @SerializedName("notified1Day")   val notified1Day: Boolean?
)

data class ResolveResponseDto(
    @SerializedName("batchCode")   val batchCode: String?,
    @SerializedName("productName") val productName: String?,
    @SerializedName("message")     val message: String?,
    @SerializedName("resolvedAt")  val resolvedAt: String?
)