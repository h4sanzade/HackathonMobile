package com.hasanzade.hackathonmobile.data.repository

import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.data.remote.api.ApiService
import com.hasanzade.hackathonmobile.data.remote.safeApiCall
import com.hasanzade.hackathonmobile.data.remote.dto.StockDto
import com.hasanzade.hackathonmobile.domain.model.DashboardModel
import com.hasanzade.hackathonmobile.domain.model.ReminderModel
import com.hasanzade.hackathonmobile.domain.model.WasteLogModel
import com.hasanzade.hackathonmobile.domain.repository.DashboardRepository
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore
) : DashboardRepository {

    override suspend fun getDashboard(): NetworkResult<DashboardModel> {
        return try {
            val displayName = tokenDataStore.getDisplayName() ?: "--"
            val department  = tokenDataStore.getDepartment() ?: ""
            val filial      = tokenDataStore.getFilial() ?: ""

            android.util.Log.d("DASHBOARD",
                "displayName=$displayName dept=$department filial=$filial")

            val reminders = fetchReminders(filial, department)
            val stocks    = fetchStock(filial, department)

            val stockHealth      = calculateStockHealth(stocks)
            val stockHealthLabel = when {
                stockHealth >= 80 -> "Good"
                stockHealth >= 50 -> "Fair"
                else              -> "Poor"
            }

            val totalWasteAmount = reminders
                .filter { it.daysLeft <= 0 }
                .sumOf { it.quantity }

            val totalStockCount = stocks.sumOf { it.totalStock ?: 0.0 }
            val wastePercent    = if (totalStockCount > 0)
                (totalWasteAmount / totalStockCount * 100)
            else 0.0

            NetworkResult.Success(
                DashboardModel(
                    departmentName   = department.ifEmpty { "--" },
                    storeName        = filial.ifEmpty { "--" },
                    displayName      = displayName,
                    wasteAmount      = totalWasteAmount,
                    wasteTrend       = wastePercent,
                    stockHealth      = stockHealth,
                    stockHealthLabel = stockHealthLabel,
                    criticalCount    = reminders.count { it.daysLeft <= 1 },
                    totalRevenue     = 0.0,
                    riskyBatches     = reminders,
                    wasteLogs        = emptyList()
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("DASHBOARD", "getDashboard error", e)
            NetworkResult.Error(e.localizedMessage ?: "Xəta baş verdi")
        }
    }

    private suspend fun fetchReminders(
        filial: String,
        department: String
    ): List<ReminderModel> {
        if (filial.isEmpty()) return emptyList()
        return try {
            when (val result = safeApiCall {
                api.getActiveReminders(
                    store      = filial,
                    department = department.ifEmpty { null }
                )
            }) {
                is NetworkResult.Success -> result.data.mapNotNull { r ->
                    try {
                        ReminderModel(
                            batchId        = r.batchId ?: return@mapNotNull null,
                            batchCode      = r.batchCode ?: "--",
                            productName    = r.productName ?: "--",
                            departmentName = r.departmentName ?: department,
                            storeName      = r.storeName ?: filial,
                            quantity       = r.quantity ?: 0.0,
                            daysLeft       = r.daysLeft ?: 0,
                            urgency        = r.urgency ?: "WARNING"
                        )
                    } catch (e: Exception) { null }
                }
                is NetworkResult.Error -> {
                    android.util.Log.e("DASHBOARD",
                        "Reminders error: ${result.message}")
                    emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("DASHBOARD", "fetchReminders exception", e)
            emptyList()
        }
    }

    private suspend fun fetchStock(
        filial: String,
        department: String
    ): List<StockDto> {
        if (filial.isEmpty() || department.isEmpty()) return emptyList()
        return try {
            when (val result = safeApiCall {
                api.getStock(filial, department)
            }) {
                is NetworkResult.Success -> result.data
                is NetworkResult.Error -> {
                    android.util.Log.e("DASHBOARD",
                        "Stock error: ${result.message}")
                    emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("DASHBOARD", "fetchStock exception", e)
            emptyList()
        }
    }

    private fun calculateStockHealth(stocks: List<StockDto>): Int {
        if (stocks.isEmpty()) return 0
        return try {
            val healthy = stocks.count { s ->
                val days = s.nearestRemovalDate?.let { dateStr ->
                    try {
                        val date = java.time.LocalDate.parse(dateStr)
                        java.time.temporal.ChronoUnit.DAYS.between(
                            java.time.LocalDate.now(), date
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("DASHBOARD",
                            "Date parse error: $dateStr", e)
                        99L
                    }
                } ?: 99L
                days > 2
            }
            ((healthy.toDouble() / stocks.size) * 100)
                .toInt()
                .coerceIn(0, 100)
        } catch (e: Exception) {
            android.util.Log.e("DASHBOARD", "calculateStockHealth error", e)
            0
        }
    }
}