package com.lifeanalytics.app.feature.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.ReviewStatus
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
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
    val habitSummary = remember(allHabits, todayCompletions, today) {
        DomainCalculations.evaluateHabitsForDate(allHabits, todayCompletions, today)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity & Planning") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Plan your day, record focused work, track recurring habits, and close the day with a structured review.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                val taskStatus = if (taskSummary.hasTasks) {
                    "${taskSummary.completedCount} of ${taskSummary.totalCount} tasks completed today"
                } else {
                    "No tasks planned for today yet"
                }
                ActivityDestinationCard(
                    title = "Daily Planning",
                    description = "Create, schedule, estimate, and complete daily tasks.",
                    statusBadge = taskStatus,
                    icon = Icons.AutoMirrored.Outlined.EventNote,
                    onClick = { onOpenPlanning(today) }
                )
            }

            item {
                val focusStatus = when (unresolvedSession?.status) {
                    FocusSessionStatus.ACTIVE -> "Running: ${unresolvedSession?.displayTitle}"
                    FocusSessionStatus.PAUSED -> "Paused: ${unresolvedSession?.displayTitle}"
                    FocusSessionStatus.INTERRUPTED -> "Needs review: ${unresolvedSession?.displayTitle}"
                    else -> "Today's focused time: ${DateAttributionPolicy.formatDurationSeconds(completedFocusSeconds)}"
                }
                ActivityDestinationCard(
                    title = "Active Work & Focus Timer",
                    description = "Run a task-linked or general focus session with reliable pause and interruption handling.",
                    statusBadge = focusStatus,
                    icon = Icons.Outlined.Timer,
                    onClick = onOpenFocus
                )
            }

            item {
                val habitStatus = when {
                    allHabits.isEmpty() -> "No habits created yet"
                    habitSummary.hasDueHabits -> "${habitSummary.completedDueCount} of ${habitSummary.totalDueCount} due habits done today"
                    else -> "No habits scheduled for today (${allHabits.count { it.isActive }} active)"
                }
                ActivityDestinationCard(
                    title = "Daily Habits",
                    description = "Manage recurring habits, weekday schedules, and dated completion history.",
                    statusBadge = habitStatus,
                    icon = Icons.Outlined.Repeat,
                    onClick = { onOpenHabits(today) }
                )
            }

            item {
                val reviewStatus = when (todayReview?.status) {
                    ReviewStatus.COMPLETED -> "Today's review is completed"
                    ReviewStatus.DRAFT -> "Draft saved for today"
                    null -> "Ready to reflect on today's activity"
                }
                ActivityDestinationCard(
                    title = "Daily Review",
                    description = "Compare planned vs actual work and record end-of-day reflections.",
                    statusBadge = reviewStatus,
                    icon = Icons.Outlined.RateReview,
                    onClick = { onOpenReview(today) }
                )
            }
        }
    }
}

@Composable
private fun ActivityDestinationCard(
    title: String,
    description: String,
    statusBadge: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusBadge,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
