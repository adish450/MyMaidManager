package com.laundrypro.mymaidmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
// Adjust import path
import com.laundrypro.mymaidmanager.models.*
import com.laundrypro.mymaidmanager.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- Maid Management Section ---
sealed class MaidListUIState {
    object Loading : MaidListUIState()
    data class Success(val maids: List<Maid>) : MaidListUIState()
    data class Error(val message: String) : MaidListUIState()
}
sealed class MaidDetailUIState {
    object Loading : MaidDetailUIState()
    data class Success(val maid: Maid) : MaidDetailUIState()
    data class Error(val message: String) : MaidDetailUIState()
}
sealed class OtpState {
    object Idle : OtpState()
    object OtpRequested : OtpState()
    object Loading : OtpState()
    data class Error(val message: String) : OtpState()
}
sealed class PayrollUIState {
    object Loading : PayrollUIState()
    data class Success(val payroll: PayrollResponse) : PayrollUIState()
    data class Error(val message: String) : PayrollUIState()
}
sealed class ManualAttendanceState {
    object Idle : ManualAttendanceState()
    object Loading : ManualAttendanceState()
    object Success : ManualAttendanceState()
    data class Error(val message: String) : ManualAttendanceState()
}

sealed class MaidDeleteState {
    object Idle : MaidDeleteState()
    object Loading : MaidDeleteState()
    object Success : MaidDeleteState()
    data class Error(val message: String) : MaidDeleteState()
}


// --- MaidViewModel ---
class MaidViewModel : ViewModel() {
    private val _maidListUIState = MutableStateFlow<MaidListUIState>(MaidListUIState.Loading)
    val maidListUIState: StateFlow<MaidListUIState> = _maidListUIState.asStateFlow()

    private val _maidDetailUIState = MutableStateFlow<MaidDetailUIState>(MaidDetailUIState.Loading)
    val maidDetailUIState: StateFlow<MaidDetailUIState> = _maidDetailUIState.asStateFlow()

    private val _otpState = MutableStateFlow<OtpState>(OtpState.Idle)
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

    private val _payrollUIState = MutableStateFlow<PayrollUIState>(PayrollUIState.Loading)
    val payrollUIState: StateFlow<PayrollUIState> = _payrollUIState.asStateFlow()

    private val _manualAttendanceState = MutableStateFlow<ManualAttendanceState>(ManualAttendanceState.Idle)
    val manualAttendanceState: StateFlow<ManualAttendanceState> = _manualAttendanceState.asStateFlow()

    private val _deleteState = MutableStateFlow<MaidDeleteState>(MaidDeleteState.Idle)
    val deleteState: StateFlow<MaidDeleteState> = _deleteState.asStateFlow()

    private val apiService = RetrofitClient.apiService

    fun fetchMaids() {
        viewModelScope.launch {
            // --- FIX: Always set to Loading to ensure refresh ---
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

    fun addMaid(name: String, mobile: String, address: String) {
        viewModelScope.launch {
            try {
                val response = apiService.addMaid(AddMaidRequest(name, mobile, address))
                if (response.isSuccessful) {
                    fetchMaids() // Refresh the list
                } else {
                    // This error is not shown, but good practice
                    _maidListUIState.value = MaidListUIState.Error("Failed to add maid. Please try again.")
                }
            } catch (e: Exception) {
                _maidListUIState.value = MaidListUIState.Error(e.message ?: "An error occurred while adding maid")
            }
        }
    }

    fun fetchMaidDetails(maidId: String) {
        viewModelScope.launch {
            // Only show full loading on initial load or retry from error
            if (_maidDetailUIState.value !is MaidDetailUIState.Success) {
                _maidDetailUIState.value = MaidDetailUIState.Loading
            }
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
                // Pass the request body
                val response = apiService.addTask(maidId, AddTaskRequest(name, price, frequency))
                if (response.isSuccessful && response.body() != null) {
                    // Update the detail screen with the new maid object from response
                    _maidDetailUIState.value = MaidDetailUIState.Success(response.body()!!)
                    fetchPayroll(maidId) // Refresh payroll after adding task
                    fetchMaids() // --- FIX: Refresh main list ---
                } else {
                    // Handle error
                }
            } catch (e: Exception) {
                // You can add error handling for the detail screen here
            }
        }
    }

    // --- NEW: updateTask function ---
    fun updateTask(maidId: String, taskId: String, name: String, price: Double, frequency: String) {
        viewModelScope.launch {
            try {
                val response = apiService.updateTask(maidId, taskId, UpdateTaskRequest(name, price, frequency))
                if (response.isSuccessful && response.body() != null) {
                    // Update the detail screen with the new maid object from response
                    _maidDetailUIState.value = MaidDetailUIState.Success(response.body()!!)
                    fetchPayroll(maidId) // Refresh payroll after updating task
                    fetchMaids() // --- FIX: Refresh main list ---
                } else {
                    // Handle error (e.g., show a toast)
                }
            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

    fun deleteTask(maidId: String, taskId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteTask(maidId, taskId)
                if (response.isSuccessful && response.body() != null) {
                    // Update the detail screen with the new maid object from response
                    _maidDetailUIState.value = MaidDetailUIState.Success(response.body()!!)
                    fetchPayroll(maidId) // Refresh payroll after deleting task
                    fetchMaids() // --- FIX: Refresh main list ---
                } else {
                    // Handle error
                }
            } catch (e: Exception) {
                // You can add error handling for the detail screen here
            }
        }
    }

    fun fetchPayroll(maidId: String) {
        viewModelScope.launch {
            // Only show full loading on initial load or retry from error
            if (_payrollUIState.value !is PayrollUIState.Success) {
                _payrollUIState.value = PayrollUIState.Loading
            }
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

    fun addManualAttendance(maidId: String, date: String, taskName: String, status: String) {
        viewModelScope.launch {
            _manualAttendanceState.value = ManualAttendanceState.Loading // Set loading state
            try {
                val response = apiService.addManualAttendance(maidId, AddManualAttendanceRequest(date, taskName, status))
                if (response.isSuccessful) {
                    // Refresh data in the background (this will no longer cause a flicker)
                    fetchMaidDetails(maidId)
                    fetchPayroll(maidId)
                    _manualAttendanceState.value = ManualAttendanceState.Success // Set success state
                } else {
                    val errorMsg = response.errorBody()?.string()?.let {
                        try {
                            JsonParser().parse(it).asJsonObject.get("msg").asString
                        } catch (e: Exception) { "Failed to add record" }
                    } ?: "Failed to add record"
                    _manualAttendanceState.value = ManualAttendanceState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _manualAttendanceState.value = ManualAttendanceState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun resetManualAttendanceState() {
        _manualAttendanceState.value = ManualAttendanceState.Idle
    }

    fun updateMaid(maidId: String, name: String, mobile: String, address: String) {
        viewModelScope.launch {
            // Don't set detail state to loading, to avoid flicker
            // _maidDetailUIState.value = MaidDetailUIState.Loading
            try {
                val response = apiService.updateMaid(maidId, UpdateMaidRequest(name, mobile, address))
                if (response.isSuccessful && response.body() != null) {
                    _maidDetailUIState.value = MaidDetailUIState.Success(response.body()!!)
                    fetchMaids() // --- FIX: Refresh main list ---
                } else {
                    _maidDetailUIState.value = MaidDetailUIState.Error("Failed to update maid")
                }
            } catch (e: Exception) {
                _maidDetailUIState.value = MaidDetailUIState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun deleteMaid(maidId: String) {
        viewModelScope.launch {
            _deleteState.value = MaidDeleteState.Loading
            try {
                val response = apiService.deleteMaid(maidId)
                if (response.isSuccessful) {
                    _deleteState.value = MaidDeleteState.Success
                    fetchMaids() // --- FIX: Refresh main list ---
                } else {
                    _deleteState.value = MaidDeleteState.Error("Failed to delete maid")
                }
            } catch (e: Exception) {
                _deleteState.value = MaidDeleteState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = MaidDeleteState.Idle
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
                    fetchMaidDetails(maidId) // Refresh details
                    fetchPayroll(maidId) // Refresh payroll
                } else {
                    val errorMsg = response.errorBody()?.string()?.let {
                        try {
                            JsonParser().parse(it).asJsonObject.get("msg").asString
                        } catch (e: Exception) { "Invalid OTP" }
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