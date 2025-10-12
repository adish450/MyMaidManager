package com.laundrypro.mymaidmanager.network

import com.laundrypro.mymaidmanager.models.AuthResponse
import com.laundrypro.mymaidmanager.models.LoginRequest
import com.laundrypro.mymaidmanager.models.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<AuthResponse>

}

