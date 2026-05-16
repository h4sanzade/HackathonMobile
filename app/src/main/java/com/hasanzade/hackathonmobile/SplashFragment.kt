package com.hasanzade.hackathonmobile

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tokenDataStore: TokenDataStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenDataStore = TokenDataStore(requireContext())

        animateLogo(view)
        checkSessionAfterDelay()
    }

    private fun animateLogo(view: View) {
        val logo = view.findViewById<ImageView>(R.id.iv_splash_logo)
        logo.alpha = 0f
        logo.scaleX = 0.8f
        logo.scaleY = 0.8f
        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun checkSessionAfterDelay() {
        handler.postDelayed({
            if (!isAdded) return@postDelayed

            lifecycleScope.launch {
                val isLoggedIn = tokenDataStore.isLoggedIn()
                val role       = tokenDataStore.getRole()

                if (!isAdded) return@launch

                if (isLoggedIn && role != null) {
                    val action = when (role) {
                        "REGIONAL_MANAGER", "SUPER_ADMIN" ->
                            R.id.action_splashFragment_to_departmentDashboardFragment  // Admin → Barkod ekranı
                        else ->
                            R.id.action_splashFragment_to_regionalDashboardFragment    // Müdür → Analitika ekranı
                    }
                    findNavController().navigate(action)
                }else {
                    // Token yoxdur — login-ə keç
                    findNavController().navigate(
                        R.id.action_splashFragment_to_loginFragment
                    )
                }
            }
        }, 3000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}