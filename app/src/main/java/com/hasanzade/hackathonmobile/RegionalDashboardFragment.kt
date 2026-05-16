package com.hasanzade.hackathonmobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import com.hasanzade.hackathonmobile.databinding.FragmentRegionalDashboardBinding

class RegionalDashboardFragment : Fragment() {

    private var _binding: FragmentRegionalDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegionalDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        animateKpiCards()
        loadMockData()
    }

    private fun loadMockData() {
        binding.tvWasteValue.text = "12.4%"
        binding.tvAiRiskValue.text = "Orta"
        binding.tvSalesValue.text = "₼ 48,230"
        binding.tvStoresValue.text = "7 / 12"
    }

    private fun animateKpiCards() {
        listOf(
            binding.cardWaste,
            binding.cardAiRisk,
            binding.cardSales,
            binding.cardStores
        ).forEachIndexed { i, card ->
            card.alpha = 0f
            card.translationY = 40f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay((i * 90).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}