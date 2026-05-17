package com.hasanzade.hackathonmobile

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hasanzade.hackathonmobile.data.local.PhotoDataStore
import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import com.hasanzade.hackathonmobile.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoDataStore: PhotoDataStore
    private lateinit var tokenDataStore: TokenDataStore
    private var currentUserId = ""

    // Kamera üçün müvəqqəti URI
    private var cameraImageUri: Uri? = null

    // ── Galereya ──────────────────────────────────────────
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { saveAndShowPhoto(uriToBitmap(it)) }
        }

    // ── Kamera ────────────────────────────────────────────
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraImageUri?.let { uri ->
                    saveAndShowPhoto(uriToBitmap(uri))
                }
            }
        }

    // ── Kamera icazəsi ────────────────────────────────────
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoDataStore = PhotoDataStore(requireContext())
        tokenDataStore = TokenDataStore(requireContext())

        loadUserData()
        setupClickListeners()
    }

    private fun setupClickListeners() {

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Avatar və ya "Şəkli dəyiş" düyməsi — ikisi də eyni dialog açır
        val photoClickListener = View.OnClickListener { showPhotoPickerDialog() }
        binding.ivAvatarPhoto.setOnClickListener(photoClickListener)
        binding.tvAvatarInitials.setOnClickListener(photoClickListener)
        binding.tvCameraBadge.setOnClickListener(photoClickListener)
        binding.tvChangePhotoHint.setOnClickListener(photoClickListener)

        binding.btnSignOut.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                tokenDataStore.clear()
                if (isAdded) {
                    findNavController().navigate(
                        R.id.action_profileFragment_to_loginFragment
                    )
                }
            }
        }

        binding.bottomNav.selectedItemId = R.id.nav_profile
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    findNavController().navigate(
                        R.id.action_profileFragment_to_departmentDashboardFragment
                    )
                    true
                }
                R.id.nav_scanner -> {
                    findNavController().navigate(
                        R.id.action_profileFragment_to_scannerFragment
                    )
                    true
                }
                R.id.nav_profile -> true
                R.id.nav_alerts  -> true
                else             -> false
            }
        }
    }

    // ── İSTİFADƏÇİ MƏLUMATLARINı YÜKLƏ ─────────────────
    private fun loadUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            currentUserId           = tokenDataStore.getUserId() ?: ""
            val displayName         = tokenDataStore.getDisplayName() ?: "--"
            val filial              = tokenDataStore.getFilial() ?: "--"
            val department          = tokenDataStore.getDepartment() ?: ""
            val role                = tokenDataStore.getRole() ?: "--"

            binding.tvDisplayName.text  = displayName
            binding.tvFullName.text     = displayName
            binding.tvUserId.text       = currentUserId.ifEmpty { "--" }
            binding.tvFilial.text       = filial
            binding.tvDepartment.text   = department.ifEmpty { "Bütün şöbələr" }
            binding.tvRoleSubtitle.text = "${roleLabel(role)} · FreshGuard"
            binding.tvRoleBadge.text    = roleLabel(role)

            val initials = displayName
                .split(" ")
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
            binding.tvAvatarInitials.text = initials.ifEmpty { "??" }

            // Saxlanmış şəkli yüklə
            if (currentUserId.isNotEmpty()) {
                val bitmap = photoDataStore.loadPhoto(currentUserId)
                if (bitmap != null) {
                    showAvatarPhoto(bitmap)
                }
            }
        }
    }

    // ── FOTO SEÇİM DIALOGU ────────────────────────────────
    private fun showPhotoPickerDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Profil şəkli")
            .setItems(arrayOf("📷  Kameradan çək", "🖼️  Qalereyadan seç")) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    // ── KAMERA İCAZƏSİ ────────────────────────────────────
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> launchCamera()

            else -> cameraPermissionLauncher.launch(
                android.Manifest.permission.CAMERA
            )
        }
    }

    private fun launchCamera() {
        val photoFile = File(
            requireContext().cacheDir,
            "profile_photo_${System.currentTimeMillis()}.jpg"
        )
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        cameraLauncher.launch(cameraImageUri)
    }

    // ── BİTMAP ÇEVRƏ ─────────────────────────────────────
    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(
                    requireContext().contentResolver, uri
                )
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(
                    requireContext().contentResolver, uri
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("PROFILE", "uriToBitmap error", e)
            null
        }
    }

    // ── SAXLA VƏ GÖSTƏR ───────────────────────────────────
    private fun saveAndShowPhoto(bitmap: Bitmap?) {
        bitmap ?: return
        if (currentUserId.isEmpty()) return
        // SharedPreferences-ə yaz (userId açarı ilə — həmişəlik qalır)
        photoDataStore.savePhoto(currentUserId, bitmap)
        showAvatarPhoto(bitmap)
    }

    private fun showAvatarPhoto(bitmap: Bitmap) {
        binding.ivAvatarPhoto.setImageBitmap(bitmap)
        binding.ivAvatarPhoto.visibility    = View.VISIBLE
        binding.tvAvatarInitials.visibility = View.GONE
    }

    private fun roleLabel(role: String) = when (role) {
        "SUPER_ADMIN"      -> "Super Admin"
        "REGIONAL_MANAGER" -> "Regional Manager"
        "DEPARTMENT_HEAD"  -> "Department Head"
        else               -> role
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}