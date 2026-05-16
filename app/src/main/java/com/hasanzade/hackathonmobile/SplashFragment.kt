package com.yourpackage

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hasanzade.hackathonmobile.R

class SplashFragment : Fragment() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Logo animasiyası
        val logo = view.findViewById<ImageView>(R.id.iv_splash_logo)
        logo.alpha = 0f
        logo.scaleX = 0.85f
        logo.scaleY = 0.85f
        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(700)
            .start()

        // 10 saniyə sonra Login-ə keç
        handler.postDelayed({
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }, 10_000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Fragment destroy olanda handler-i təmizlə (memory leak)
        handler.removeCallbacksAndMessages(null)
    }
}