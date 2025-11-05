package com.laundrypro.mymaidmanager.models

import com.google.gson.annotations.SerializedName

data class Maid(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val mobile: String,
    val address: String,
    val user: String,
    val tasks: List<Task>,
    val attendance: List<AttendanceRecord>,
    val notes: List<Note>
)

data class Task(
    @SerializedName("_id")
    val id: String?,
    val name: String,
    val price: Double,
    val frequency: String
)

data class AttendanceRecord(
    @SerializedName("_id")
    val id: String,
    val date: String,
    val taskName: String,
    val status: String
)

data class Note(
    @SerializedName("_id")
    val id: String,
    val date: String,
    val text: String
)

data class AddMaidRequest(
    val name: String,
    val mobile: String,
    val address: String
)

data class UpdateMaidRequest(
    val name: String,
    val mobile: String,
    val address: String
)

data class VerifyOtpRequest(
    val otp: String,
    val taskName: String
)

data class AddTaskRequest(
    val name: String,
    val price: Double,
    val frequency: String
)

data class UpdateTaskRequest(
    val name: String,
    val price: Double,
    val frequency: String
)

data class AddNoteRequest(
    val date: String,
    val text: String
)