package com.laundrypro.mymaidmanager.network

import com.laundrypro.mymaidmanager.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @GET("api/maids")
    suspend fun getMaids(): Response<List<Maid>>

    @GET("api/maids/{id}")
    suspend fun getMaidDetails(@Path("id") maidId: String): Response<Maid>

    @POST("api/maids")
    suspend fun addMaid(@Body addMaidRequest: AddMaidRequest): Response<Maid>

    @PUT("api/maids/{id}")
    suspend fun updateMaid(@Path("id") maidId: String, @Body updateMaidRequest: UpdateMaidRequest): Response<Maid>

    @DELETE("api/maids/{id}")
    suspend fun deleteMaid(@Path("id") maidId: String): Response<Unit>

    // New payroll endpoint
    @GET("api/maids/{maidId}/payroll")
    suspend fun getPayroll(@Path("maidId") maidId: String): Response<PayrollResponse>

    @POST("api/maids/{maidId}/tasks")
    suspend fun addTask(@Path("maidId") maidId: String, @Body addTaskRequest: AddTaskRequest): Response<List<Task>>

    @DELETE("api/maids/{maidId}/tasks/{taskId}")
    suspend fun deleteTask(@Path("maidId") maidId: String, @Path("taskId") taskId: String): Response<Unit>

    @POST("api/maids/request-otp/{maidId}")
    suspend fun requestOtp(@Path("maidId") maidId: String): Response<Unit>

    @POST("api/maids/verify-otp/{maidId}")
    suspend fun verifyOtp(@Path("maidId") maidId: String, @Body verifyOtpRequest: VerifyOtpRequest): Response<Unit>

    @POST("api/maids/{maidId}/attendance/manual")
    suspend fun addManualAttendance(@Path("maidId") maidId: String, @Body request: AddManualAttendanceRequest): Response<List<AttendanceRecord>>
}