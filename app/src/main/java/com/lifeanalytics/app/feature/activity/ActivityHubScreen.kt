package com.lifeanalytics.app.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.designsystem.LaArrowButton
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaHeroPanel
import com.lifeanalytics.app.core.designsystem.LaIconChip
import com.lifeanalytics.app.core.designsystem.LaPrimaryButton
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaSessionTimeline
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.LaTimelineSegment
import com.lifeanalytics.app.core.designsystem.LaTintedTile
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.ReviewStatus
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

@Composable
fun ActivityHubScreen(
    repository: LifeAnalyticsRepository,
    onOpenPlanning: (LocalDate) -> Unit,
    onOpenFocus: () -> Unit,
    onOpenHabits: (LocalDate) -> Unit,
    onOpenReview: (LocalDate) -> Unit
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }

    val todayTasks by remember(today) {
        repository.observeTasksForDate(today)
    }.collectAsState(initial = emptyList())

    val todaySessions by remember(today) {
        repository.observeSessionsForDate(today)
    }.collectAsState(initial = emptyList())

    val unresolvedSession by repository.observeUnresolvedSession().collectAsState(initial = null)

    val allHabits by repository.observeAllHabits().collectAsState(initial = emptyList())
    val todayCompletions by remember(today) {
        repository.observeHabitCompletionsForDate(today)
    }.collectAsState(initial = emptyList())

    val todayReview by remember(today) {
        repository.observeReviewForDate(today)
    }.collectAsState(initial = null)

    val taskSummary = remember(todayTasks) {
        DomainCalculations.calculateTaskSummary(todayTasks)
    }
    val completedFocusSeconds = remember(todaySessions) {
        DomainCalculations.sumCompletedFocusDurationSeconds(todaySessions)
    }
    val completedFocusCount = remember(todaySessions) {
        todaySessions.count { it.status == FocusSessionStatus.COMPLETED }
    }
    val habitSummary = remember(allHabits, todayCompletions, today) {
        DomainCalculations.evaluateHabitsForDate(allHabits, todayCompletions, today)
    }

    // Live tick for running session hero
    var liveNowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(unresolvedSession?.id, unresolvedSession?.status) {
        if (unresolvedSession?.status == FocusSessionStatus.ACTIVE) {
            while (true) {
                liveNowMillis = System.currentTimeMillis()
                delay(1_000L)
            }
        } else {
            liveNowMillis = System.currentTimeMillis()
        }
    }

    // Build real timeline segments for today's completed focus sessions
    val timelineSegments = remember(todaySessions) {
        todaySessions
            .filter { it.status == FocusSessionStatus.COMPLETED }
            .mapIndexed { idx, session ->
                val zone = runCatching { ZoneId.of(session.timezoneId) }.getOrDefault(ZoneId.systemDefault())
                val startTime = Instant.ofEpochMilli(session.startedAtEpochMillis).atZone(zone).toLocalTime()
                val endEpoch = session.endedAtEpochMillis
                    ?: (session.startedAtEpochMillis + session.focusedDurationSeconds * 1000L)
                val endTime = Instant.ofEpochMilli(endEpoch).atZone(zone).toLocalTime()
                val startHr = startTime.hour + (startTime.minute / 60f)
                val rawEndHr = endTime.hour + (endTime.minute / 60f)
                val endHr = if (rawEndHr <= startHr) (startHr + 0.35f).coerceAtMost(24f) else rawEndHr
                LaTimelineSegment(
                    startHourFraction = startHr,
                    endHourFraction = endHr,
                    label = session.displayTitle,
                    isHighlighted = idx == 0
                )
            }
    }

    val colors = LaTheme.colors
    val spacing = LaTheme.spacing
    val fontScale = LocalDensity.current.fontScale

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentPadding = PaddingValues(
            start = spacing.screenInset,
            end = spacing.screenInset,
            top = spacing.space2,
            bottom = spacing.dockBottomClearance
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        // 1. Concise Header
        item {
            LaScreenHeader(
                title = "Activity",
                subtitle = "Plan tasks, run focused sessions, track habits, and review your day."
            )
        }

        // 2. Active Focus Running Hero (if a session is currently running/paused/interrupted)
        if (unresolvedSession != null) {
            val active = unresolvedSession!!
            item {
                val effectiveEnd = if (active.status == FocusSessionStatus.PAUSED) {
                    active.pauses.lastOrNull { it.resumedAtEpochMillis == null }?.pausedAtEpochMillis
                        ?: liveNowMillis
                } else {
                    maxOf(liveNowMillis, active.startedAtEpochMillis)
                }
                val liveSeconds = DomainCalculations.calculateFocusedDurationSeconds(
                    startedAtEpochMillis = active.startedAtEpochMillis,
                    endOrNowEpochMillis = effectiveEnd,
                    pauses = active.pauses
                )
                LaHeroPanel(
                    title = when (active.status) {
                        FocusSessionStatus.ACTIVE -> "Focus session running"
                        FocusSessionStatus.PAUSED -> "Focus session paused"
                        FocusSessionStatus.INTERRUPTED -> "Session interrupted — review needed"
                        else -> "Focus session"
                    },
                    subtitle = active.displayTitle,
                    icon = Icons.Outlined.Timer,
                    containerColor = colors.tints.mint.strong,
                    onActionClick = onOpenFocus,
                    actionContentDescription = "Open Focus Timer"
                ) {
                    Text(
                        text = DateAttributionPolicy.formatLiveTimer(liveSeconds),
                        style = LaTheme.typography.timerValue,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LaPrimaryButton(
                        text = "Return to timer",
                        icon = Icons.Outlined.PlayArrow,
                        onClick = onOpenFocus,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. Bento of 4 Module Tiles (Section 4.4 Tint Map; Focus Tile is Largest)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackColumns = maxWidth < 340.dp || fontScale >= 1.35f

                Column(verticalArrangement = Arrangement.spacedBy(spacing.tileGutter)) {
                    // Largest Bento Tile: Active Work & Focus (tint.mint)
                    LaTintedTile(
                        containerColor = colors.tints.mint.base,
                        shape = LaTheme.radii.shapeLg,
                        padding = spacing.heroPadding,
                        onClick = onOpenFocus,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LaIconChip(
                                icon = Icons.Outlined.Timer,
                                onTintedSurface = true
                            )
                            LaArrowButton(
                                contentDescription = "Open Focus Timer",
                                onClick = onOpenFocus,
                                onTintedSurface = true
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Active Work & Focus",
                            style = LaTheme.typography.sectionTitle,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DateAttributionPolicy.formatDurationSeconds(completedFocusSeconds),
                            style = LaTheme.typography.metricValue,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusPillRow(
                            text = if (completedFocusCount > 0) {
                                "$completedFocusCount session(s) completed today"
                            } else {
                                "No focus sessions recorded today"
                            },
                            isCompleted = completedFocusCount > 0
                        )
                        if (unresolvedSession == null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            LaPrimaryButton(
                                text = "Start focus",
                                icon = Icons.Outlined.PlayArrow,
                                onClick = onOpenFocus,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 2-Column Bento Row: Daily Planning (lavender) + Daily Habits (butter) (Rule 6: 0 instead of 0 / 0 or 0 due)
                    val allTasksDone = taskSummary.hasTasks && taskSummary.completedCount == taskSummary.totalCount
                    val taskMetricText = if (taskSummary.hasTasks) taskSummary.completionRatioText else "0"
                    val taskStatusText = if (taskSummary.hasTasks) {
                        "${taskSummary.completedCount} of ${taskSummary.totalCount} done"
                    } else {
                        "No tasks planned"
                    }

                    val allHabitsDone = habitSummary.hasDueHabits &&
                        habitSummary.completedDueCount == habitSummary.totalDueCount
                    val habitMetricText = if (habitSummary.hasDueHabits) {
                        "${habitSummary.completedDueCount} / ${habitSummary.totalDueCount}"
                    } else {
                        "0"
                    }
                    val habitStatusText = when {
                        allHabits.isEmpty() -> "No habits scheduled"
                        habitSummary.hasDueHabits -> "${habitSummary.completedDueCount} of ${habitSummary.totalDueCount} due done"
                        else -> "None due today"
                    }

                    if (stackColumns) {
                        BentoModuleCard(
                            title = "Daily Planning",
                            metricValue = taskMetricText,
                            isEmptyMetric = !taskSummary.hasTasks,
                            statusText = taskStatusText,
                            isCompleted = allTasksDone,
                            icon = Icons.AutoMirrored.Outlined.EventNote,
                            containerColor = colors.tints.lavender.base,
                            onClick = { onOpenPlanning(today) }
                        )
                        BentoModuleCard(
                            title = "Daily Habits",
                            metricValue = habitMetricText,
                            isEmptyMetric = !habitSummary.hasDueHabits,
                            statusText = habitStatusText,
                            isCompleted = allHabitsDone,
                            icon = Icons.Outlined.Repeat,
                            containerColor = colors.tints.butter.base,
                            onClick = { onOpenHabits(today) }
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                        ) {
                            BentoModuleCard(
                                title = "Daily Planning",
                                metricValue = taskMetricText,
                                isEmptyMetric = !taskSummary.hasTasks,
                                statusText = taskStatusText,
                                isCompleted = allTasksDone,
                                icon = Icons.AutoMirrored.Outlined.EventNote,
                                containerColor = colors.tints.lavender.base,
                                onClick = { onOpenPlanning(today) },
                                modifier = Modifier.weight(1f)
                            )
                            BentoModuleCard(
                                title = "Daily Habits",
                                metricValue = habitMetricText,
                                isEmptyMetric = !habitSummary.hasDueHabits,
                                statusText = habitStatusText,
                                isCompleted = allHabitsDone,
                                icon = Icons.Outlined.Repeat,
                                containerColor = colors.tints.butter.base,
                                onClick = { onOpenHabits(today) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Full-Width Daily Review Tile (tint.rose)
                    val reviewDone = todayReview?.status == ReviewStatus.COMPLETED
                    val reviewStatusText = when (todayReview?.status) {
                        ReviewStatus.COMPLETED -> "Today's review completed"
                        ReviewStatus.DRAFT -> "Draft saved for today"
                        null -> "Ready for evening reflection"
                    }
                    LaTintedTile(
                        containerColor = colors.tints.rose.base,
                        onClick = { onOpenReview(today) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LaIconChip(
                                    icon = Icons.Outlined.RateReview,
                                    onTintedSurface = true
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Daily Review",
                                        style = LaTheme.typography.sectionTitle,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    StatusPillRow(
                                        text = reviewStatusText,
                                        isCompleted = reviewDone
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            LaArrowButton(
                                contentDescription = "Open Daily Review",
                                onClick = { onOpenReview(today) },
                                onTintedSurface = true
                            )
                        }
                    }
                }
            }
        }

        // 4. Today's Session Timeline (Section 11.2 & Step 2 Activity: concise subtitle, no duplicate Open timer link)
        item {
            LaContainerPanel(
                title = "Today's focus timeline",
                subtitle = "Today's sessions, excluding pauses"
            ) {
                LaSessionTimeline(
                    segments = timelineSegments,
                    tint = colors.tints.mint,
                    emptyMessage = "No completed focus intervals today yet",
                    accessibilitySummary = "Today's focus timeline showing ${timelineSegments.size} completed session intervals."
                )
            }
        }
    }
}

@Composable
private fun BentoModuleCard(
    title: String,
    metricValue: String,
    isEmptyMetric: Boolean,
    statusText: String,
    isCompleted: Boolean,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LaTheme.colors

    LaTintedTile(
        containerColor = containerColor,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(152.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LaIconChip(
                icon = icon,
                onTintedSurface = true
            )
            LaArrowButton(
                contentDescription = "Open $title",
                onClick = onClick,
                onTintedSurface = true
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = LaTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onTintSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = metricValue,
            style = if (isEmptyMetric) {
                LaTheme.typography.metricEmptyValue
            } else {
                LaTheme.typography.metricValue
            },
            color = if (isEmptyMetric) colors.onTintSecondary else colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatusPillRow(
            text = statusText,
            isCompleted = isCompleted
        )
    }
}

/**
 * Reserves the check mark strictly for completed states; uses a neutral dot for planned/empty states.
 */
@Composable
private fun StatusPillRow(
    text: String,
    isCompleted: Boolean
) {
    val colors = LaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.ink),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Completed",
                    tint = colors.onInk,
                    modifier = Modifier.size(11.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.onTintSecondary.copy(alpha = 0.65f))
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.Medium),
            color = colors.onTintSecondary
        )
    }
}
