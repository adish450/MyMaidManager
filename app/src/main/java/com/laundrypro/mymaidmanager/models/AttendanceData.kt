package com.laundrypro.mymaidmanager.models

// Data class for the manual attendance request body
data class AddManualAttendanceRequest(
    val date: String, // Format: "YYYY-MM-DD"
    val taskName: String,
    val status: String // e.g., "Absent"
)