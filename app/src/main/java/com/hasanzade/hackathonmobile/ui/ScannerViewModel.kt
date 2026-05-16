package com.hasanzade.hackathonmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.domain.model.ProductModel
import com.hasanzade.hackathonmobile.domain.usecase.GetProductByBarcodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ScanUiState {
    object Idle                              : ScanUiState()
    object Loading                           : ScanUiState()
    data class Success(val product: ProductModel) : ScanUiState()
    data class Error(val message: String)    : ScanUiState()
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val getProductByBarcodeUseCase: GetProductByBarcodeUseCase
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState

    fun searchBarcode(barcode: String) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Loading
            _scanState.value = when (val result = getProductByBarcodeUseCase(barcode)) {
                is NetworkResult.Success -> ScanUiState.Success(result.data)
                is NetworkResult.Error   -> ScanUiState.Error(result.message)
                else                     -> ScanUiState.Error("Xəta baş verdi")
            }
        }
    }

    fun resetState() {
        _scanState.value = ScanUiState.Idle
    }
}