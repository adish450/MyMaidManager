package com.laundrypro.mymaidmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laundrypro.mymaidmanager.models.LoginRequest
import com.laundrypro.mymaidmanager.models.RegisterRequest
import com.laundrypro.mymaidmanager.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthViewModel : ViewModel() {
    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult

    private val apiService = RetrofitClient.apiService

    fun registerUser(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            try {
                val response = apiService.registerUser(RegisterRequest(name, email, password))
                if (response.isSuccessful && response.body()?.token != null) {
                    _authResult.value = AuthResult.Success
                    // TODO: Save the token
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
                    _authResult.value = AuthResult.Success
                    // TODO: Save the token
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Invalid credentials"
                    _authResult.value = AuthResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authResult.value = AuthResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun resetAuthState() {
        _authResult.value = AuthResult.Idle
    }
}