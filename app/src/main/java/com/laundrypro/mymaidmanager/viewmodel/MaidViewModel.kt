package com.laundrypro.mymaidmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laundrypro.mymaidmanager.models.*
import com.laundrypro.mymaidmanager.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MaidListUIState {
    data object Loading : MaidListUIState()
    data class Success(val maids: List<Maid>) : MaidListUIState()
    data class Error(val message: String) : MaidListUIState()
}

sealed class MaidDetailUIState {
    data object Loading : MaidDetailUIState()
    data class Success(val maid: Maid) : MaidDetailUIState()
    data class Error(val message: String) : MaidDetailUIState()
}

sealed class OtpState {
    data object Idle : OtpState()
    data object OtpRequested : OtpState()
    data object Loading : OtpState()
    data class Error(val message: String) : OtpState()
}

sealed class PayrollUIState {
    data object Loading : PayrollUIState()
    data class Success(val payroll: PayrollResponse) : PayrollUIState()
    data class Error(val message: String) : PayrollUIState()
}

class MaidViewModel : ViewModel() {
    private val _maidListUIState = MutableStateFlow<MaidListUIState>(MaidListUIState.Loading)
    val maidListUIState: StateFlow<MaidListUIState> = _maidListUIState.asStateFlow()

    private val _maidDetailUIState = MutableStateFlow<MaidDetailUIState>(MaidDetailUIState.Loading)
    val maidDetailUIState: StateFlow<MaidDetailUIState> = _maidDetailUIState.asStateFlow()

    private val _otpState = MutableStateFlow<OtpState>(OtpState.Idle)
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

    private val _payrollUIState = MutableStateFlow<PayrollUIState>(PayrollUIState.Loading)
    val payrollUIState: StateFlow<PayrollUIState> = _payrollUIState.asStateFlow()

    private val apiService = RetrofitClient.apiService

    fun fetchMaids() {
        viewModelScope.launch {
            _maidListUIState.value = MaidListUIState.Loading
            try {
                val response = apiService.getMaids()
                if (response.isSuccessful && response.body() != null) {
                    _maidListUIState.value = MaidListUIState.Success(response.body()!!)
                } else {
                    _maidListUIState.value = MaidListUIState.Error("Failed to fetch maids")
                }
            } catch (e: Exception) {
                _maidListUIState.value = MaidListUIState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // --- THIS FUNCTION IS UPDATED ---
    fun addMaid(name: String, mobile: String, address: String) {
        viewModelScope.launch {
            try {
                val response = apiService.addMaid(AddMaidRequest(name, mobile, address))
                if (response.isSuccessful) {
                    // THE FIX: After a successful add, refresh the list.
                    fetchMaids()
                } else {
                    // Optionally, handle the error case, e.g., show a toast
                    _maidListUIState.value = MaidListUIState.Error("Failed to add maid. Please try again.")
                }
            } catch (e: Exception) {
                _maidListUIState.value = MaidListUIState.Error(e.message ?: "An error occurred while adding maid")
            }
        }
    }

    fun fetchMaidDetails(maidId: String) {
        viewModelScope.launch {
            _maidDetailUIState.value = MaidDetailUIState.Loading
            try {
                val response = apiService.getMaidDetails(maidId)
                if (response.isSuccessful && response.body() != null) {
                    _maidDetailUIState.value = MaidDetailUIState.Success(response.body()!!)
                } else {
                    _maidDetailUIState.value = MaidDetailUIState.Error("Failed to load details")
                }
            } catch (e: Exception) {
                _maidDetailUIState.value = MaidDetailUIState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun addTask(maidId: String, name: String, price: Double, frequency: String) {
        viewModelScope.launch {
            try {
                apiService.addTask(maidId, AddTaskRequest(name, price, frequency))
                fetchMaidDetails(maidId)
            } catch (e: Exception) {
                // You can add error handling for the detail screen here
            }
        }
    }

    fun deleteTask(maidId: String, taskId: String) {
        viewModelScope.launch {
            try {
                apiService.deleteTask(maidId, taskId)
                fetchMaidDetails(maidId)
            } catch (e: Exception) {
                // You can add error handling for the detail screen here
            }
        }
    }

    fun fetchPayroll(maidId: String) {
        viewModelScope.launch {
            _payrollUIState.value = PayrollUIState.Loading
            try {
                val response = apiService.getPayroll(maidId)
                if (response.isSuccessful && response.body() != null) {
                    _payrollUIState.value = PayrollUIState.Success(response.body()!!)
                } else {
                    _payrollUIState.value = PayrollUIState.Error("Failed to calculate payroll")
                }
            } catch (e: Exception) {
                _payrollUIState.value = PayrollUIState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun requestOtpForAttendance(maidId: String) {
        viewModelScope.launch {
            _otpState.value = OtpState.Loading
            try {
                val response = apiService.requestOtp(maidId)
                if (response.isSuccessful) {
                    _otpState.value = OtpState.OtpRequested
                } else {
                    _otpState.value = OtpState.Error("Failed to request OTP")
                }
            } catch (e: Exception) {
                _otpState.value = OtpState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun verifyOtpForAttendance(maidId: String, otp: String, taskName: String) {
        viewModelScope.launch {
            _otpState.value = OtpState.Loading
            try {
                val response = apiService.verifyOtp(maidId, VerifyOtpRequest(otp, taskName))
                if (response.isSuccessful) {
                    _otpState.value = OtpState.Idle
                    fetchMaidDetails(maidId)
                } else {
                    val errorMsg = response.errorBody()?.string()?.let {
                        com.google.gson.JsonParser().parse(it).asJsonObject.get("msg").asString
                    } ?: "Invalid OTP"
                    _otpState.value = OtpState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _otpState.value = OtpState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun resetOtpState() {
        _otpState.value = OtpState.Idle
    }
}