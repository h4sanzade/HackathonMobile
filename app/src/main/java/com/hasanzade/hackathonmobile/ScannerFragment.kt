package com.hasanzade.hackathonmobile

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.hasanzade.hackathonmobile.databinding.FragmentScannerBinding
import com.hasanzade.hackathonmobile.domain.model.ProductModel
import com.hasanzade.hackathonmobile.ui.ScanUiState
import com.hasanzade.hackathonmobile.ui.ScannerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScannerViewModel by viewModels()

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var camera: Camera? = null
    private var isFlashOn = false
    private var isScanning = true
    private var scanLineAnimator: ObjectAnimator? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else {
                Snackbar.make(binding.root, "Kamera icazəsi lazımdır", Snackbar.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        startScanLineAnimation()
        observeViewModel()
        checkCameraPermission()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scanState.collect { state ->
                when (state) {
                    is ScanUiState.Idle -> {
                        binding.tvProductInfo.text = "--"
                        binding.tvScanStatus.text  = "Gözlənir"
                        resetScanLine()
                    }
                    is ScanUiState.Loading -> {
                        binding.tvProductInfo.text = "Axtarılır..."
                        binding.tvScanStatus.text  = "⏳"
                    }
                    is ScanUiState.Success -> {
                        showProductFound(state.product)
                    }
                    is ScanUiState.Error -> {
                        binding.tvProductInfo.text = "Tapılmadı"
                        binding.tvScanStatus.text  = "✗ Xəta"
                        binding.scanLine.setBackgroundColor(
                            android.graphics.Color.parseColor("#DC2626")
                        )
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                        scheduleReset()
                    }
                }
            }
        }
    }

    private fun showProductFound(product: ProductModel) {
        binding.tvLastScan.text    = product.barcode
        binding.tvProductInfo.text = product.name
        binding.tvScanStatus.text  = "✓ Tapıldı"

        binding.scanLine.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.brand_primary)
        )

        vibrate()
        scheduleReset()
    }

    private fun scheduleReset() {
        binding.root.postDelayed({
            if (isAdded) {
                isScanning = true
                viewModel.resetState()
                resetScanLine()
            }
        }, 2500)
    }

    private fun resetScanLine() {
        binding.scanLine.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_scan_line)
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> startCamera()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                        if (isScanning) {
                            isScanning = false
                            requireActivity().runOnUiThread {
                                binding.tvLastScan.text = barcode
                                viewModel.searchBarcode(barcode)
                            }
                        }
                    })
                }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                android.util.Log.e("SCANNER", "Camera error", e)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startScanLineAnimation() {
        binding.scanBox.post {
            val boxHeight = binding.scanBox.height.toFloat()
            scanLineAnimator = ObjectAnimator.ofFloat(
                binding.scanLine, "translationY", 0f, boxHeight - 4f
            ).apply {
                duration     = 1800
                repeatCount  = ValueAnimator.INFINITE
                repeatMode   = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnFlashlight.setOnClickListener {
            isFlashOn = !isFlashOn
            camera?.cameraControl?.enableTorch(isFlashOn)
            binding.tvFlashLabel.text = if (isFlashOn) " Flash: ON" else " Flash"
            binding.btnFlashlight.setCardBackgroundColor(
                if (isFlashOn)
                    ContextCompat.getColor(requireContext(), R.color.brand_primary)
                else
                    android.graphics.Color.parseColor("#1AFFFFFF")
            )
        }

        binding.btnManualEntry.setOnClickListener {
            showManualEntryDialog()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun showManualEntryDialog() {
        val editText = EditText(requireContext()).apply {
            hint      = "Barkodu daxil edin"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Manual Barkod Girişi")
            .setView(editText)
            .setPositiveButton("Axtar") { _, _ ->
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    isScanning = false
                    binding.tvLastScan.text = input
                    viewModel.searchBarcode(input)
                }
            }
            .setNegativeButton("Ləğv et", null)
            .show()
    }

    private fun vibrate() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService(
                    android.os.VibratorManager::class.java
                )
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(android.os.Vibrator::class.java)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        100L,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100L)
            }
        } catch (e: Exception) {
            android.util.Log.e("SCANNER", "Vibrate error", e)
        }
    }

    inner class BarcodeAnalyzer(
        private val onResult: (String) -> Unit
    ) : ImageAnalysis.Analyzer {

        private val scanner = BarcodeScanning.getClient()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close(); return
            }
            val image = InputImage.fromMediaImage(
                mediaImage, imageProxy.imageInfo.rotationDegrees
            )
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.rawValue?.let { onResult(it) }
                }
                .addOnCompleteListener { imageProxy.close() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scanLineAnimator?.cancel()
        cameraExecutor.shutdown()
        _binding = null
    }
}