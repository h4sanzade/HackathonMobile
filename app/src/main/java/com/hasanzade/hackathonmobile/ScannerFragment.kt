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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.hasanzade.hackathonmobile.databinding.FragmentScannerBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var camera: Camera? = null
    private var isFlashOn = false
    private var isScanning = true
    private var scanLineAnimator: ObjectAnimator? = null

    // Kamera icazəsi
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else {
                Toast.makeText(requireContext(), "Kamera icazəsi lazımdır", Toast.LENGTH_SHORT).show()
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
        checkCameraPermission()
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
                                onBarcodeDetected(barcode)
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
                android.util.Log.e("SCANNER", "Camera bind error", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // ── BARCODE DETECT ────────────────────────────────────
    private fun onBarcodeDetected(barcode: String) {
        // Scan line rəngini yaşıla çevir
        binding.scanLine.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.brand_primary)
        )

        // 3 box güncəllə
        binding.tvLastScan.text    = barcode
        binding.tvProductInfo.text = "Yüklənir..."
        binding.tvScanStatus.text  = "✓ Tapıldı"

        // Vibrate
        val vibrator = requireContext().getSystemService(android.os.Vibrator::class.java)
        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(100,
            android.os.VibrationEffect.DEFAULT_AMPLITUDE))

        Toast.makeText(requireContext(), "Scan edildi: $barcode", Toast.LENGTH_SHORT).show()

        // 2 saniyə sonra yenidən scan et
        binding.root.postDelayed({
            if (isAdded) {
                isScanning = true
                binding.tvScanStatus.text = "Gözlənir"
                binding.scanLine.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_scan_line)
            }
        }, 2000)
    }

    // ── SCAN LINE ANİMASİYA ───────────────────────────────
    private fun startScanLineAnimation() {
        binding.scanBox.post {
            val boxHeight = binding.scanBox.height.toFloat()

            scanLineAnimator = ObjectAnimator.ofFloat(
                binding.scanLine, "translationY", 0f, boxHeight - 4f
            ).apply {
                duration          = 1800
                repeatCount       = ValueAnimator.INFINITE
                repeatMode        = ValueAnimator.REVERSE
                interpolator      = LinearInterpolator()
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

    // ── MANUAL GİRİŞ DİALOQ ──────────────────────────────
    private fun showManualEntryDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Barkodu daxil edin"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Manual Barkod Girişi")
            .setView(editText)
            .setPositiveButton("Axtar") { _, _ ->
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    onBarcodeDetected(input)
                }
            }
            .setNegativeButton("Ləğv et", null)
            .show()
    }

    // ── BARCODE ANALYZER ─────────────────────────────────
    inner class BarcodeAnalyzer(
        private val onResult: (String) -> Unit
    ) : ImageAnalysis.Analyzer {

        private val scanner = BarcodeScanning.getClient()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return
            }

            val image = InputImage.fromMediaImage(
                mediaImage, imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { onResult(it) }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scanLineAnimator?.cancel()
        cameraExecutor.shutdown()
        _binding = null
    }
}