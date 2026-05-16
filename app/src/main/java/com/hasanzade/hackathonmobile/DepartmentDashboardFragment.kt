package com.hasanzade.hackathonmobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import com.hasanzade.hackathonmobile.databinding.FragmentDepartmentDashboardBinding

class DepartmentDashboardFragment : Fragment() {

    private var _binding: FragmentDepartmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDepartmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        animateCards()
        loadMockData()
    }

    private fun loadMockData() {
        binding.tvCriticalValue.text = "3"
        binding.tvExpiryValue.text = "2 gün"
        binding.tvRemovalValue.text = "5 məhsul"
        binding.tvStockValue.text = "Aşağı"
    }

    private fun animateCards() {
        listOf(
            binding.cardCritical,
            binding.cardExpiry,
            binding.cardRemoval,
            binding.cardStock
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