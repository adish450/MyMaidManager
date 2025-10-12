package com.laundrypro.mymaidmanager.models

import com.google.gson.annotations.SerializedName

// --- Request Bodies ---

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

// --- Response Body ---

data class AuthResponse(
    @SerializedName("token")
    val token: String?,
    @SerializedName("msg")
    val message: String?
)