package com.hasanzade.hackathonmobile.ui

data class SelectedProductInfo(
    val productId: Long,
    val productName: String,
    val barcode: String,
    val totalStock: Double,
    val sellPrice: Double,
    val unit: String,
    val batchId: Long? = null
)