package com.laundrypro.mymaidmanager.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.laundrypro.mymaidmanager.data.SessionManager
import com.laundrypro.mymaidmanager.models.LoginRequest
import com.laundrypro.mymaidmanager.models.RegisterRequest
import com.laundrypro.mymaidmanager.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Unknown : AuthState()
}

class AuthViewModel(application: Application) : ViewModel() {
    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val sessionManager = SessionManager(application)
    private val apiService = RetrofitClient.apiService

    companion object {
        var token: String? = null
            private set
    }

    init {
        viewModelScope.launch {
            val savedToken = sessionManager.fetchAuthToken()
            if (savedToken != null) {
                token = savedToken
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun registerUser(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            try {
                val response = apiService.registerUser(RegisterRequest(name, email, password))
                if (response.isSuccessful && response.body()?.token != null) {
                    val newToken = response.body()!!.token!!
                    token = newToken
                    sessionManager.saveAuthToken(newToken)
                    _authState.value = AuthState.Authenticated
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    _authResult.value = AuthResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            try {
                val response = apiService.loginUser(LoginRequest(email, password))
                if (response.isSuccessful && response.body()?.token != null) {
                    val newToken = response.body()!!.token!!
                    token = newToken
                    sessionManager.saveAuthToken(newToken)
                    _authState.value = AuthState.Authenticated
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Invalid credentials"
                    _authResult.value = AuthResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun logout() {
        token = null
        sessionManager.clearAuthToken()
        _authState.value = AuthState.Unauthenticated
    }

    fun resetAuthResult() {
        _authResult.value = AuthResult.Idle
    }
}

class AuthViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}