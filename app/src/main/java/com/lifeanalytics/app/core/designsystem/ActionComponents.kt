package com.lifeanalytics.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared quick-action pill used on Home, Health, and Career.
 *
 * Matches the approved reference layout: a 56 dp tall fully-rounded pill with a leading
 * `+` icon and a semibold label. `primary = true` renders the ink-filled variant
 * (single full-width hero action); otherwise the white surface variant is used.
 */
object LaActionPillDefaults {
    val Height: Dp = 56.dp
    val Gap: Dp = 10.dp
    val IconSize: Dp = 20.dp

    /** Caps action groups on wide screens so pills never stretch into thin bars. */
    val MaxGroupWidth: Dp = 560.dp
}

@Composable
fun LaActionPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    icon: ImageVector = Icons.Outlined.Add
) {
    val colors = LaTheme.colors
    val container: Color = if (primary) colors.ink else colors.surface
    val content: Color = if (primary) colors.onInk else colors.textPrimary

    Row(
        modifier = modifier
            .heightIn(min = LaActionPillDefaults.Height)
            .clip(CircleShape)
            .background(container)
            .pressScaleClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(LaActionPillDefaults.IconSize)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = LaTheme.typography.buttonLabel.copy(fontWeight = FontWeight.SemiBold),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

data class LaActionSpec(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector = Icons.Outlined.Add
)

/**
 * Reference-image arrangement: optional full-width primary pill on top, then the
 * remaining actions as equal-width white pills in a single row below.
 * The whole group is width-capped and centred on wide (tablet/desktop) windows.
 */
@Composable
fun LaActionPillGroup(
    secondary: List<LaActionSpec>,
    modifier: Modifier = Modifier,
    primary: LaActionSpec? = null
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = LaActionPillDefaults.MaxGroupWidth)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LaActionPillDefaults.Gap)
        ) {
            if (primary != null) {
                LaActionPillButton(
                    text = primary.label,
                    onClick = primary.onClick,
                    icon = primary.icon,
                    primary = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (secondary.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LaActionPillDefaults.Gap)
                ) {
                    secondary.forEach { spec ->
                        LaActionPillButton(
                            text = spec.label,
                            onClick = spec.onClick,
                            icon = spec.icon,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared "completed / total" summary card used by the Health and Career pages.
 * Built on [LaStatTile] so it shares the Finance page's Total Income / Total Expenses
 * card anatomy (radius, padding, label → big value hierarchy).
 *
 * A zero denominator is shown as "No data" (never as 0), per metric-integrity rules.
 */
@Composable
fun LaCompletionCountTile(
    label: String,
    completed: Int,
    total: Int,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    val hasData = total > 0
    LaStatTile(
        label = label,
        value = if (hasData) "$completed / $total" else "No data",
        icon = icon,
        containerColor = containerColor,
        isEmptyValue = !hasData,
        minHeight = 140.dp,
        modifier = modifier
    )
}

/**
 * Shared Week / Month period selector used by the Health and Career screens so both
 * follow the exact same date-filtering UX as Finance and Insights.
 */
@Composable
fun LaCategoryPeriodSelector(
    window: com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow,
    anchorDate: java.time.LocalDate,
    today: java.time.LocalDate,
    onWindowChange: (com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow) -> Unit,
    onAnchorChange: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow.WEEKLY,
        com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow.MONTHLY
    )
    val (periodStart, periodEnd) = when (window) {
        com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow.WEEKLY ->
            com.lifeanalytics.app.core.time.DateAttributionPolicy.isoWeekRange(anchorDate)
        else ->
            com.lifeanalytics.app.core.time.DateAttributionPolicy.monthRange(java.time.YearMonth.from(anchorDate))
    }
    val isCurrentPeriod = !today.isBefore(periodStart) && !today.isAfter(periodEnd)
    val rangeTitle = when (window) {
        com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow.WEEKLY ->
            "${com.lifeanalytics.app.core.time.DateAttributionPolicy.formatShortDate(periodStart)} – ${com.lifeanalytics.app.core.time.DateAttributionPolicy.formatShortDate(periodEnd)}"
        else ->
            com.lifeanalytics.app.core.time.DateAttributionPolicy.formatMonthYear(java.time.YearMonth.from(periodStart))
    }
    val badgeText = when (window) {
        com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow.WEEKLY ->
            if (isCurrentPeriod) "This week" else "Selected week"
        else ->
            if (isCurrentPeriod) "This month" else "Selected month"
    }
    val resetLabel = when (window) {
        com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow.WEEKLY -> "This week"
        else -> "This month"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LaSegmentedControl(
            options = options,
            selectedOption = window,
            onOptionSelected = onWindowChange,
            labelProvider = { it.label },
            modifier = Modifier.fillMaxWidth()
        )
        LaDateRangePill(
            title = rangeTitle,
            badgeText = badgeText,
            onPrevious = {
                val prev = when (window) {
                    com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow.WEEKLY -> anchorDate.minusWeeks(1)
                    else -> anchorDate.minusMonths(1)
                }
                onAnchorChange(prev)
            },
            onNext = {
                val next = when (window) {
                    com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow.WEEKLY -> anchorDate.plusWeeks(1)
                    else -> anchorDate.plusMonths(1)
                }
                onAnchorChange(next)
            },
            onResetToCurrent = if (!isCurrentPeriod) {
                { onAnchorChange(today) }
            } else null,
            resetLabel = resetLabel
        )
    }
}

/**
 * Reusable Material 3 DatePickerDialog styled to match Life Analytics.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LaDatePickerDialog(
    initialDate: java.time.LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (java.time.LocalDate) -> Unit
) {
    val initialMillis = initialDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            LaTextButton(
                text = "Select",
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        onConfirm(picked)
                    } else {
                        onDismiss()
                    }
                }
            )
        },
        dismissButton = {
            LaTextButton(text = "Cancel", onClick = onDismiss, color = LaTheme.colors.textSecondary)
        }
    ) {
        androidx.compose.material3.DatePicker(state = state)
    }
}

/**
 * Reusable Material 3 TimePicker dialog styled to match Life Analytics.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LaTimePickerDialog(
    initialTime: java.time.LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (java.time.LocalTime) -> Unit
) {
    val fallback = initialTime ?: java.time.LocalTime.of(9, 0)
    val state = androidx.compose.material3.rememberTimePickerState(
        initialHour = fallback.hour,
        initialMinute = fallback.minute,
        is24Hour = true
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = LaTheme.radii.shapeLg,
        containerColor = LaTheme.colors.surface,
        title = {
            Text(
                text = "Select time",
                style = LaTheme.typography.sectionTitle,
                color = LaTheme.colors.textPrimary
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.TimePicker(state = state)
            }
        },
        confirmButton = {
            LaTextButton(
                text = "Set time",
                onClick = { onConfirm(java.time.LocalTime.of(state.hour, state.minute)) }
            )
        },
        dismissButton = {
            LaTextButton(text = "Cancel", onClick = onDismiss, color = LaTheme.colors.textSecondary)
        }
    )
}

