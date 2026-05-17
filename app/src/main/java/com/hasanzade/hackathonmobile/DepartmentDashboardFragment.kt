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
import com.hasanzade.hackathonmobile.data.remote.NetworkResult
import com.hasanzade.hackathonmobile.databinding.FragmentDepartmentDashboardBinding
import com.hasanzade.hackathonmobile.domain.model.DashboardModel
import com.hasanzade.hackathonmobile.ui.DashboardUiState
import com.hasanzade.hackathonmobile.ui.DepartmentDashboardViewModel
import com.hasanzade.hackathonmobile.ui.adapter.RiskyBatchAdapter
import com.hasanzade.hackathonmobile.ui.adapter.WasteLogAdapter
import com.hasanzade.hackathonmobile.ui.model.RiskyBatchUiModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DepartmentDashboardFragment : Fragment() {

    private var _binding: FragmentDepartmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DepartmentDashboardViewModel by viewModels()
    private val riskyBatchAdapter = RiskyBatchAdapter()
    private val wasteLogAdapter   = WasteLogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDepartmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupBottomNav()
        setupClickListeners()
        observeViewModel()

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>("waste_logged")
            ?.observe(viewLifecycleOwner) { wasLogged ->
                if (wasLogged == true) {
                    viewModel.loadDashboard()
                }
            }
    }

    private fun setupRecyclerViews() {
        binding.rvRiskyBatches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiskyBatches.adapter = riskyBatchAdapter

        binding.rvWasteLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWasteLogs.adapter = wasteLogAdapter
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_dashboard
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_alerts    -> true
                R.id.nav_scanner   -> {
                    findNavController().navigate(
                        R.id.action_departmentDashboard_to_scannerFragment
                    )
                    true
                }
                R.id.nav_profile   -> {
                    findNavController().navigate(
                        R.id.action_departmentDashboard_to_profileFragment
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {

        binding.cardBatches.setOnClickListener {
            findNavController().navigate(
                R.id.action_departmentDashboard_to_batchListFragment
            )
        }
        binding.cardScanBarcode.setOnClickListener {
            findNavController().navigate(
                R.id.action_departmentDashboard_to_scannerFragment
            )
        }
        binding.cardLogWaste.setOnClickListener {
            findNavController().navigate(
                R.id.action_departmentDashboard_to_logWasteFragment
            )
        }
        binding.ivSettings.setOnClickListener {
            findNavController().navigate(
                R.id.action_departmentDashboard_to_profileFragment
            )
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
                    is NetworkResult.Success -> showSnackbar("✓ ${result.data}")
                    is NetworkResult.Error   -> showSnackbar("⚠ ${result.message}")
                    else -> Unit
                }
            }
        }
    }

    private fun showLoading() {
        binding.tvAppName.text               = "FreshGuard"
        binding.tvSectorLabel.text           = "Yüklənir..."
        binding.tvWasteLabel.text            = "-- WASTE"
        binding.tvWasteAmount.text           = "--"
        binding.tvWasteTrend.text            = "↑ --%"
        binding.tvStockHealthPercent.text    = "--%"
        binding.tvStockHealthLabel.text      = "--"
        binding.progressStockHealth.progress = 0
        binding.tvNeedsAttention.text        = "--"
        binding.tvBatchesPlaceholder.text    = "Yüklənir..."
        binding.tvBatchesPlaceholder.visibility = View.VISIBLE
        binding.rvRiskyBatches.visibility    = View.GONE
        binding.tvWasteLogPlaceholder.text   = "Yüklənir..."
        binding.tvWasteLogPlaceholder.visibility = View.VISIBLE
        binding.rvWasteLogs.visibility       = View.GONE
    }

    private fun showData(data: DashboardModel) {
        binding.tvAppName.text = data.displayName
        binding.tvSectorLabel.text = buildString {
            if (data.storeName != "--") append(data.storeName)
            if (data.storeName != "--" && data.departmentName != "--") append(" · ")
            if (data.departmentName != "--") append(data.departmentName)
            if (isEmpty()) append("--")
        }

        // ── WASTE ─────────────────────────────────────────────
        binding.tvWasteLabel.text = "RİSK MALLAR"
        binding.tvWasteAmount.text = String.format("%.0f", data.wasteAmount)

        val trendSign = if (data.wasteTrend >= 0) "↑" else "↓"
        val trendAbs  = kotlin.math.abs(data.wasteTrend)
        binding.tvWasteTrend.text = "$trendSign ${String.format("%.1f", trendAbs)}% stokdan"
        binding.tvWasteTrend.setTextColor(
            if (data.wasteTrend > 10)
                requireContext().getColor(R.color.error)
            else
                requireContext().getColor(R.color.brand_primary)
        )

        // ── STOCK HEALTH ──────────────────────────────────────
        binding.tvStockHealthPercent.text =
            if (data.stockHealth == 0) "--%"
            else "${data.stockHealth}%"

        binding.tvStockHealthLabel.text = data.stockHealthLabel

        // Rəng: zəif=qırmızı, orta=narıncı, yaxşı=yaşıl
        binding.tvStockHealthPercent.setTextColor(
            when {
                data.stockHealth < 50 -> requireContext().getColor(R.color.error)
                data.stockHealth < 80 -> android.graphics.Color.parseColor("#D97706")
                else                  -> requireContext().getColor(R.color.brand_primary)
            }
        )
        binding.progressStockHealth.progress = data.stockHealth
        binding.progressStockHealth.progressTintList =
            android.content.res.ColorStateList.valueOf(
                when {
                    data.stockHealth < 50 ->
                        requireContext().getColor(R.color.error)
                    data.stockHealth < 80 ->
                        android.graphics.Color.parseColor("#D97706")
                    else ->
                        requireContext().getColor(R.color.brand_primary)
                }
            )

        // ── NEEDS ATTENTION ───────────────────────────────────
        binding.tvNeedsAttention.text =
            if (data.riskyBatches.isEmpty()) "✓ Təhlükəsiz"
            else "⚠ ${data.criticalCount} kritik"

        // ── RISKY BATCHES ─────────────────────────────────────
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
            binding.tvBatchesPlaceholder.text       = "✓ Bütün məhsullar sağlamdır"
            binding.tvBatchesPlaceholder.visibility = View.VISIBLE
            binding.rvRiskyBatches.visibility       = View.GONE
        } else {
            binding.tvBatchesPlaceholder.visibility = View.GONE
            binding.rvRiskyBatches.visibility       = View.VISIBLE
            riskyBatchAdapter.submitList(uiModels)
        }

        // ── WASTE LOGS ────────────────────────────────────────
        val totalWaste = data.wasteLogs.sumOf { it.totalLoss }
        binding.tvTotalWasteLabel.text =
            "Cəmi risk: ${String.format("%.2f", totalWaste)} AZN"

        if (data.wasteLogs.isEmpty()) {
            binding.tvWasteLogPlaceholder.text       = "✓ İsraf qeydi yoxdur"
            binding.tvWasteLogPlaceholder.visibility = View.VISIBLE
            binding.rvWasteLogs.visibility           = View.GONE
        } else {
            binding.tvWasteLogPlaceholder.visibility = View.GONE
            binding.rvWasteLogs.visibility           = View.VISIBLE
            wasteLogAdapter.submitList(data.wasteLogs.takeLast(5).reversed())
        }

        animateCards()
    }

    private fun showError(message: String) {
        binding.tvBatchesPlaceholder.text        = "⚠ $message"
        binding.tvBatchesPlaceholder.visibility  = View.VISIBLE
        binding.rvRiskyBatches.visibility        = View.GONE
        binding.tvWasteLogPlaceholder.text       = "⚠ $message"
        binding.tvWasteLogPlaceholder.visibility = View.VISIBLE
        binding.rvWasteLogs.visibility           = View.GONE
        showSnackbar(message)
    }

    private fun animateCards() {
        listOf(
            binding.cardWaste,
            binding.cardStockHealth,
            binding.cardScanBarcode,
            binding.cardLogWaste,
            binding.cardBatches
        ).forEachIndexed { i, card ->
            card.alpha        = 0f
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

    private fun showSnackbar(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}