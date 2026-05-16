package com.hasanzade.hackathonmobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hasanzade.hackathonmobile.databinding.FragmentLoginBinding
import com.hasanzade.hackathonmobile.ui.LoginUiState
import com.hasanzade.hackathonmobile.ui.LoginViewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        observeViewModel()
        animateEntrance()
    }

    private fun setupUi() {
        // Clear errors on typing
        binding.etUserId.doAfterTextChanged {
            binding.tilUserId.error = null
            binding.tvError.visibility = View.GONE
        }
        binding.etPassword.doAfterTextChanged {
            binding.tilPassword.error = null
            binding.tvError.visibility = View.GONE
        }

        // Keyboard "Done" triggers login
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                triggerLogin()
                true
            } else false
        }

        binding.btnLogin.setOnClickListener {
            hideKeyboard()
            triggerLogin()
        }

    }

    private fun triggerLogin() {
        val userId = binding.etUserId.text?.toString().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        viewModel.login(userId, password)
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginUiState.Idle -> setLoadingState(false)

                is LoginUiState.Loading -> setLoadingState(true)

                is LoginUiState.Success -> {
                    setLoadingState(false)
                    // Navigate to home, clearing back stack so user can't go back to login
                    findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                }

                is LoginUiState.Error -> {
                    setLoadingState(false)
                    when {
                        state.message == "EMPTY_FIELDS" -> {
                            val msg = getString(R.string.error_empty_fields)
                            showInlineError(msg)
                            if (binding.etUserId.text.isNullOrBlank()) {
                                binding.tilUserId.error = " "
                            }
                            if (binding.etPassword.text.isNullOrBlank()) {
                                binding.tilPassword.error = " "
                            }
                        }
                        else -> showInlineError(getString(R.string.error_login_failed))
                    }
                    shakeCard()
                }
            }
        }
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.etUserId.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        binding.btnLogin.text = if (loading) "" else getString(R.string.btn_login)
        binding.progressLogin.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showInlineError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun shakeCard() {
        val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
        binding.cardForm.startAnimation(shake)
    }

    private fun animateEntrance() {
        val views = listOf(
            binding.tvWelcome,
            binding.tvSubtitle,
            binding.dividerLine,
            binding.cardForm,
        )
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 30f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay((index * 80).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}