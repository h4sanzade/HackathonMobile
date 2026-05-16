package com.hasanzade.hackathonmobile.domain.repository

import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.domain.model.ProductModel

interface ProductRepository {
    suspend fun getProductByBarcode(barcode: String): NetworkResult<ProductModel>
}