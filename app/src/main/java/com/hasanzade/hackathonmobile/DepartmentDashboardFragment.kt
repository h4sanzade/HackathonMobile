package com.hasanzade.hackathonmobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.databinding.FragmentDepartmentDashboardBinding
import com.hasanzade.hackathonmobile.domain.model.DashboardModel
import com.hasanzade.hackathonmobile.ui.DashboardUiState
import com.hasanzade.hackathonmobile.ui.DepartmentDashboardViewModel
import com.hasanzade.hackathonmobile.ui.adapter.RiskyBatchAdapter
import com.hasanzade.hackathonmobile.ui.model.RiskyBatchUiModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DepartmentDashboardFragment : Fragment() {

    private var _binding: FragmentDepartmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DepartmentDashboardViewModel by viewModels()
    private val adapter = RiskyBatchAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDepartmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupBottomNav()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvRiskyBatches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiskyBatches.adapter = adapter
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_dashboard
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_alerts -> true
                R.id.nav_scanner -> {
                    findNavController().navigate(
                        R.id.action_departmentDashboard_to_scannerFragment
                    )
                    true
                }
                R.id.nav_profile -> {
                    logout()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        binding.cardScanBarcode.setOnClickListener {
            findNavController().navigate(
                R.id.action_departmentDashboard_to_scannerFragment
            )
        }
        binding.cardLogWaste.setOnClickListener {
            // TODO: Log Waste fragment
        }
        binding.ivSettings.setOnClickListener {
            logout()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is DashboardUiState.Loading -> showLoading()
                    is DashboardUiState.Success -> showData(state.data)
                    is DashboardUiState.Error   -> showError(state.message)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resolveState.collect { result ->
                when (result) {
                    is NetworkResult.Success ->
                        showSnackbar("✓ ${result.data}")
                    is NetworkResult.Error ->
                        showSnackbar("⚠ ${result.message}")
                    else -> Unit
                }
            }
        }
    }

    private fun showLoading() {
        binding.tvSectorLabel.text              = "Sector View: --"
        binding.tvWasteLabel.text               = "-- WASTE"
        binding.tvWasteAmount.text              = "--"
        binding.tvWasteTrend.text               = "↑ --%"
        binding.tvStockHealthPercent.text       = "--%"
        binding.tvStockHealthLabel.text         = "--"
        binding.progressStockHealth.progress    = 0
        binding.tvNeedsAttention.text           = "--"
        binding.tvBatchesPlaceholder.text       = "Yüklənir..."
        binding.tvBatchesPlaceholder.visibility = View.VISIBLE
        binding.rvRiskyBatches.visibility       = View.GONE
    }

    private fun showData(data: DashboardModel) {
        // Header
        binding.tvSectorLabel.text = "Sector View: ${data.departmentName}"

        // Waste card
        binding.tvWasteLabel.text  = "${data.departmentName.uppercase()} WASTE"
        binding.tvWasteAmount.text = String.format("%.0f", data.wasteAmount)
        val trendSign = if (data.wasteTrend >= 0) "↑" else "↓"
        binding.tvWasteTrend.text  = "$trendSign ${String.format("%.1f", kotlin.math.abs(data.wasteTrend))}%"

        // Stock health card
        binding.tvStockHealthPercent.text    =
            if (data.stockHealth == 0) "--%"
            else "${data.stockHealth}%"
        binding.tvStockHealthLabel.text      = data.stockHealthLabel
        binding.progressStockHealth.progress = data.stockHealth

        // Risky batches
        binding.tvNeedsAttention.text =
            if (data.riskyBatches.isEmpty()) "Təhlükəsiz"
            else "Diqqət tələb edir (${data.riskyBatches.size})"

        val uiModels = data.riskyBatches.map { r ->
            RiskyBatchUiModel.fromReminder(
                productName      = r.productName,
                batchCode        = r.batchCode,
                daysLeft         = r.daysLeft,
                urgency          = r.urgency,
                quantity         = r.quantity,
                originalQuantity = r.quantity * 2
            )
        }

        if (uiModels.isEmpty()) {
            binding.tvBatchesPlaceholder.text       = "Aktiv batch yoxdur"
            binding.tvBatchesPlaceholder.visibility = View.VISIBLE
            binding.rvRiskyBatches.visibility       = View.GONE
        } else {
            binding.tvBatchesPlaceholder.visibility = View.GONE
            binding.rvRiskyBatches.visibility       = View.VISIBLE
            adapter.submitList(uiModels)
        }

        animateCards()
    }

    private fun showError(message: String) {
        binding.tvBatchesPlaceholder.text       = "⚠ $message"
        binding.tvBatchesPlaceholder.visibility = View.VISIBLE
        binding.rvRiskyBatches.visibility       = View.GONE
        showSnackbar(message)
    }

    private fun animateCards() {
        listOf(
            binding.cardWaste,
            binding.cardStockHealth,
            binding.cardScanBarcode,
            binding.cardLogWaste
        ).forEachIndexed { i, card ->
            card.alpha       = 0f
            card.translationY = 30f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay((i * 70).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun logout() {
        viewLifecycleOwner.lifecycleScope.launch {
            TokenDataStore(requireContext()).clear()
            if (isAdded) {
                findNavController().navigate(
                    R.id.action_departmentDashboard_to_loginFragment
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}