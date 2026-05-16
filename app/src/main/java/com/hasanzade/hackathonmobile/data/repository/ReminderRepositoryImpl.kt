package com.hasanzade.hackathonmobile.data.repository

import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.data.remote.api.ApiService
import com.hasanzade.hackathonmobile.data.remote.safeApiCall
import com.hasanzade.hackathonmobile.domain.model.ReminderModel
import com.hasanzade.hackathonmobile.domain.repository.ReminderRepository
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val api: ApiService
) : ReminderRepository {

    override suspend fun getActiveReminders(
        store: String, department: String?
    ): NetworkResult<List<ReminderModel>> {
        return when (val result = safeApiCall {
            api.getActiveReminders(store, department)
        }) {
            is NetworkResult.Success -> NetworkResult.Success(
                result.data.map { r ->
                    ReminderModel(
                        batchId        = r.batchId ?: 0L,
                        batchCode      = r.batchCode ?: "--",
                        productName    = r.productName ?: "--",
                        departmentName = r.departmentName ?: "--",
                        storeName      = r.storeName ?: "--",
                        quantity       = r.quantity ?: 0.0,
                        daysLeft       = r.daysLeft ?: 0,
                        urgency        = r.urgency ?: "WARNING"
                    )
                }
            )
            is NetworkResult.Error   -> result
            is NetworkResult.Loading -> result
        }
    }

    override suspend fun resolveReminder(batchId: Long): NetworkResult<String> {
        return when (val result = safeApiCall { api.resolveReminder(batchId) }) {
            is NetworkResult.Success -> NetworkResult.Success(
                result.data.message ?: "Həll edildi"
            )
            is NetworkResult.Error   -> result
            is NetworkResult.Loading -> result
        }
    }
}