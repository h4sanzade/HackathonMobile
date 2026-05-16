package com.hasanzade.hackathonmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("id")             val id: Long?,
    @SerializedName("name")           val name: String?,
    @SerializedName("barcode")        val barcode: String?,
    @SerializedName("category")       val category: String?,
    @SerializedName("departmentId")   val departmentId: Long?,
    @SerializedName("departmentName") val departmentName: String?,
    @SerializedName("storeName")      val storeName: String?,
    @SerializedName("unit")           val unit: String?,
    @SerializedName("costPrice")      val costPrice: Double?,
    @SerializedName("sellPrice")      val sellPrice: Double?,
    @SerializedName("active")         val active: Boolean?
)

data class StockDto(
    @SerializedName("productId")          val productId: Long?,
    @SerializedName("productName")        val productName: String?,
    @SerializedName("barcode")            val barcode: String?,
    @SerializedName("category")           val category: String?,
    @SerializedName("departmentName")     val departmentName: String?,
    @SerializedName("totalStock")         val totalStock: Double?,
    @SerializedName("batchCount")         val batchCount: Int?,
    @SerializedName("nearestRemovalDate") val nearestRemovalDate: String?
)

data class CreateProductRequestDto(
    @SerializedName("name")         val name: String,
    @SerializedName("barcode")      val barcode: String?,
    @SerializedName("category")     val category: String,
    @SerializedName("departmentId") val departmentId: Long,
    @SerializedName("unit")         val unit: String?,
    @SerializedName("costPrice")    val costPrice: Double?,
    @SerializedName("sellPrice")    val sellPrice: Double?
)

data class AddBatchRequestDto(
    @SerializedName("productId")      val productId: Long,
    @SerializedName("quantity")       val quantity: Double,
    @SerializedName("deliveryDate")   val deliveryDate: String,
    @SerializedName("removalDate")    val removalDate: String,
    @SerializedName("addedByUserId")  val addedByUserId: String?
)