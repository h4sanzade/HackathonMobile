package com.hasanzade.hackathonmobile

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.hasanzade.hackathonmobile.databinding.FragmentConfirmProductBinding
import com.hasanzade.hackathonmobile.ui.ConfirmProductUiState
import com.hasanzade.hackathonmobile.ui.ConfirmProductViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ConfirmProductFragment : Fragment() {

    private var _binding: FragmentConfirmProductBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConfirmProductViewModel by viewModels()

    private val categories = listOf(
        "Meyvə", "Tərəvəz", "Ət və Toyuq",
        "Süd məhsulları", "Çörək və Pastry",
        "Yumurta", "Şirniyyat", "İçki", "Digər"
    )

    private var selectedCategory = ""
    private var arrivalDate      = ""
    private var removalDate      = ""
    private var expiryDate       = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Safe Args yox — Bundle ilə al
        val barcode = arguments?.getString("barcode") ?: ""
        viewModel.loadProductByBarcode(barcode)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.cardCategory.setOnClickListener {
            showCategoryDialog()
        }

        binding.cardArrivalDate.setOnClickListener {
            showDatePicker { date ->
                arrivalDate = date
                binding.tvArrivalDate.text = date
                binding.tvArrivalDate.setTextColor(
                    requireContext().getColor(R.color.text_primary)
                )
            }
        }

        binding.cardRemovalDate.setOnClickListener {
            showDatePicker { date ->
                removalDate = date
                binding.tvRemovalDate.text = date
                binding.tvRemovalDate.setTextColor(
                    requireContext().getColor(R.color.text_primary)
                )
            }
        }

        binding.cardExpiryDate.setOnClickListener {
            showDatePicker { date ->
                expiryDate = date
                binding.tvExpiryDate.text = date
                binding.tvExpiryDate.setTextColor(
                    requireContext().getColor(R.color.error)
                )
            }
        }

        binding.btnSaveProduct.setOnClickListener {
            save(scanNext = false)
        }

        binding.btnScanNext.setOnClickListener {
            save(scanNext = true)
        }
    }

    private fun save(scanNext: Boolean) {
        val name             = binding.etProductName.text?.toString()?.trim() ?: ""
        val quantity         = binding.etQuantity.text?.toString()
            ?.toDoubleOrNull() ?: 1.0
        val finalRemovalDate = removalDate.ifEmpty { expiryDate }

        viewModel.saveProduct(
            productName  = name,
            quantity     = quantity,
            category     = selectedCategory.ifEmpty { "Digər" },
            arrivalDate  = arrivalDate,
            removalDate  = finalRemovalDate,
            expiryDate   = expiryDate,
            scanNext     = scanNext
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.productState.collect { state ->
                if (state.isLoading) {
                    binding.etProductName.hint = "Yüklənir..."
                    return@collect
                }

                // Məhsul adını doldur
                if (state.productName.isNotEmpty()) {
                    binding.etProductName.setText(state.productName)
                } else {
                    binding.etProductName.hint = "Məhsul adını daxil edin"
                }

                // Kateqoriyanı doldur
                if (state.category.isNotEmpty()) {
                    selectedCategory        = state.category
                    binding.tvCategory.text = state.category
                    binding.tvCategory.setTextColor(
                        requireContext().getColor(R.color.text_primary)
                    )
                }

                // Barkod badge
                binding.tvBarcodeBadge.text =
                    if (state.barcode.isNotEmpty()) "⬛ ${state.barcode}"
                    else "⬛ Manual"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is ConfirmProductUiState.Loading -> {
                        binding.btnSaveProduct.isEnabled = false
                        binding.btnScanNext.isEnabled    = false
                        binding.btnSaveProduct.text      = "Saxlanılır..."
                    }
                    is ConfirmProductUiState.SavedAndDone -> {
                        viewModel.resetUiState()
                        Snackbar.make(
                            binding.root,
                            "✓ Məhsul uğurla saxlandı!",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(
                            R.id.action_confirmProductFragment_to_departmentDashboardFragment
                        )
                    }
                    is ConfirmProductUiState.SavedAndScanNext -> {
                        viewModel.resetUiState()
                        Snackbar.make(
                            binding.root,
                            "✓ Saxlandı! Növbəti məhsulu scan edin.",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(
                            R.id.action_confirmProductFragment_to_scannerFragment
                        )
                    }
                    is ConfirmProductUiState.Error -> {
                        binding.btnSaveProduct.isEnabled = true
                        binding.btnScanNext.isEnabled    = true
                        binding.btnSaveProduct.text      = "💾 Save Product"
                        Snackbar.make(
                            binding.root,
                            "⚠ ${state.msg}",
                            Snackbar.LENGTH_LONG
                        ).show()
                        viewModel.resetUiState()
                    }
                    else -> {
                        binding.btnSaveProduct.isEnabled = true
                        binding.btnScanNext.isEnabled    = true
                        binding.btnSaveProduct.text      = "💾 Save Product"
                    }
                }
            }
        }
    }

    private fun showCategoryDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Kateqoriya seç")
            .setItems(categories.toTypedArray()) { _, which ->
                selectedCategory        = categories[which]
                binding.tvCategory.text = selectedCategory
                binding.tvCategory.setTextColor(
                    requireContext().getColor(R.color.text_primary)
                )
            }
            .show()
    }

    private fun showDatePicker(onDate: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                cal.set(year, month, day)
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                onDate(fmt.format(cal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}