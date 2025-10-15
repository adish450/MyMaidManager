package com.laundrypro.mymaidmanager.models

data class PayrollResponse(
    val totalSalary: Double,
    val totalDeductions: Double,
    val payableAmount: Double,
    val deductionsBreakdown: List<DeductionItem>,
    val billingCycle: BillingCycle
)

data class DeductionItem(
    val taskName: String,
    val missedDays: Int,
    val deductionAmount: Double
)

data class BillingCycle(
    val start: String,
    val end: String
)