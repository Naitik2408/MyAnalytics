package com.lifeanalytics.app.core.time

import android.os.SystemClock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

interface TimeProvider {
    fun nowEpochMillis(): Long
    fun elapsedRealtimeMillis(): Long = nowEpochMillis()
    fun currentZoneId(): ZoneId
    fun todayLocalDate(): LocalDate =
        Instant.ofEpochMilli(nowEpochMillis()).atZone(currentZoneId()).toLocalDate()
}

class SystemTimeProvider : TimeProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
    override fun currentZoneId(): ZoneId = ZoneId.systemDefault()
}

object DateAttributionPolicy {
    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val displayDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.US)
    private val shortDisplayDateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.US)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    /**
     * Authoritative attribution rule (docs/11_DATA_ARCHITECTURE.md Section 5.4):
     * A timestamped event (such as a Focus Session) is attributed to the local calendar
     * date in the timezone captured when the event started. Historical event instants
     * are never rewritten when the device timezone changes.
     */
    fun localDateForEvent(epochMillis: Long, zoneIdString: String?): LocalDate {
        val zone = try {
            if (!zoneIdString.isNullOrBlank()) ZoneId.of(zoneIdString) else ZoneId.systemDefault()
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }
        return Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    }

    fun formatIsoDate(date: LocalDate): String = date.format(isoDateFormatter)

    fun parseIsoDate(isoDate: String): LocalDate = LocalDate.parse(isoDate, isoDateFormatter)

    fun formatLocalTime(time: LocalTime): String = time.format(timeFormatter)

    fun parseLocalTimeOrNull(value: String?): LocalTime? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalTime.parse(value.trim(), timeFormatter)
        } catch (_: Exception) {
            null
        }
    }

    fun formatDisplayDate(date: LocalDate): String = date.format(displayDateFormatter)

    fun formatShortDate(date: LocalDate): String = date.format(shortDisplayDateFormatter)

    fun formatMonthYear(yearMonth: YearMonth): String = yearMonth.format(monthYearFormatter)

    fun formatDisplayTime(time: LocalTime): String = time.format(displayTimeFormatter)

    fun formatInstantTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalTime().format(displayTimeFormatter)
    }

    /**
     * Returns Monday..Sunday bounds for the week containing [anchorDate]
     * (docs/09_LIFE_ANALYTICS.md Section 6.1 & 6.3).
     */
    fun weekRangeForDate(anchorDate: LocalDate): Pair<LocalDate, LocalDate> {
        val start = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        return start to end
    }

    /**
     * Returns first..last day of the calendar month for [yearMonth].
     */
    fun monthRange(yearMonth: YearMonth): Pair<LocalDate, LocalDate> {
        return yearMonth.atDay(1) to yearMonth.atEndOfMonth()
    }

    /**
     * Formats focused duration according to docs/06_ACTIVE_WORK_FOCUS.md Section 11.3:
     * Under one hour: "25m" (or "0m" when zero, "< 1m" when 1..59s).
     * One hour or more: "1h 10m" (or "2h" when exact hour).
     */
    fun formatSummaryDuration(totalSeconds: Long): String {
        val clamped = totalSeconds.coerceAtLeast(0L)
        if (clamped == 0L) return "0m"
        val totalMinutes = clamped / 60L
        if (totalMinutes == 0L) return "< 1m"
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours == 0L) {
            "${minutes}m"
        } else if (minutes == 0L) {
            "${hours}h"
        } else {
            "${hours}h ${minutes}m"
        }
    }

    fun formatSummaryMinutes(totalMinutes: Int): String {
        return formatSummaryDuration(totalMinutes.coerceAtLeast(0).toLong() * 60L)
    }

    /**
     * Formats live timer HH:MM:SS with tabular two-digit components.
     */
    fun formatLiveTimer(totalSeconds: Long): String {
        val clamped = totalSeconds.coerceAtLeast(0L)
        val hours = clamped / 3600L
        val minutes = (clamped % 3600L) / 60L
        val seconds = clamped % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun todayLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        LocalDate.now(zoneId)

    fun greetingForHour(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello"
    }

    fun formatDurationSeconds(totalSeconds: Long): String = formatSummaryDuration(totalSeconds)

    fun formatMinutes(totalMinutes: Int): String = formatSummaryMinutes(totalMinutes)

    fun isoWeekRange(anchorDate: LocalDate): Pair<LocalDate, LocalDate> = weekRangeForDate(anchorDate)
}
