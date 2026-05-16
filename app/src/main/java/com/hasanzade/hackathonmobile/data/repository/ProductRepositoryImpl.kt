package com.hasanzade.hackathonmobile.data.repository

import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.data.remote.api.ApiService
import com.hasanzade.hackathonmobile.data.remote.safeApiCall
import com.hasanzade.hackathonmobile.domain.model.ProductModel
import com.hasanzade.hackathonmobile.domain.repository.ProductRepository
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val api: ApiService
) : ProductRepository {

    override suspend fun getProductByBarcode(barcode: String): NetworkResult<ProductModel> {
        return when (val result = safeApiCall { api.getProductByBarcode(barcode) }) {
            is NetworkResult.Success -> {
                val dto = result.data
                NetworkResult.Success(
                    ProductModel(
                        id             = dto.id ?: 0L,
                        name           = dto.name ?: "--",
                        barcode        = dto.barcode ?: barcode,
                        category       = dto.category ?: "--",
                        departmentName = dto.departmentName ?: "--",
                        storeName      = dto.storeName ?: "--",
                        unit           = dto.unit ?: "--",
                        sellPrice      = dto.sellPrice ?: 0.0
                    )
                )
            }
            is NetworkResult.Error   -> result
            is NetworkResult.Loading -> result
        }
    }
}