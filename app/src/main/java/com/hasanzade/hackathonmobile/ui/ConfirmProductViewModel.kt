package com.hasanzade.hackathonmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.data.remote.api.ApiService
import com.hasanzade.hackathonmobile.data.remote.dto.AddBatchRequestDto
import com.hasanzade.hackathonmobile.data.remote.dto.CreateProductRequestDto
import com.hasanzade.hackathonmobile.data.remote.dto.ProductDto
import com.hasanzade.hackathonmobile.data.remote.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ConfirmProductUiState {
    object Idle                       : ConfirmProductUiState()
    object Loading                    : ConfirmProductUiState()
    object SavedAndScanNext           : ConfirmProductUiState()
    object SavedAndDone               : ConfirmProductUiState()
    data class Error(val msg: String) : ConfirmProductUiState()
}

data class ScannedProductState(
    val barcode: String       = "",
    val productName: String   = "",
    val category: String      = "",
    val departmentId: Long    = 0L,
    val unit: String          = "ədəd",
    val sellPrice: Double     = 0.0,
    val costPrice: Double     = 0.0,
    val productId: Long?      = null,   // backend-dən gəlir
    val isLoading: Boolean    = true,
    val error: String?        = null
)

@HiltViewModel
class ConfirmProductViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _productState = MutableStateFlow(ScannedProductState())
    val productState: StateFlow<ScannedProductState> = _productState

    private val _uiState = MutableStateFlow<ConfirmProductUiState>(ConfirmProductUiState.Idle)
    val uiState: StateFlow<ConfirmProductUiState> = _uiState

    // Saxlanmış məhsulların siyahısı — tracking üçün
    private val _savedProducts = MutableStateFlow<List<SavedProductEntry>>(emptyList())
    val savedProducts: StateFlow<List<SavedProductEntry>> = _savedProducts

    private var currentFilial     = ""
    private var currentDepartment = ""
    private var currentDeptId     = 0L

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            currentFilial     = tokenDataStore.getFilial() ?: ""
            currentDepartment = tokenDataStore.getDepartment() ?: ""
        }
    }

    // Barkod scan olduqda çağır — məhsulu API-dən çək
    fun loadProductByBarcode(barcode: String) {
        viewModelScope.launch {
            _productState.value = ScannedProductState(
                barcode   = barcode,
                isLoading = true
            )

            when (val result = safeApiCall {
                api.getProductByBarcode(barcode)
            }) {
                is NetworkResult.Success -> {
                    val dto = result.data
                    _productState.value = ScannedProductState(
                        barcode      = barcode,
                        productName  = dto.name ?: "",
                        category     = dto.category ?: "",
                        departmentId = dto.departmentId ?: 0L,
                        unit         = dto.unit ?: "ədəd",
                        sellPrice    = dto.sellPrice ?: 0.0,
                        costPrice    = dto.costPrice ?: 0.0,
                        productId    = dto.id,
                        isLoading    = false
                    )
                    currentDeptId = dto.departmentId ?: 0L
                }
                is NetworkResult.Error -> {
                    // Məhsul tapılmadı — formu boş aç, user doldursun
                    _productState.value = ScannedProductState(
                        barcode   = barcode,
                        isLoading = false,
                        error     = null // xəta göstərmə, boş form aç
                    )
                    android.util.Log.d("CONFIRM",
                        "Product not found for barcode: $barcode — opening empty form")
                }
                else -> Unit
            }
        }
    }

    // Save Product — məhsul + batch saxla
    fun saveProduct(
        productName: String,
        quantity: Double,
        category: String,
        arrivalDate: String,
        removalDate: String,
        expiryDate: String,
        scanNext: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = ConfirmProductUiState.Loading

            val state = _productState.value

            // Validasiya
            if (productName.isBlank()) {
                _uiState.value = ConfirmProductUiState.Error("Məhsul adı boş ola bilməz")
                return@launch
            }
            if (arrivalDate.isBlank() || removalDate.isBlank()) {
                _uiState.value = ConfirmProductUiState.Error("Tarixlər doldurulmalıdır")
                return@launch
            }

            try {
                // Məhsul ID-si yoxdursa — yeni məhsul yarat
                val productId = if (state.productId != null) {
                    state.productId
                } else {
                    createProduct(
                        name         = productName,
                        barcode      = state.barcode,
                        category     = category,
                        departmentId = currentDeptId
                    ) ?: run {
                        _uiState.value = ConfirmProductUiState.Error(
                            "Məhsul yaradıla bilmədi"
                        )
                        return@launch
                    }
                }

                // Batch əlavə et
                val userId = tokenDataStore.getUserId() ?: ""
                val batchResult = safeApiCall {
                    api.addBatch(
                        AddBatchRequestDto(
                            productId     = productId,
                            quantity      = quantity,
                            deliveryDate  = arrivalDate,
                            removalDate   = removalDate,
                            addedByUserId = userId
                        )
                    )
                }

                when (batchResult) {
                    is NetworkResult.Success -> {
                        val batchCode = batchResult.data

                        // Tracking listinə əlavə et
                        val entry = SavedProductEntry(
                            productName  = productName,
                            barcode      = state.barcode,
                            quantity     = quantity,
                            batchCode    = batchCode ?: "--",
                            arrivalDate  = arrivalDate,
                            removalDate  = removalDate,
                            category     = category
                        )
                        _savedProducts.value = _savedProducts.value + entry

                        android.util.Log.d("CONFIRM",
                            "Saved: $productName batch=$batchCode")

                        _uiState.value = if (scanNext)
                            ConfirmProductUiState.SavedAndScanNext
                        else
                            ConfirmProductUiState.SavedAndDone
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = ConfirmProductUiState.Error(batchResult.message)
                    }
                    else -> Unit
                }

            } catch (e: Exception) {
                _uiState.value = ConfirmProductUiState.Error(
                    e.localizedMessage ?: "Xəta baş verdi"
                )
            }
        }
    }

    private suspend fun createProduct(
        name: String,
        barcode: String,
        category: String,
        departmentId: Long
    ): Long? {
        val result = safeApiCall {
            api.createProduct(
                CreateProductRequestDto(
                    name         = name,
                    barcode      = barcode.ifEmpty { null },
                    category     = mapCategoryToEnum(category),
                    departmentId = departmentId,
                    unit         = "ədəd",
                    costPrice    = null,
                    sellPrice    = null
                )
            )
        }
        return when (result) {
            is NetworkResult.Success -> result.data.id
            else -> null
        }
    }

    private fun mapCategoryToEnum(label: String): String {
        return when (label) {
            "Meyvə"           -> "FRUIT"
            "Tərəvəz"         -> "VEGETABLE"
            "Ət və Toyuq"     -> "MEAT"
            "Süd məhsulları"  -> "DAIRY"
            "Çörək və Pastry" -> "BREAD"
            "Yumurta"         -> "EGG"
            "Şirniyyat"       -> "CONFECTIONERY"
            "İçki"            -> "BEVERAGE"
            else              -> "OTHER"
        }
    }

    fun resetUiState() {
        _uiState.value = ConfirmProductUiState.Idle
    }
}

data class SavedProductEntry(
    val productName: String,
    val barcode: String,
    val quantity: Double,
    val batchCode: String,
    val arrivalDate: String,
    val removalDate: String,
    val category: String,
    val savedAt: String = java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
)