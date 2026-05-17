package com.hasanzade.hackathonmobile.data.repository

import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.data.remote.api.ApiService
import com.hasanzade.hackathonmobile.data.remote.dto.StockDto
import com.hasanzade.hackathonmobile.data.remote.safeApiCall
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

            if (filial.isEmpty()) {
                return NetworkResult.Success(
                    emptyDashboard(displayName, department, filial)
                )
            }

            // Hamısını paralel çək
            val reminders = fetchReminders(filial, department)
            val stocks    = fetchStock(filial, department)
            val wasteLogs = fetchWasteLogs(filial, department)

            android.util.Log.d("DASHBOARD",
                "reminders=${reminders.size} " +
                        "stocks=${stocks.size} " +
                        "wasteLogs=${wasteLogs.size}")

            // Stock Health — removal date > 2 gün olanlar sağlamdır
            val stockHealth = calcStockHealth(stocks)
            val stockHealthLabel = when {
                stockHealth >= 80 -> "Yaxşı"
                stockHealth >= 50 -> "Orta"
                else              -> "Zəif"
            }

            // Waste amount — real waste loglardan
            val totalWasteAmount = wasteLogs.sumOf { it.totalLoss }

            // Waste trend — risk altındakı stock faizi
            val totalStock  = stocks.sumOf { it.totalStock ?: 0.0 }
            val riskyQty    = reminders.filter { it.daysLeft <= 3 }.sumOf { it.quantity }
            val wasteTrend  = if (totalStock > 0)
                (riskyQty / totalStock * 100) else 0.0

            NetworkResult.Success(
                DashboardModel(
                    departmentName   = department.ifEmpty { "--" },
                    storeName        = filial.ifEmpty { "--" },
                    displayName      = displayName,
                    wasteAmount      = totalWasteAmount,
                    wasteTrend       = wasteTrend,
                    stockHealth      = stockHealth,
                    stockHealthLabel = stockHealthLabel,
                    criticalCount    = reminders.count { it.daysLeft <= 1 },
                    totalRevenue     = totalStock * 2.5,
                    riskyBatches     = reminders,
                    wasteLogs        = wasteLogs
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("DASHBOARD", "getDashboard error", e)
            NetworkResult.Error(e.localizedMessage ?: "Xəta baş verdi")
        }
    }

    private fun emptyDashboard(
        displayName: String,
        department: String,
        filial: String
    ) = DashboardModel(
        departmentName   = department.ifEmpty { "--" },
        storeName        = filial.ifEmpty { "--" },
        displayName      = displayName,
        wasteAmount      = 0.0,
        wasteTrend       = 0.0,
        stockHealth      = 0,
        stockHealthLabel = "Məlumat yoxdur",
        criticalCount    = 0,
        totalRevenue     = 0.0,
        riskyBatches     = emptyList(),
        wasteLogs        = emptyList()
    )

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
                        "Reminders err: ${result.message}")
                    emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
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
                        "Stock err: ${result.message}")
                    emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun fetchWasteLogs(
        filial: String,
        department: String
    ): List<WasteLogModel> {
        if (filial.isEmpty()) return emptyList()
        return try {
            when (val result = safeApiCall {
                api.getWasteLogs(
                    store      = filial,
                    department = department.ifEmpty { null }
                )
            }) {
                is NetworkResult.Success -> result.data.map { w ->
                    WasteLogModel(
                        productName    = w.productName ?: "--",
                        quantity       = w.quantity ?: 0.0,
                        totalLoss      = w.totalLoss ?: 0.0,
                        reason         = w.reason ?: "--",
                        departmentName = w.departmentName ?: department,
                        wasteDate      = w.wasteDate ?: "--"
                    )
                }
                is NetworkResult.Error -> {
                    android.util.Log.e("DASHBOARD",
                        "WasteLogs err: ${result.message}")
                    emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun calcStockHealth(stocks: List<StockDto>): Int {
        if (stocks.isEmpty()) return 0
        return try {
            val healthy = stocks.count { s ->
                val days = s.nearestRemovalDate?.let { dateStr ->
                    try {
                        val date = java.time.LocalDate.parse(dateStr)
                        java.time.temporal.ChronoUnit.DAYS.between(
                            java.time.LocalDate.now(), date
                        )
                    } catch (e: Exception) { 99L }
                } ?: 99L
                days > 2
            }
            ((healthy.toDouble() / stocks.size) * 100)
                .toInt().coerceIn(0, 100)
        } catch (e: Exception) { 0 }
    }
}