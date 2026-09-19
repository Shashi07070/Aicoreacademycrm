package com.example.data.model

enum class RevenueTimeRange(val label: String, val monthCount: Int) {
    LAST_6_MONTHS("Last 6 Months", 6),
    LAST_12_MONTHS("Last 12 Months", 12)
}

enum class ChartMetricMode(val label: String) {
    COMPLETED_FEES("Completed Fees", ),
    ALL_COLLECTIONS("All Collections"),
    COMPARATIVE("Comparison")
}

data class MonthlyRevenueItem(
    val yearMonth: String,            // e.g. "2026-09"
    val monthShort: String,           // e.g. "Sep"
    val monthFull: String,            // e.g. "September 2026"
    val completedRevenue: Double,     // Revenue specifically from students whose fee status is COMPLETED
    val totalRevenue: Double,         // Total payment collections in that month
    val completedStudentsCount: Int,  // Number of distinct completed students contributing in that month
    val totalPaymentsCount: Int,      // Total transactions in that month
    val isCurrentMonth: Boolean = false
)

data class MonthlyRevenueOverview(
    val items: List<MonthlyRevenueItem> = emptyList(),
    val totalCompletedRevenue: Double = 0.0,
    val totalAllRevenue: Double = 0.0,
    val averageMonthlyCompletedRevenue: Double = 0.0,
    val peakMonthItem: MonthlyRevenueItem? = null,
    val lowestMonthItem: MonthlyRevenueItem? = null,
    val currentMonthGrowthRate: Double = 0.0 // % change compared to previous month
)
