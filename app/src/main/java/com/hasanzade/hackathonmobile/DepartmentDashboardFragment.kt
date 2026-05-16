package com.hasanzade.hackathonmobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import com.hasanzade.hackathonmobile.databinding.FragmentDepartmentDashboardBinding
import com.hasanzade.hackathonmobile.ui.DepartmentDashboardViewModel
import com.hasanzade.hackathonmobile.ui.adapter.RiskyBatchAdapter
import kotlinx.coroutines.launch

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

        // Mock data yüklə (API hazır olanda viewModel.loadDashboard(...) ilə əvəz et)
        viewModel.loadMockData()
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
                R.id.nav_alerts    -> {
                    // TODO: AlertsFragment-ə navigate et
                    true
                }
                R.id.nav_scanner   -> {
                    // TODO: ScannerFragment-ə navigate et
                    true
                }
                R.id.nav_profile   -> {
                    // Logout
                    logout()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        binding.cardScanBarcode.setOnClickListener {
            // TODO: Scanner-ə navigate et
        }
        binding.cardLogWaste.setOnClickListener {
            // TODO: Log Waste-ə navigate et
        }
        binding.ivSettings.setOnClickListener {
            logout()
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->

            // Header
            binding.tvSectorLabel.text = "Sector View: ${state.sectorName}"

            // KPI — Waste
            binding.tvWasteLabel.text  = "${state.sectorName.uppercase()} WASTE"
            binding.tvWasteAmount.text = state.wasteAmount
            binding.tvWasteTrend.text  = state.wasteTrend

            // KPI — Stock Health
            binding.tvStockHealthPercent.text = if (state.stockHealthPercent == 0)
                "--%"
            else
                "${state.stockHealthPercent}%"
            binding.tvStockHealthLabel.text   = state.stockHealthLabel
            binding.progressStockHealth.progress = state.stockHealthPercent

            // Risky batches header
            binding.tvNeedsAttention.text = state.attentionCount

            // Risky batches list
            if (state.isLoading) {
                binding.tvBatchesPlaceholder.visibility = View.VISIBLE
                binding.rvRiskyBatches.visibility       = View.GONE
            } else {
                binding.tvBatchesPlaceholder.visibility = View.GONE
                binding.rvRiskyBatches.visibility       = View.VISIBLE
                adapter.submitList(state.riskyBatches)
            }

            // Animate cards
            animateCards()
        }
    }

    private fun animateCards() {
        listOf(binding.cardWaste, binding.cardStockHealth,
            binding.cardScanBarcode, binding.cardLogWaste)
            .forEachIndexed { i, card ->
                card.alpha = 0f
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

    private fun logout() {
        lifecycleScope.launch {
            TokenDataStore(requireContext()).clear()
            if (isAdded) {
                findNavController().navigate(
                    R.id.action_departmentDashboard_to_loginFragment
                )
            }
        }
    }

    private fun findNavController() =
        androidx.navigation.fragment.NavHostFragment.findNavController(this)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}