package com.laundrypro.mymaidmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

// Represents whether the user is logged in or out
sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Unknown : AuthState()
}


class AuthViewModel : ViewModel() {
    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()


    private val apiService = RetrofitClient.apiService

    // Companion object to hold the token statically, accessible from RetrofitClient
    companion object {
        var token: String? = null
            private set
    }

    init {
        // In a real app, you would check SharedPreferences for a saved token here
        _authState.value = AuthState.Unauthenticated
    }

    fun registerUser(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            try {
                val response = apiService.registerUser(RegisterRequest(name, email, password))
                if (response.isSuccessful && response.body()?.token != null) {
                    // Save the token and update the auth state to Authenticated
                    token = response.body()?.token
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
                    token = response.body()?.token
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
        _authState.value = AuthState.Unauthenticated
    }

    fun resetAuthResult() {
        _authResult.value = AuthResult.Idle
    }
}

