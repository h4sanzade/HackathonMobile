package com.hasanzade.hackathonmobile.data.repository

import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.data.remote.api.ApiService
import com.hasanzade.hackathonmobile.data.remote.safeApiCall
import com.hasanzade.hackathonmobile.domain.model.DashboardModel
import com.hasanzade.hackathonmobile.domain.model.ReminderModel
import com.hasanzade.hackathonmobile.domain.repository.DashboardRepository
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val api: ApiService
) : DashboardRepository {

    override suspend fun getDashboard(): NetworkResult<DashboardModel> {
        return when (val result = safeApiCall { api.getDashboard() }) {
            is NetworkResult.Success -> {
                val dto = result.data
                NetworkResult.Success(
                    DashboardModel(
                        departmentName = dto.departmentName ?: "--",
                        storeName = dto.storeName ?: "--",
                        wasteAmount = dto.wasteAmount ?: 0.0,
                        wasteTrend = dto.wasteTrend ?: 0.0,
                        stockHealth = dto.stockHealth ?: 0,
                        stockHealthLabel = dto.stockHealthLabel ?: "--",
                        criticalCount = dto.criticalCount ?: 0,
                        totalRevenue = dto.totalRevenue ?: 0.0,
                        riskyBatches = dto.riskyBatches?.map { r ->
                            ReminderModel(
                                batchId = r.batchId ?: 0L,
                                batchCode = r.batchCode ?: "--",
                                productName = r.productName ?: "--",
                                departmentName = r.departmentName ?: "--",
                                storeName = r.storeName ?: "--",
                                quantity = r.quantity ?: 0.0,
                                daysLeft = r.daysLeft ?: 0,
                                urgency = r.urgency ?: "WARNING"
                            )
                        } ?: emptyList()
                    )
                )
            }
            is NetworkResult.Error   -> result
            is NetworkResult.Loading -> result
        }
    }
}