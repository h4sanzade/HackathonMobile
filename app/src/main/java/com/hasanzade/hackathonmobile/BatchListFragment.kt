package com.hasanzade.hackathonmobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hasanzade.hackathonmobile.databinding.FragmentBatchListBinding
import com.hasanzade.hackathonmobile.ui.BatchListViewModel
import com.hasanzade.hackathonmobile.ui.adapter.BatchAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BatchListFragment : Fragment() {

    private var _binding: FragmentBatchListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BatchListViewModel by viewModels()
    private val adapter = BatchAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvBatches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBatches.adapter = adapter

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.tvPlaceholder.text =
                    if (loading) "Yüklənir..." else "Batch tapılmadı"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.displayItems.collect { list ->
                if (list.isEmpty()) {
                    binding.tvPlaceholder.visibility = View.VISIBLE
                    binding.rvBatches.visibility     = View.GONE
                } else {
                    binding.tvPlaceholder.visibility = View.GONE
                    binding.rvBatches.visibility     = View.VISIBLE
                    adapter.submitList(list)
                }

                binding.tvBatchSubtitle.text  = "${list.size} aktiv batch"
                binding.tvTotalBatches.text   = list.size.toString()
                binding.tvCriticalCount.text  =
                    list.count { it.urgency == "CRITICAL" }.toString()

                val totalRisk = list.sumOf {
                    if (it.sellPrice > 0) it.quantity * it.sellPrice
                    else it.totalValueAzn
                }
                binding.tvTotalRiskAzn.text =
                    "${String.format("%.2f", totalRisk)} AZN"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}