package com.hasanzade.hackathonmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.data.repository.BatchRepository
import com.hasanzade.hackathonmobile.domain.model.ReminderModel
import com.hasanzade.hackathonmobile.domain.usecase.GetRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatchDisplayItem(
    val productName: String,
    val barcode: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val arrivalDate: String,
    val removalDate: String,
    val expiryDate: String,
    val batchCode: String,
    val daysLeft: Int,
    val urgency: String,
    val sellPrice: Double,
    val totalValueAzn: Double,
    val isFromApi: Boolean
)

@HiltViewModel
class BatchListViewModel @Inject constructor(
    private val getRemindersUseCase: GetRemindersUseCase,
    private val tokenDataStore: TokenDataStore,
    private val batchRepository: BatchRepository
) : ViewModel() {

    private val _apiBatches = MutableStateFlow<List<ReminderModel>>(emptyList())

    private val _displayItems = MutableStateFlow<List<BatchDisplayItem>>(emptyList())
    val displayItems: StateFlow<List<BatchDisplayItem>> = _displayItems

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadApiReminders()
        observeAndMerge()
    }

    private fun loadApiReminders() {
        viewModelScope.launch {
            val filial = tokenDataStore.getFilial() ?: return@launch
            val dept   = tokenDataStore.getDepartment()

            when (val result = getRemindersUseCase(filial, dept)) {
                is NetworkResult.Success -> _apiBatches.value = result.data
                else -> Unit
            }
            _isLoading.value = false
        }
    }

    private fun observeAndMerge() {
        viewModelScope.launch {
            combine(_apiBatches, batchRepository.savedBatches) { apiList, localList ->

                // API reminder-larını BatchDisplayItem-ə çevir
                val fromApi = apiList.map { r ->
                    val daysLeft = r.daysLeft
                    BatchDisplayItem(
                        productName   = r.productName,
                        barcode       = "--",
                        category      = "--",
                        quantity      = r.quantity,
                        unit          = "ədəd",
                        arrivalDate   = r.batchCode
                            .split("-").getOrNull(3)
                            ?.let { formatDateFromCode(it) } ?: "--",
                        removalDate   = calcDateFromDays(daysLeft),
                        expiryDate    = calcDateFromDays(daysLeft),
                        batchCode     = r.batchCode,
                        daysLeft      = daysLeft,
                        urgency       = r.urgency,
                        sellPrice     = 0.0,
                        totalValueAzn = r.quantity * 2.5,
                        isFromApi     = true
                    )
                }

                // Local (yeni əlavə edilmiş) batch-ləri çevir
                val fromLocal = localList.map { s ->
                    val daysLeft = calcDaysLeft(s.removalDate)
                    val urgency  = when {
                        daysLeft <= 1 -> "CRITICAL"
                        daysLeft <= 3 -> "WARNING"
                        else          -> "OK"
                    }
                    BatchDisplayItem(
                        productName   = s.productName,
                        barcode       = s.barcode,
                        category      = s.category,
                        quantity      = s.quantity,
                        unit          = s.unit,
                        arrivalDate   = s.arrivalDate,
                        removalDate   = s.removalDate,
                        expiryDate    = s.expiryDate.ifEmpty { s.removalDate },
                        batchCode     = s.batchCode,
                        daysLeft      = daysLeft,
                        urgency       = urgency,
                        sellPrice     = s.sellPrice,
                        totalValueAzn = s.quantity * s.sellPrice,
                        isFromApi     = false
                    )
                }

                // Local-ı üstdə göstər, sonra API
                fromLocal + fromApi

            }.collect { merged ->
                _displayItems.value = merged
            }
        }
    }

    private fun calcDaysLeft(dateStr: String): Int {
        return try {
            val target = java.time.LocalDate.parse(dateStr)
            java.time.temporal.ChronoUnit.DAYS
                .between(java.time.LocalDate.now(), target).toInt()
        } catch (e: Exception) { 99 }
    }

    private fun calcDateFromDays(days: Int): String {
        val date = java.time.LocalDate.now().plusDays(days.toLong())
        return "${date.dayOfMonth.toString().padStart(2,'0')}.${
            date.monthValue.toString().padStart(2,'0')}.${date.year}"
    }

    private fun formatDateFromCode(yyyymmdd: String): String {
        return try {
            "${yyyymmdd.substring(6,8)}.${yyyymmdd.substring(4,6)}.${yyyymmdd.substring(0,4)}"
        } catch (e: Exception) { "--" }
    }
}