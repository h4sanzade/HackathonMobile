package com.hasanzade.hackathonmobile.ui

data class SavedProductEntry(
    val productName: String,
    val barcode: String,
    val quantity: Double,
    val batchCode: String,
    val arrivalDate: String,
    val removalDate: String,
    val expiryDate: String  = "",
    val category: String,
    val sellPrice: Double   = 0.0,
    val unit: String        = "ədəd",
    val savedAt: String     = java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
)