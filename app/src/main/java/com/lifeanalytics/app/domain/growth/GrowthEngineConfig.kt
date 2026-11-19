package com.lifeanalytics.app.domain.growth

/**
 * Centralized thresholds and minimum requirements for Growth Direction.
 * Reference: docs/14_GROWTH_ENGINE.md
 */
data class GrowthEngineConfig(
    // Delta thresholds in percentage points
    val dayDeltaThresholdPercent: Int = 15,
    val periodDeltaThresholdPercent: Int = 5,

    // Minimum completed local days in current period to unlock window
    val minCompletedDaysWeek: Int = 3,
    val minCompletedDaysMonth: Int = 7,
    val minCompletedDaysYear: Int = 30,

    // Minimum recorded days in previous period to unlock window
    val minRecordedDaysDay: Int = 1, // At least 1 recorded day on each of the 2 days
    val minRecordedDaysWeek: Int = 5,
    val minRecordedDaysMonth: Int = 14,
    val minRecordedDaysYear: Int = 90,

    // Tasks denominator minimums
    val minTasksDay: Int = 2,
    val minTasksWeek: Int = 4,
    val minTasksMonth: Int = 10,
    val minTasksYear: Int = 40
)
