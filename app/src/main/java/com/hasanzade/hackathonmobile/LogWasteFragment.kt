package com.hasanzade.hackathonmobile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.hasanzade.hackathonmobile.databinding.FragmentLogWasteBinding
import com.hasanzade.hackathonmobile.ui.LogWasteUiState
import com.hasanzade.hackathonmobile.ui.LogWasteViewModel
import com.hasanzade.hackathonmobile.ui.SelectedProductInfo
import com.hasanzade.hackathonmobile.ui.adapter.ProductSearchAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class LogWasteFragment : Fragment() {

    private var _binding: FragmentLogWasteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LogWasteViewModel by viewModels()
    private lateinit var searchAdapter: ProductSearchAdapter

    private var selectedReason = "EXPIRED"
    private var selectedUnit   = "Units"
    private val calendar       = Calendar.getInstance()

    // Reason butonları və labelları
    private val reasonButtons by lazy {
        listOf(
            Triple(binding.btnReasonExpired,  "EXPIRED",  "📅 Expired"),
            Triple(binding.btnReasonDamaged,  "DAMAGED",  "◇ Damaged"),
            Triple(binding.btnReasonQuality,  "QUALITY",  "♻ Quality"),
            Triple(binding.btnReasonRecall,   "RECALL",   "⬆ Recall"),
            Triple(binding.btnReasonOther,    "OTHER",    "··· Other")
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogWasteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        updateDateTimeDisplay()
        selectReason("EXPIRED")
    }

    private fun setupRecyclerView() {
        searchAdapter = ProductSearchAdapter { product ->
            viewModel.selectProductFromStock(product)
            binding.etSearch.setText("")
            binding.rvSearchResults.visibility = View.GONE
        }
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter       = searchAdapter
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }


        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                viewModel.searchProduct(query)
                binding.rvSearchResults.visibility =
                    if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        // Quantity
        binding.etQuantity.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val qty = s?.toString()?.toDoubleOrNull() ?: 0.0
                viewModel.calculateLoss(qty)
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        // Unit selector
        binding.tvUnitUnits.setOnClickListener  { selectUnit("Units") }
        binding.tvUnitKg.setOnClickListener     { selectUnit("KG") }
        binding.tvUnitLiters.setOnClickListener { selectUnit("Liters") }

        // Reason buttons
        binding.btnReasonExpired.setOnClickListener { selectReason("EXPIRED") }
        binding.btnReasonDamaged.setOnClickListener { selectReason("DAMAGED") }
        binding.btnReasonQuality.setOnClickListener { selectReason("QUALITY") }
        binding.btnReasonRecall.setOnClickListener  { selectReason("RECALL") }
        binding.btnReasonOther.setOnClickListener   { selectReason("OTHER") }

        // Date picker
        binding.cardDatetime.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    TimePickerDialog(
                        requireContext(),
                        { _, hour, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            updateDateTimeDisplay()
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Confirm
        binding.btnConfirmLog.setOnClickListener {
            val qty   = binding.etQuantity.text?.toString()?.toDoubleOrNull() ?: 0.0
            val notes = binding.etNotes.text?.toString() ?: ""

            if (qty <= 0) {
                Snackbar.make(binding.root,
                    "Miqdar 0-dan böyük olmalıdır",
                    Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val backendReason = when (selectedReason) {
                "EXPIRED" -> "EXPIRED"
                "DAMAGED" -> "DAMAGED"
                "QUALITY" -> "DAMAGED"
                "RECALL"  -> "REMOVED"
                "OTHER"   -> "REMOVED"
                else      -> "EXPIRED"
            }

            viewModel.logWaste(qty, backendReason, notes)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sectorName.collect { name ->
                binding.tvSectorName.text = "$name Sector"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredList.collect { list ->
                searchAdapter.submitList(list)
                val query = binding.etSearch.text?.toString() ?: ""
                binding.rvSearchResults.visibility =
                    if (list.isNotEmpty() && query.isNotEmpty()) View.VISIBLE
                    else View.GONE
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedProduct.collect { product ->
                if (product != null) {
                    showSelectedProduct(product)

                    binding.etSearch.setText("")
                    binding.rvSearchResults.visibility = View.GONE
                } else {
                    binding.cardSelectedProduct.visibility = View.GONE
                    binding.cardEstimatedLoss.visibility   = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.estimatedLoss.collect { loss ->
                if (loss > 0) {
                    binding.cardEstimatedLoss.visibility = View.VISIBLE
                    binding.tvEstimatedLoss.text =
                        String.format("%.2f AZN", loss)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.barcodeLoading.collect { loading ->
                binding.btnConfirmLog.isEnabled = !loading
                if (loading) {
                    binding.tvProductName.text             = "Axtarılır..."
                    binding.cardSelectedProduct.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logState.collect { state ->
                when (state) {
                    is LogWasteUiState.Loading -> {
                        binding.btnConfirmLog.isEnabled = false
                        binding.btnConfirmLog.text      = "Göndərilir..."
                    }
                    is LogWasteUiState.Success -> {
                        findNavController()
                            .previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("waste_logged", true)

                        Snackbar.make(
                            binding.root,
                            "✓ Waste uğurla qeyd edildi!",
                            Snackbar.LENGTH_SHORT
                        ).show()

                        findNavController().popBackStack()
                    }
                    is LogWasteUiState.Error -> {
                        binding.btnConfirmLog.isEnabled = true
                        binding.btnConfirmLog.text      = "✓  Confirm Log"
                        Snackbar.make(
                            binding.root,
                            "⚠ ${state.msg}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    is LogWasteUiState.Idle -> {
                        binding.btnConfirmLog.isEnabled = true
                        binding.btnConfirmLog.text      = "✓  Confirm Log"
                    }
                }
            }
        }
    }

    private fun showSelectedProduct(product: SelectedProductInfo) {
        binding.cardSelectedProduct.visibility = View.VISIBLE
        binding.rvSearchResults.visibility     = View.GONE
        binding.tvProductName.text  = product.productName
        binding.tvProductSku.text   =
            "Barcode: ${product.barcode} | Stock: ${
                String.format("%.0f", product.totalStock)} ${product.unit}"
        binding.tvUnitPrice.text    =
            if (product.sellPrice > 0)
                String.format("%.2f AZN", product.sellPrice)
            else "-- AZN"
        binding.cardEstimatedLoss.visibility = View.VISIBLE
        val qty = binding.etQuantity.text?.toString()?.toDoubleOrNull() ?: 1.0
        viewModel.calculateLoss(qty, product.sellPrice)
    }

    // ── REASON SEÇİMİ — text rəngi düzəldildi ────────────
    private fun selectReason(reason: String) {
        selectedReason = reason

        val brandColor = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        val cardColor  = ContextCompat.getColor(requireContext(), R.color.surface_card)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val textColor  = ContextCompat.getColor(requireContext(), R.color.text_primary)

        reasonButtons.forEach { (card, btnReason, _) ->
            val isSelected = btnReason == reason
            card.setCardBackgroundColor(if (isSelected) brandColor else cardColor)

            // Bütün TextView-ların rəngini dəyiş
            setAllTextColors(card, if (isSelected) whiteColor else textColor)
        }
    }

    // CardView içindəki bütün TextView-ları tap və rəngini dəyiş
    private fun setAllTextColors(view: View, color: Int) {
        if (view is android.widget.TextView) {
            view.setTextColor(color)
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                setAllTextColors(view.getChildAt(i), color)
            }
        }
    }

    private fun selectUnit(unit: String) {
        selectedUnit = unit
        val white   = ContextCompat.getColor(requireContext(), android.R.color.white)
        val primary = ContextCompat.getColor(requireContext(), R.color.text_primary)

        binding.tvUnitUnits.apply {
            setTextColor(if (unit == "Units") white else primary)
            setBackgroundResource(
                if (unit == "Units") R.drawable.bg_unit_selected else 0)
        }
        binding.tvUnitKg.apply {
            setTextColor(if (unit == "KG") white else primary)
            setBackgroundResource(
                if (unit == "KG") R.drawable.bg_unit_selected else 0)
        }
        binding.tvUnitLiters.apply {
            setTextColor(if (unit == "Liters") white else primary)
            setBackgroundResource(
                if (unit == "Liters") R.drawable.bg_unit_selected else 0)
        }
    }

    private fun updateDateTimeDisplay() {
        val fmt = SimpleDateFormat("MM/dd/yyyy, hh:mm a", Locale.getDefault())
        binding.tvDatetime.text = fmt.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
//use jsonreader.setleneint(true) to accept malformed json at line 1 column 1 path $