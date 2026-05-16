package com.hasanzade.hackathonmobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import com.hasanzade.hackathonmobile.data.remote.AuthRepository
import com.hasanzade.hackathonmobile.data.remote.LoginResponse
import com.hasanzade.hackathonmobile.data.remote.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle    : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val response: LoginResponse) : LoginUiState()
    data class Error(val message: String)           : LoginUiState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenDataStore = TokenDataStore(application)
    private val authRepository = AuthRepository(tokenDataStore)

    private val _uiState = MutableLiveData<LoginUiState>(LoginUiState.Idle)
    val uiState: LiveData<LoginUiState> = _uiState

    fun login(userId: String, password: String) {
        if (userId.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("EMPTY_FIELDS")
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            val result = authRepository.login(userId.trim(), password)
            launch(Dispatchers.Main) {
                _uiState.value = when (result) {
                    is Result.Success -> LoginUiState.Success(result.data)
                    is Result.Error   -> LoginUiState.Error(result.message)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}