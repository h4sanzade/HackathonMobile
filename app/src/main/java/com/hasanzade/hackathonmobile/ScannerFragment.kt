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
    private var isFlashOn    = false
    private var isScanning   = true
    private var scanLineAnimator: ObjectAnimator? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else {
                Snackbar.make(
                    binding.root,
                    "Kamera icazəsi lazımdır",
                    Snackbar.LENGTH_LONG
                ).show()
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

    // ── VIEWMODEL OBSERVE ─────────────────────────────────
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scanState.collect { state ->
                when (state) {
                    is ScanUiState.Idle -> {
                        binding.tvProductInfo.text = "--"
                        binding.tvScanStatus.text  = "Gözlənir"
                        resetScanLine()
                        isScanning = true
                    }
                    is ScanUiState.Loading -> {
                        binding.tvProductInfo.text = "Axtarılır..."
                        binding.tvScanStatus.text  = "⏳"
                    }
                    is ScanUiState.Success -> {
                        showProductFound(state.product)
                    }
                    is ScanUiState.Error -> {
                        // Məhsul tapılmadı — yenə də ConfirmProduct-a keç
                        // User özü məlumatları doldursun
                        binding.tvProductInfo.text = "Tapılmadı"
                        binding.tvScanStatus.text  = "ℹ Yeni məhsul"
                        binding.scanLine.setBackgroundColor(
                            android.graphics.Color.parseColor("#F97316")
                        )
                        Snackbar.make(
                            binding.root,
                            "Məhsul tapılmadı — əl ilə doldura bilərsiniz",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        // 1.5s sonra ConfirmProduct-a keç — boş form
                        val lastBarcode = binding.tvLastScan.text.toString()
                        binding.root.postDelayed({
                            if (isAdded) {
                                navigateToConfirm(lastBarcode)
                            }
                        }, 1500)
                    }
                }
            }
        }
    }

    // ── MƏHSUL TAPILDI ────────────────────────────────────
    private fun showProductFound(product: ProductModel) {
        binding.tvLastScan.text    = product.barcode
        binding.tvProductInfo.text = product.name
        binding.tvScanStatus.text  = "✓ Tapıldı"

        binding.scanLine.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.brand_primary)
        )
        vibrate()

        // 1.5s sonra ConfirmProduct ekranına keç
        binding.root.postDelayed({
            if (isAdded) {
                navigateToConfirm(product.barcode)
            }
        }, 1500)
    }

    // ── CONFIRM PRODUCT EKRANINA KEÇ ─────────────────────
    private fun navigateToConfirm(barcode: String) {
        try {
            val bundle = Bundle().apply {
                putString("barcode", barcode.ifEmpty { "MANUAL" })
            }
            findNavController().navigate(
                R.id.action_scannerFragment_to_confirmProductFragment,
                bundle
            )
        } catch (e: Exception) {
            android.util.Log.e("SCANNER", "Navigate error", e)
        }
    }

    // ── KAMERA İCAZƏSİ ────────────────────────────────────
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> startCamera()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ── KAMERA BAŞLAT ─────────────────────────────────────
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

    // ── SCAN LINE ANİMASİYA ───────────────────────────────
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

    // ── DÜYMƏLƏR ──────────────────────────────────────────
    private fun setupClickListeners() {

        // Flashlight
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

        // Manual Entry
        binding.btnManualEntry.setOnClickListener {
            showManualEntryDialog()
        }

        // Cancel
        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // ── MANUAL GİRİŞ — ConfirmProduct-a keç ──────────────
    private fun showManualEntryDialog() {
        val editText = EditText(requireContext()).apply {
            hint      = "Barkodu daxil edin"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Manual Barkod Girişi")
            .setView(editText)
            .setPositiveButton("Davam et") { _, _ ->
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    // Birbaşa ConfirmProduct-a keç — API-yə ConfirmProduct özü müraciət edir
                    navigateToConfirm(input)
                }
            }
            .setNegativeButton("Ləğv et", null)
            .show()
    }

    // ── VİBRASİYA ─────────────────────────────────────────
    private fun vibrate() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.S) {
                val vm = requireContext().getSystemService(
                    android.os.VibratorManager::class.java
                )
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(android.os.Vibrator::class.java)
            }

            if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O) {
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

    private fun resetScanLine() {
        binding.scanLine.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_scan_line)
    }

    // ── BARCODE ANALYZER ─────────────────────────────────
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