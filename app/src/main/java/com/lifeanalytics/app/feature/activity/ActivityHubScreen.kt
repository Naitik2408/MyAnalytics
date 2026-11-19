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
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.designsystem.LaArrowButton
import com.lifeanalytics.app.core.designsystem.LaIconChip
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.LaTintedTile
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.ReviewStatus
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.LocalDate

@Composable
fun ActivityHubScreen(
    repository: LifeAnalyticsRepository,
    onOpenPlanning: (LocalDate) -> Unit,
    onOpenHabits: (LocalDate) -> Unit,
    onOpenReview: (LocalDate) -> Unit
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }

    val todayTasks by remember(today) {
        repository.observeTasksForDate(today)
    }.collectAsState(initial = emptyList())

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
    val habitSummary = remember(allHabits, todayCompletions, today) {
        DomainCalculations.evaluateHabitsForDate(allHabits, todayCompletions, today)
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
        item {
            LaScreenHeader(
                title = "Activity",
                subtitle = "Plan daily tasks, track habits, and review your day."
            )
        }

        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackColumns = maxWidth < 340.dp || fontScale >= 1.35f

                Column(verticalArrangement = Arrangement.spacedBy(spacing.tileGutter)) {
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
