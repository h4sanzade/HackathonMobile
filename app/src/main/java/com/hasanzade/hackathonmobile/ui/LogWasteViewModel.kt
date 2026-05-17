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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LogWasteUiState {
    object Idle                       : LogWasteUiState()
    object Loading                    : LogWasteUiState()
    object Success                    : LogWasteUiState()
    data class Error(val msg: String) : LogWasteUiState()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class LogWasteViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

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

    private val _searchQuery = MutableStateFlow("")

    private var fullStockList: List<StockDto> = emptyList()

    // Bütün reminder-ları cache-lə — batchId axtarmaq üçün
    private var cachedReminders: List<com.hasanzade.hackathonmobile.data.remote.dto.ReminderDto> =
        emptyList()

    private var currentFilial     = ""
    private var currentDepartment = ""

    init {
        loadSession()
        observeSearch()
    }

    private fun loadSession() {
        viewModelScope.launch {
            currentDepartment = tokenDataStore.getDepartment() ?: ""
            currentFilial     = tokenDataStore.getFilial() ?: ""
            _sectorName.value = currentDepartment.ifEmpty { "--" }
            if (currentFilial.isNotEmpty()) {
                loadStockAndReminders()
            }
        }
    }

    // Stock və reminder-ları eyni vaxtda yüklə
    private fun loadStockAndReminders() {
        viewModelScope.launch {
            _barcodeLoading.value = true

            // Stock yüklə
            if (currentDepartment.isNotEmpty()) {
                when (val result = safeApiCall {
                    api.getStock(currentFilial, currentDepartment)
                }) {
                    is NetworkResult.Success -> {
                        fullStockList       = result.data
                        _filteredList.value = result.data
                        android.util.Log.d("LOG_WASTE",
                            "Stock loaded: ${result.data.size} items")
                        result.data.forEach {
                            android.util.Log.d("LOG_WASTE",
                                "  stock: id=${it.productId} " +
                                        "name=${it.productName} " +
                                        "barcode=${it.barcode} " +
                                        "qty=${it.totalStock}")
                        }
                    }
                    is NetworkResult.Error ->
                        android.util.Log.e("LOG_WASTE", "Stock err: ${result.message}")
                    else -> Unit
                }
            }

            // Reminder-ları yüklə və cache-lə
            when (val result = safeApiCall {
                api.getActiveReminders(
                    store      = currentFilial,
                    department = currentDepartment.ifEmpty { null }
                )
            }) {
                is NetworkResult.Success -> {
                    cachedReminders = result.data
                    android.util.Log.d("LOG_WASTE",
                        "Reminders loaded: ${result.data.size} items")
                    result.data.forEach {
                        android.util.Log.d("LOG_WASTE",
                            "  reminder: batchId=${it.batchId} " +
                                    "batchCode=${it.batchCode} " +
                                    "product=${it.productName} " +
                                    "qty=${it.quantity} " +
                                    "daysLeft=${it.daysLeft}")
                    }
                }
                is NetworkResult.Error ->
                    android.util.Log.e("LOG_WASTE",
                        "Reminders err: ${result.message}")
                else -> Unit
            }

            _barcodeLoading.value = false
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(250)
                .distinctUntilChanged()
                .collect { query -> performSearch(query) }
        }
    }

    fun searchProduct(query: String) {
        _searchQuery.value = query
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _filteredList.value = fullStockList
            return
        }

        // Local axtarış — ad, barcode, kateqoriya
        val local = fullStockList.filter { s ->
            s.productName?.contains(query, ignoreCase = true) == true ||
                    s.barcode?.contains(query, ignoreCase = true) == true ||
                    s.category?.contains(query, ignoreCase = true) == true
        }

        if (local.isNotEmpty()) {
            _filteredList.value = local
            return
        }

        // Barcode ilə API axtarışı
        if (query.length >= 3) {
            _barcodeLoading.value = true
            when (val result = safeApiCall {
                api.getProductByBarcode(query.trim())
            }) {
                is NetworkResult.Success -> {
                    val dto = result.data
                    _filteredList.value = listOf(
                        StockDto(
                            productId          = dto.id,
                            productName        = dto.name,
                            barcode            = dto.barcode,
                            category           = dto.category,
                            departmentName     = dto.departmentName,
                            totalStock         = 0.0,
                            batchCount         = 0,
                            nearestRemovalDate = null
                        )
                    )
                }
                else -> _filteredList.value = emptyList()
            }
            _barcodeLoading.value = false
        } else {
            _filteredList.value = emptyList()
        }
    }

    fun selectProductFromStock(product: StockDto) {
        viewModelScope.launch {
            _barcodeLoading.value = true
            _filteredList.value   = emptyList()

            android.util.Log.d("LOG_WASTE",
                "Selecting: ${product.productName} " +
                        "id=${product.productId} " +
                        "stock=${product.totalStock}")

            var sellPrice = 0.0
            var unit      = "ədəd"
            var productId = product.productId ?: 0L

            // Barcode endpoint-dən əlavə məlumat al
            val barcode = product.barcode ?: ""
            if (barcode.isNotEmpty()) {
                when (val res = safeApiCall {
                    api.getProductByBarcode(barcode)
                }) {
                    is NetworkResult.Success -> {
                        sellPrice = res.data.sellPrice ?: 0.0
                        unit      = res.data.unit ?: "ədəd"
                        // productId-ni təsdiqlə
                        if (res.data.id != null) productId = res.data.id
                        android.util.Log.d("LOG_WASTE",
                            "Product details: sellPrice=$sellPrice " +
                                    "unit=$unit id=$productId")
                    }
                    else -> Unit
                }
            }

            // Cache-dən batchId tap
            val batchId = findBatchIdFromCache(productId, product.productName ?: "")

            android.util.Log.d("LOG_WASTE",
                "Final: productId=$productId batchId=$batchId " +
                        "stock=${product.totalStock} sellPrice=$sellPrice")

            _selectedProduct.value = SelectedProductInfo(
                productId   = productId,
                productName = product.productName ?: "--",
                barcode     = barcode,
                totalStock  = product.totalStock ?: 0.0,
                sellPrice   = sellPrice,
                unit        = unit,
                batchId     = batchId
            )

            _barcodeLoading.value = false
            calculateLoss(1.0, sellPrice)
        }
    }

    // Cache-dən batchId tap — çoxlu strategiya
    private fun findBatchIdFromCache(productId: Long, productName: String): Long? {
        if (cachedReminders.isEmpty()) {
            android.util.Log.w("LOG_WASTE", "No reminders in cache!")
            return null
        }

        // Strategiya 1: batchCode-da productId var (BC-store-productId-date)
        val byCode = cachedReminders.find { r ->
            val parts = r.batchCode?.split("-")
            parts?.getOrNull(2)?.toLongOrNull() == productId
        }
        if (byCode != null) {
            android.util.Log.d("LOG_WASTE",
                "BatchId found by code: ${byCode.batchId}")
            return byCode.batchId
        }

        // Strategiya 2: product adı tam uyğun gəlir
        val byExactName = cachedReminders.find { r ->
            r.productName?.trim()
                ?.equals(productName.trim(), ignoreCase = true) == true
        }
        if (byExactName != null) {
            android.util.Log.d("LOG_WASTE",
                "BatchId found by exact name: ${byExactName.batchId}")
            return byExactName.batchId
        }

        // Strategiya 3: product adı qismən uyğun gəlir
        val byPartialName = cachedReminders.find { r ->
            r.productName?.contains(productName, ignoreCase = true) == true ||
                    productName.contains(r.productName ?: "", ignoreCase = true)
        }
        if (byPartialName != null) {
            android.util.Log.d("LOG_WASTE",
                "BatchId found by partial name: ${byPartialName.batchId}")
            return byPartialName.batchId
        }

        // Strategiya 4: batchCode-da productId string kimi var
        val byCodeString = cachedReminders.find { r ->
            r.batchCode?.contains(productId.toString()) == true
        }
        if (byCodeString != null) {
            android.util.Log.d("LOG_WASTE",
                "BatchId found by code string: ${byCodeString.batchId}")
            return byCodeString.batchId
        }

        // Heç biri tapılmadı — null qaytar, logWaste batchId-siz göndərəcək
        android.util.Log.w("LOG_WASTE",
            "BatchId NOT found for productId=$productId name=$productName")
        android.util.Log.w("LOG_WASTE",
            "Available reminders: ${cachedReminders.map {
                "${it.productName}/${it.batchCode}/${it.batchId}"
            }}")
        return null
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

        if (quantity <= 0) {
            _logState.value = LogWasteUiState.Error("Miqdar 0-dan böyük olmalıdır")
            return
        }

        viewModelScope.launch {
            _logState.value = LogWasteUiState.Loading

            // batchId yoxsa yenidən cache-dən axtar
            val batchId = product.batchId
                ?: findBatchIdFromCache(product.productId, product.productName)

            android.util.Log.d("WASTE_LOG",
                "Sending: productId=${product.productId} " +
                        "batchId=$batchId qty=$quantity reason=$reason")

            // batchId null olsa belə göndər — server qəbul edə bilər
            val request = WasteLogRequestDto(
                productId = product.productId,
                quantity  = quantity,
                reason    = reason,
                batchId   = batchId  // null ola bilər
            )

            _logState.value = when (val result = safeApiCall {
                api.logWaste(request)
            }) {
                is NetworkResult.Success -> {
                    android.util.Log.d("WASTE_LOG",
                        "Success: loss=${result.data.totalLoss}")
                    LogWasteUiState.Success
                }
                is NetworkResult.Error -> {
                    android.util.Log.e("WASTE_LOG",
                        "Error: ${result.message} code=${result.code}")

                    // Əgər batchId xətası verirsə — batchId-siz yenidən cəhd et
                    if (result.message.contains("batch", ignoreCase = true) ||
                        result.code == 400) {
                        android.util.Log.d("WASTE_LOG",
                            "Retrying without batchId...")
                        retryWithoutBatchId(product.productId, quantity, reason)
                    } else {
                        LogWasteUiState.Error(result.message)
                    }
                }
                else -> LogWasteUiState.Error("Xəta baş verdi")
            }
        }
    }

    private suspend fun retryWithoutBatchId(
        productId: Long,
        quantity: Double,
        reason: String
    ): LogWasteUiState {
        return when (val result = safeApiCall {
            api.logWaste(
                WasteLogRequestDto(
                    productId = productId,
                    quantity  = quantity,
                    reason    = reason,
                    batchId   = null
                )
            )
        }) {
            is NetworkResult.Success -> {
                android.util.Log.d("WASTE_LOG", "Retry success!")
                LogWasteUiState.Success
            }
            is NetworkResult.Error -> {
                android.util.Log.e("WASTE_LOG", "Retry failed: ${result.message}")
                LogWasteUiState.Error(result.message)
            }
            else -> LogWasteUiState.Error("Xəta baş verdi")
        }
    }
}