package com.hasanzade.hackathonmobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import com.hasanzade.hackathonmobile.ui.model.RiskyBatchUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class DeptDashboardState(
    val isLoading: Boolean                    = true,
    val sectorName: String                    = "--",
    val displayName: String                   = "--",
    val wasteAmount: String                   = "--",
    val wasteTrend: String                    = "↑ --%",
    val stockHealthPercent: Int               = 0,
    val stockHealthLabel: String              = "--",
    val attentionCount: String                = "--",
    val riskyBatches: List<RiskyBatchUiModel> = RiskyBatchUiModel.placeholder(),
    val error: String?                        = null
)

class DepartmentDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenDataStore = TokenDataStore(application)

    private val _state = MutableLiveData(DeptDashboardState())
    val state: LiveData<DeptDashboardState> = _state

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val displayName = tokenDataStore.getDisplayName() ?: "--"
            val department  = tokenDataStore.getDepartment() ?: "--"
            val filial      = tokenDataStore.getFilial() ?: "--"

            _state.value = _state.value?.copy(
                sectorName  = department,
                displayName = displayName,
                isLoading   = false
            )
        }
    }

    // Backend hazır olanda bu metodu çağır
    fun loadDashboard(store: String, department: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: API call burada olacaq
            // val reminders = reminderRepository.getActiveReminders(store, department)
            // val batches = reminders.map { RiskyBatchUiModel.fromReminder(...) }
            // _state.postValue(_state.value?.copy(riskyBatches = batches))
        }
    }

    // Mock data — demo üçün
    fun loadMockData() {
        val mockBatches = listOf(
            RiskyBatchUiModel.fromReminder(
                productName      = "Organic Green Juices (500ml)",
                batchCode        = "FG-1-0516-DR8821",
                daysLeft         = 0,
                urgency          = "CRITICAL",
                quantity         = 5.0,
                originalQuantity = 50.0
            ),
            RiskyBatchUiModel.fromReminder(
                productName      = "Sparkling Water (Premium 1L)",
                batchCode        = "FG-2-0516-DR9045",
                daysLeft         = 4,
                urgency          = "WARNING",
                quantity         = 25.0,
                originalQuantity = 40.0
            ),
            RiskyBatchUiModel.fromReminder(
                productName      = "Craft Kola (Zero Sugar)",
                batchCode        = "FG-3-0516-DR7762",
                daysLeft         = 1,
                urgency          = "QUALITY",
                quantity         = 8.0,
                originalQuantity = 60.0
            )
        )

        _state.value = _state.value?.copy(
            wasteAmount        = "3,200",
            wasteTrend         = "↑ 4%",
            stockHealthPercent = 92,
            stockHealthLabel   = "Good",
            attentionCount     = "Needs Attention",
            riskyBatches       = mockBatches,
            isLoading          = false
        )
    }
}