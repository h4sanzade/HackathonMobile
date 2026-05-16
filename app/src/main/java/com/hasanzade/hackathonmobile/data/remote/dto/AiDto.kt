package com.hasanzade.hackathonmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AiAnalysisDto(
    @SerializedName("riskScore")       val riskScore: Int?,
    @SerializedName("riskLevel")       val riskLevel: String?,
    @SerializedName("predictions")     val predictions: List<AiPredictionDto>?,
    @SerializedName("recommendations") val recommendations: List<String>?,
    @SerializedName("summary")         val summary: String?
)

data class AiPredictionDto(
    @SerializedName("productName")    val productName: String?,
    @SerializedName("predictedWaste") val predictedWaste: Double?,
    @SerializedName("confidence")     val confidence: Int?,
    @SerializedName("action")         val action: String?
)

data class LoginRequestDto(
    @SerializedName("userId")   val userId: String,
    @SerializedName("password") val password: String
)

data class LoginResponseDto(
    @SerializedName("accessToken")      val accessToken: String?,
    @SerializedName("expiresInSeconds") val expiresInSeconds: Long?,
    @SerializedName("role")             val role: String?,
    @SerializedName("displayName")      val displayName: String?,
    @SerializedName("filial")           val filial: String?,
    @SerializedName("department")       val department: String?,
    @SerializedName("allDepartments")   val allDepartments: Boolean?
)