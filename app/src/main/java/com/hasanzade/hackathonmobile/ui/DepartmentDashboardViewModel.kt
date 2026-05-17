package com.hasanzade.hackathonmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.domain.model.DashboardModel
import com.hasanzade.hackathonmobile.domain.usecase.GetDashboardUseCase
import com.hasanzade.hackathonmobile.domain.usecase.ResolveReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DashboardUiState {
    object Loading                               : DashboardUiState()
    data class Success(val data: DashboardModel) : DashboardUiState()
    data class Error(val message: String)        : DashboardUiState()
}

@HiltViewModel
class DepartmentDashboardViewModel @Inject constructor(
    private val getDashboardUseCase: GetDashboardUseCase,
    private val resolveReminderUseCase: ResolveReminderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState

    private val _resolveState = MutableStateFlow<NetworkResult<String>?>(null)
    val resolveState: StateFlow<NetworkResult<String>?> = _resolveState

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            _uiState.value = when (val result = getDashboardUseCase()) {
                is NetworkResult.Success -> DashboardUiState.Success(result.data)
                is NetworkResult.Error   -> DashboardUiState.Error(result.message)
                else                     -> DashboardUiState.Error("Xəta")
            }
        }
    }

    fun resolveReminder(batchId: Long) {
        viewModelScope.launch {
            _resolveState.value = NetworkResult.Loading
            _resolveState.value = resolveReminderUseCase(batchId)
            loadDashboard()
        }
    }
}