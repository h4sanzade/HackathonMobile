package com.hasanzade.hackathonmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.data.remote.api.ApiService
import com.hasanzade.hackathonmobile.data.remote.dto.StockDto
import com.hasanzade.hackathonmobile.data.remote.dto.WasteLogRequestDto
import com.hasanzade.hackathonmobile.data.remote.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LogWasteUiState {
    object Idle                       : LogWasteUiState()
    object Loading                    : LogWasteUiState()
    object Success                    : LogWasteUiState()
    data class Error(val msg: String) : LogWasteUiState()
}

@HiltViewModel
class LogWasteViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _stockList    = MutableStateFlow<List<StockDto>>(emptyList())
    val stockList: StateFlow<List<StockDto>> = _stockList

    private val _filteredList = MutableStateFlow<List<StockDto>>(emptyList())
    val filteredList: StateFlow<List<StockDto>> = _filteredList

    private val _selectedProduct = MutableStateFlow<SelectedProductInfo?>(null)
    val selectedProduct: StateFlow<SelectedProductInfo?> = _selectedProduct

    private val _estimatedLoss = MutableStateFlow(0.0)
    val estimatedLoss: StateFlow<Double> = _estimatedLoss

    private val _logState = MutableStateFlow<LogWasteUiState>(LogWasteUiState.Idle)
    val logState: StateFlow<LogWasteUiState> = _logState

    private val _sectorName = MutableStateFlow("--")
    val sectorName: StateFlow<String> = _sectorName

    private val _barcodeLoading = MutableStateFlow(false)
    val barcodeLoading: StateFlow<Boolean> = _barcodeLoading

    private var currentFilial     = ""
    private var currentDepartment = ""

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val dept   = tokenDataStore.getDepartment() ?: ""
            val filial = tokenDataStore.getFilial() ?: ""
            currentDepartment = dept
            currentFilial     = filial
            _sectorName.value = dept.ifEmpty { "--" }
            if (filial.isNotEmpty() && dept.isNotEmpty()) {
                loadStock(filial, dept)
            }
        }
    }

    private fun loadStock(store: String, department: String) {
        viewModelScope.launch {
            when (val result = safeApiCall { api.getStock(store, department) }) {
                is NetworkResult.Success -> {
                    _stockList.value    = result.data
                    _filteredList.value = result.data
                }
                else -> Unit
            }
        }
    }

    fun searchProduct(query: String) {
        if (query.isBlank()) {
            _filteredList.value = _stockList.value
            return
        }
        val localResults = _stockList.value.filter { stock ->
            stock.productName?.contains(query, ignoreCase = true) == true ||
                    stock.barcode?.contains(query, ignoreCase = true) == true
        }
        if (localResults.isNotEmpty()) {
            _filteredList.value = localResults
        } else {
            if (query.length >= 5) fetchProductByBarcode(query)
        }
    }

    fun fetchProductByBarcode(barcode: String) {
        viewModelScope.launch {
            _barcodeLoading.value = true
            when (val result = safeApiCall {
                api.getProductByBarcode(barcode.trim())
            }) {
                is NetworkResult.Success -> {
                    val dto  = result.data
                    val info = SelectedProductInfo(
                        productId   = dto.id ?: 0L,
                        productName = dto.name ?: "--",
                        barcode     = dto.barcode ?: barcode,
                        totalStock  = 0.0,
                        sellPrice   = dto.sellPrice ?: 0.0,
                        unit        = dto.unit ?: "ədəd",
                        batchId     = null
                    )
                    _selectedProduct.value = info
                    _filteredList.value    = emptyList()
                    calculateLoss(1.0, info.sellPrice)
                }
                is NetworkResult.Error -> {
                    _filteredList.value = emptyList()
                }
                else -> Unit
            }
            _barcodeLoading.value = false
        }
    }

    fun selectProductFromStock(product: StockDto) {
        viewModelScope.launch {
            val barcode = product.barcode ?: ""
            _barcodeLoading.value = true
            if (barcode.isNotEmpty()) {
                when (val result = safeApiCall {
                    api.getProductByBarcode(barcode)
                }) {
                    is NetworkResult.Success -> {
                        val dto  = result.data
                        val info = SelectedProductInfo(
                            productId   = dto.id ?: product.productId ?: 0L,
                            productName = dto.name ?: product.productName ?: "--",
                            barcode     = dto.barcode ?: barcode,
                            totalStock  = product.totalStock ?: 0.0,
                            sellPrice   = dto.sellPrice ?: 0.0,
                            unit        = dto.unit ?: "ədəd",
                            batchId     = null
                        )
                        _selectedProduct.value = info
                        calculateLoss(1.0, info.sellPrice)
                    }
                    else -> {
                        val info = SelectedProductInfo(
                            productId   = product.productId ?: 0L,
                            productName = product.productName ?: "--",
                            barcode     = barcode,
                            totalStock  = product.totalStock ?: 0.0,
                            sellPrice   = 0.0,
                            unit        = "ədəd",
                            batchId     = null
                        )
                        _selectedProduct.value = info
                        calculateLoss(1.0, 0.0)
                    }
                }
            } else {
                val info = SelectedProductInfo(
                    productId   = product.productId ?: 0L,
                    productName = product.productName ?: "--",
                    barcode     = "--",
                    totalStock  = product.totalStock ?: 0.0,
                    sellPrice   = 0.0,
                    unit        = "ədəd",
                    batchId     = null
                )
                _selectedProduct.value = info
            }
            _filteredList.value   = emptyList()
            _barcodeLoading.value = false
        }
    }

    fun calculateLoss(quantity: Double, unitPrice: Double? = null) {
        val price = unitPrice ?: _selectedProduct.value?.sellPrice ?: 0.0
        _estimatedLoss.value = quantity * price
    }

    fun logWaste(quantity: Double, reason: String, notes: String) {
        val product = _selectedProduct.value ?: run {
            _logState.value = LogWasteUiState.Error("Məhsul seçilməyib")
            return
        }

        viewModelScope.launch {
            _logState.value = LogWasteUiState.Loading
            android.util.Log.d("WASTE_LOG",
                "productId=${product.productId} batchId=${product.batchId} " +
                        "qty=$quantity reason=$reason")

            _logState.value = when (val result = safeApiCall {
                api.logWaste(
                    WasteLogRequestDto(
                        productId = product.productId,
                        batchId   = product.batchId,
                        quantity  = quantity,
                        reason    = reason
                    )
                )
            }) {
                is NetworkResult.Success -> LogWasteUiState.Success
                is NetworkResult.Error   -> {
                    android.util.Log.e("WASTE_LOG", "Error: ${result.message}")
                    LogWasteUiState.Error(result.message)
                }
                else -> LogWasteUiState.Error("Xəta")
            }
        }
    }
}