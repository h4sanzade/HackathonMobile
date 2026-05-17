package com.hasanzade.hackathonmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.data.remote.api.ApiService
import com.hasanzade.hackathonmobile.data.remote.dto.AddBatchRequestDto
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

    // Bütün stock listini saxla — batchId tapmaq üçün
    private var fullStockList: List<StockDto> = emptyList()

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
                    fullStockList       = result.data
                    _stockList.value    = result.data
                    _filteredList.value = result.data
                }
                else -> Unit
            }
        }
    }

    fun searchProduct(query: String) {
        if (query.isBlank()) {
            _filteredList.value = fullStockList
            return
        }
        val localResults = fullStockList.filter { stock ->
            stock.productName?.contains(query, ignoreCase = true) == true ||
                    stock.barcode?.contains(query, ignoreCase = true) == true
        }
        if (localResults.isNotEmpty()) {
            _filteredList.value = localResults
        } else {
            if (query.length >= 5) fetchProductByBarcode(query)
        }
    }

    // Barkodla axtarış — stock-da yoxdursa API-yə get
    fun fetchProductByBarcode(barcode: String) {
        viewModelScope.launch {
            _barcodeLoading.value = true

            // Əvvəlcə stock listindən batchId tap
            val stockItem = fullStockList.find {
                it.barcode?.equals(barcode, ignoreCase = true) == true
            }

            when (val result = safeApiCall {
                api.getProductByBarcode(barcode.trim())
            }) {
                is NetworkResult.Success -> {
                    val dto = result.data

                    // Stock-dan batchId tap — reminders endpoint-dən gəlir
                    val batchId = fetchBatchIdForProduct(
                        productId = dto.id ?: 0L,
                        barcode   = barcode
                    )

                    val info = SelectedProductInfo(
                        productId   = dto.id ?: 0L,
                        productName = dto.name ?: "--",
                        barcode     = dto.barcode ?: barcode,
                        totalStock  = stockItem?.totalStock ?: 0.0,
                        sellPrice   = dto.sellPrice ?: 0.0,
                        unit        = dto.unit ?: "ədəd",
                        batchId     = batchId
                    )

                    android.util.Log.d("LOG_WASTE",
                        "Product: ${info.productName} batchId=${info.batchId}")

                    _selectedProduct.value = info
                    _filteredList.value    = emptyList()
                    calculateLoss(1.0, info.sellPrice)
                }
                is NetworkResult.Error -> {
                    _filteredList.value = emptyList()
                    android.util.Log.e("LOG_WASTE",
                        "Barcode not found: $barcode")
                }
                else -> Unit
            }
            _barcodeLoading.value = false
        }
    }

    // Məhsulun aktiv batch-ını tap
    private suspend fun fetchBatchIdForProduct(
        productId: Long,
        barcode: String
    ): Long? {
        return try {
            // Reminders-dən batchId tap
            val store = currentFilial
            val dept  = currentDepartment

            if (store.isEmpty()) return null

            when (val result = safeApiCall {
                api.getActiveReminders(
                    store      = store,
                    department = dept.ifEmpty { null }
                )
            }) {
                is NetworkResult.Success -> {
                    val reminder = result.data.find { r ->
                        // Barcode ilə uyğun reminder tap
                        r.batchCode?.contains(
                            productId.toString(), ignoreCase = true
                        ) == true
                    }
                    val batchId = reminder?.batchId
                    android.util.Log.d("LOG_WASTE",
                        "Found batchId=$batchId for productId=$productId")
                    batchId
                }
                else -> null
            }
        } catch (e: Exception) {
            android.util.Log.e("LOG_WASTE", "fetchBatchId error", e)
            null
        }
    }

    fun selectProductFromStock(product: StockDto) {
        viewModelScope.launch {
            val barcode = product.barcode ?: ""
            _barcodeLoading.value = true

            // Barcode endpoint-dən əlavə məlumat al
            val productDto = if (barcode.isNotEmpty()) {
                when (val result = safeApiCall {
                    api.getProductByBarcode(barcode)
                }) {
                    is NetworkResult.Success -> result.data
                    else -> null
                }
            } else null

            // Bu məhsulun batchId-sini tap
            val batchId = fetchBatchIdForProduct(
                productId = product.productId ?: 0L,
                barcode   = barcode
            )

            val info = SelectedProductInfo(
                productId   = productDto?.id ?: product.productId ?: 0L,
                productName = productDto?.name ?: product.productName ?: "--",
                barcode     = productDto?.barcode ?: barcode,
                totalStock  = product.totalStock ?: 0.0,
                sellPrice   = productDto?.sellPrice ?: 0.0,
                unit        = productDto?.unit ?: "ədəd",
                batchId     = batchId
            )

            android.util.Log.d("LOG_WASTE",
                "Selected: ${info.productName} batchId=${info.batchId}")

            _selectedProduct.value = info
            _filteredList.value    = emptyList()
            _barcodeLoading.value  = false
            calculateLoss(1.0, info.sellPrice)
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

        // batchId yoxdursa yenidən cəhd et
        if (product.batchId == null) {
            viewModelScope.launch {
                _logState.value = LogWasteUiState.Loading
                val batchId = fetchBatchIdForProduct(
                    productId = product.productId,
                    barcode   = product.barcode
                )

                if (batchId == null) {
                    // BatchId tapılmadı — stok yoxdur və ya aktiv batch yoxdur
                    _logState.value = LogWasteUiState.Error(
                        "Bu məhsul üçün aktiv batch tapılmadı. " +
                                "Zəhmət olmasa əvvəlcə batch əlavə edin."
                    )
                    return@launch
                }

                // batchId tapıldı — yenilə və davam et
                _selectedProduct.value = product.copy(batchId = batchId)
                sendWasteLog(product.copy(batchId = batchId), quantity, reason)
            }
            return
        }

        viewModelScope.launch {
            sendWasteLog(product, quantity, reason)
        }
    }

    private suspend fun sendWasteLog(
        product: SelectedProductInfo,
        quantity: Double,
        reason: String
    ) {
        _logState.value = LogWasteUiState.Loading

        android.util.Log.d("WASTE_LOG",
            "Sending: productId=${product.productId} " +
                    "batchId=${product.batchId} qty=$quantity reason=$reason")

        _logState.value = when (val result = safeApiCall {
            api.logWaste(
                WasteLogRequestDto(
                    productId = product.productId,
                    quantity  = quantity,
                    reason    = reason,
                    batchId   = product.batchId
                )
            )
        }) {
            is NetworkResult.Success -> {
                android.util.Log.d("WASTE_LOG", "Success!")
                LogWasteUiState.Success
            }
            is NetworkResult.Error -> {
                android.util.Log.e("WASTE_LOG",
                    "Error: ${result.message} code=${result.code}")
                LogWasteUiState.Error(result.message)
            }
            else -> LogWasteUiState.Error("Xəta")
        }
    }
}