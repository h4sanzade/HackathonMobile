package com.hasanzade.hackathonmobile.data.remote.api

import com.hasanzade.hackathonmobile.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<LoginResponseDto>

    @GET("api/products/barcode/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<ProductDto>

    @GET("api/products/stock")
    suspend fun getStock(
        @Query("store") store: String,
        @Query("department") department: String
    ): Response<List<StockDto>>

    @POST("api/products")
    suspend fun createProduct(
        @Body request: CreateProductRequestDto
    ): Response<ProductDto>

    @POST("api/products/batch")
    suspend fun addBatch(
        @Body request: AddBatchRequestDto
    ): Response<String>

    @GET("api/reminders/active")
    suspend fun getActiveReminders(
        @Query("store") store: String,
        @Query("department") department: String? = null
    ): Response<List<ReminderDto>>

    @PUT("api/reminders/{batchId}/resolve")
    suspend fun resolveReminder(
        @Path("batchId") batchId: Long
    ): Response<ResolveResponseDto>

    @POST("api/reminders/fcm-token")
    suspend fun registerFcmToken(
        @Query("token") token: String
    ): Response<String>

    @POST("api/waste/log")
    suspend fun logWaste(
        @Body request: WasteLogRequestDto
    ): Response<WasteLogResponseDto>

    @GET("api/ai/analyze")
    suspend fun getAiAnalysis(): Response<AiAnalysisDto>
}