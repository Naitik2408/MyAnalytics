package com.lifeanalytics.app.feature.home

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.designsystem.EmptyStateCard
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.MetricCard
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.designsystem.SuccessGreen
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.ReviewStatus
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.model.TransactionType
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import com.lifeanalytics.app.feature.finance.TransactionFormDialog
import com.lifeanalytics.app.feature.planning.TaskFormDialog
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    repository: LifeAnalyticsRepository,
    currencyCode: String,
    onNavigateToPlanning: (LocalDate) -> Unit,
    onNavigateToFocus: (String?) -> Unit,
    onNavigateToHabits: (LocalDate) -> Unit,
    onNavigateToFinanceDay: (LocalDate) -> Unit,
    onNavigateToDailyReview: (LocalDate) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }
    val greeting = remember { DateAttributionPolicy.greetingForHour(LocalTime.now().hour) }

    val todayTasks by remember(today) {
        repository.observeTasksForDate(today)
    }.collectAsState(initial = emptyList())

    val todaySessions by remember(today) {
        repository.observeSessionsForDate(today)
    }.collectAsState(initial = emptyList())

    val unresolvedSession by repository.observeUnresolvedSession().collectAsState(initial = null)

    val allHabits by repository.observeAllHabits().collectAsState(initial = emptyList())
    val todayHabitCompletions by remember(today) {
        repository.observeHabitCompletionsForDate(today)
    }.collectAsState(initial = emptyList())

    val todayTransactions by remember(today) {
        repository.observeTransactionsForDate(today)
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
    val completedSessionCount = remember(todaySessions) {
        todaySessions.count { it.status == FocusSessionStatus.COMPLETED }
    }
    val habitSummary = remember(allHabits, todayHabitCompletions, today) {
        DomainCalculations.evaluateHabitsForDate(allHabits, todayHabitCompletions, today)
    }
    val financeSummary = remember(todayTransactions) {
        DomainCalculations.calculateFinancePeriodSummary(todayTransactions)
    }

    val scope = rememberCoroutineScope()
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = DateAttributionPolicy.formatDisplayDate(today),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (errorMessage != null) {
                item {
                    ErrorBanner(
                        message = errorMessage!!,
                        onDismiss = { errorMessage = null }
                    )
                }
            }

            if (unresolvedSession != null) {
                val active = unresolvedSession!!
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToFocus(null) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    val statusText = when (active.status) {
                                        FocusSessionStatus.ACTIVE -> "Focus session running"
                                        FocusSessionStatus.PAUSED -> "Focus session paused"
                                        FocusSessionStatus.INTERRUPTED -> "Interrupted session needs review"
                                        else -> "Focus session"
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = active.displayTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            FilledTonalButton(onClick = { onNavigateToFocus(null) }) {
                                Text("Open")
                            }
                        }
                    }
                }
            }

            // 2x2 Summary Metric Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            label = "Tasks completed",
                            value = taskSummary.completionRatioText,
                            supportingText = if (taskSummary.hasTasks) {
                                "${taskSummary.completedCount} of ${taskSummary.totalCount} done today"
                            } else {
                                "No tasks planned"
                            },
                            onClick = { onNavigateToPlanning(today) },
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Focused time",
                            value = DateAttributionPolicy.formatDurationSeconds(completedFocusSeconds),
                            supportingText = when {
                                unresolvedSession != null -> "Session in progress"
                                completedSessionCount > 0 -> "$completedSessionCount session(s) today"
                                else -> "No sessions today"
                            },
                            onClick = { onNavigateToFocus(null) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            label = "Today's expenses",
                            value = MoneyUtils.formatMinorUnits(financeSummary.totalExpenseMinor, currencyCode),
                            supportingText = if (financeSummary.transactionCount > 0) {
                                "${financeSummary.transactionCount} transaction(s) today"
                            } else {
                                "No expenses today"
                            },
                            onClick = { onNavigateToFinanceDay(today) },
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Habits done",
                            value = habitSummary.completionRatioText,
                            supportingText = when {
                                allHabits.isEmpty() -> "No habits created"
                                habitSummary.hasDueHabits -> "${habitSummary.completedDueCount} of ${habitSummary.totalDueCount} due today"
                                else -> "None due on this weekday"
                            },
                            onClick = { onNavigateToHabits(today) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Quick Actions")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToFocus(null) },
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (unresolvedSession != null) "Resume Focus" else "Start Focus")
                        }
                        OutlinedButton(
                            onClick = { showAddTaskDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Task")
                        }
                        OutlinedButton(
                            onClick = { showAddExpenseDialog = true },
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Expense")
                        }
                    }
                }
            }

            // Today's Tasks Preview (up to 4 tasks, incomplete first)
            item {
                SectionHeader(
                    title = "Today's Tasks",
                    actionLabel = "View all",
                    onActionClick = { onNavigateToPlanning(today) }
                )
            }

            if (todayTasks.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No tasks planned for today",
                        message = "Plan your top tasks for today to keep your day clear and focused.",
                        actionLabel = "Add Task",
                        onActionClick = { showAddTaskDialog = true }
                    )
                }
            } else {
                val previewTasks = todayTasks.take(4)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        previewTasks.forEach { task ->
                            HomeTaskPreviewRow(
                                task = task,
                                onToggleComplete = { checked ->
                                    scope.launch {
                                        repository.setTaskCompleted(task.id, checked)
                                            .onFailure { errorMessage = it.message }
                                    }
                                },
                                onStartFocus = { onNavigateToFocus(task.id) },
                                onOpenPlanning = { onNavigateToPlanning(today) }
                            )
                        }
                        if (todayTasks.size > 4) {
                            Text(
                                text = "+${todayTasks.size - 4} more task(s) in Daily Planning",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onNavigateToPlanning(today) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Today's Habits Preview
            item {
                SectionHeader(
                    title = "Today's Habits",
                    actionLabel = "View all",
                    onActionClick = { onNavigateToHabits(today) }
                )
            }

            if (allHabits.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No habits created yet",
                        message = "Set up daily or weekday habits to track your routines alongside tasks.",
                        actionLabel = "Open Habits",
                        onActionClick = { onNavigateToHabits(today) }
                    )
                }
            } else if (habitSummary.dueHabits.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No habits scheduled for today",
                        message = "Your ${habitSummary.totalActiveHabitsCount} active habit(s) are scheduled on other weekdays.",
                        actionLabel = "View Schedule",
                        onActionClick = { onNavigateToHabits(today) }
                    )
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        habitSummary.dueHabits.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            repository.setHabitCompletionForDate(
                                                habitId = item.habit.id,
                                                date = today,
                                                completed = !item.isCompleted
                                            ).onFailure { errorMessage = it.message }
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isCompleted,
                                        onCheckedChange = { checked ->
                                            scope.launch {
                                                repository.setHabitCompletionForDate(
                                                    habitId = item.habit.id,
                                                    date = today,
                                                    completed = checked
                                                ).onFailure { errorMessage = it.message }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.habit.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (item.isCompleted) "Done" else "Due today",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (item.isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily Review entry card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDailyReview(today) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RateReview,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Daily Review",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val statusSubtitle = when (todayReview?.status) {
                                    ReviewStatus.COMPLETED -> "Completed for today • Tap to view or edit"
                                    ReviewStatus.DRAFT -> "Draft saved for today • Tap to finish"
                                    null -> "Reflect on today's summary in under a minute"
                                }
                                Text(
                                    text = statusSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(onClick = { onNavigateToDailyReview(today) }) {
                            Text(if (todayReview == null) "Review" else "Open")
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        TaskFormDialog(
            initialTask = null,
            defaultDate = today,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, category, plannedDate, plannedTime, estimateMinutes ->
                repository.createTask(
                    title = title,
                    category = category,
                    plannedDate = plannedDate,
                    plannedTime = plannedTime,
                    estimatedDurationMinutes = estimateMinutes
                ).map {
                    showAddTaskDialog = false
                }
            }
        )
    }

    if (showAddExpenseDialog) {
        TransactionFormDialog(
            initialTransaction = null,
            defaultType = TransactionType.EXPENSE,
            defaultDate = today,
            currencyCode = currencyCode,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { type, amountMinor, category, date, note ->
                scope.launch {
                    repository.saveTransaction(
                        transactionId = null,
                        type = type,
                        amountMinor = amountMinor,
                        currencyCode = currencyCode,
                        category = category,
                        transactionDate = date,
                        note = note
                    ).onSuccess {
                        showAddExpenseDialog = false
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Unable to save expense."
                    }
                }
            }
        )
    }
}

@Composable
private fun HomeTaskPreviewRow(
    task: Task,
    onToggleComplete: (Boolean) -> Unit,
    onStartFocus: () -> Unit,
    onOpenPlanning: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPlanning),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onToggleComplete
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val metaParts = buildList {
                    add(task.category.displayName)
                    task.plannedTime?.let { add(DateAttributionPolicy.formatDisplayTime(it)) }
                    task.estimatedDurationMinutes?.let { add(DateAttributionPolicy.formatMinutes(it)) }
                }
                Text(
                    text = metaParts.joinToString(" • "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!task.isCompleted) {
                IconButton(onClick = onStartFocus) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Start focus on ${task.title}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
