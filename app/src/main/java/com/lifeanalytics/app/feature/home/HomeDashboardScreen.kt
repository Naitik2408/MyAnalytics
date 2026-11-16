package com.lifeanalytics.app.feature.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.designsystem.EmptyStateCard
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaAnimatedCheckbox
import com.lifeanalytics.app.core.designsystem.LaArrowButton
import com.lifeanalytics.app.core.designsystem.LaCircularIconButton
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaGoalGauge
import com.lifeanalytics.app.core.designsystem.LaHeroPanel
import com.lifeanalytics.app.core.designsystem.LaHorizontalTrackBar
import com.lifeanalytics.app.core.designsystem.LaIconChip
import com.lifeanalytics.app.core.designsystem.LaPillBarChart
import com.lifeanalytics.app.core.designsystem.LaPillBarEntry
import com.lifeanalytics.app.core.designsystem.LaPrimaryButton
import com.lifeanalytics.app.core.designsystem.LaRowDivider
import com.lifeanalytics.app.core.designsystem.LaSecondaryButton
import com.lifeanalytics.app.core.designsystem.LaStatTile
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.LaTintedTile
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.designsystem.pressScaleClickable
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
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

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
    val (weekStart, weekEnd) = remember(today) { DateAttributionPolicy.isoWeekRange(today) }

    val todayTasks by remember(today) {
        repository.observeTasksForDate(today)
    }.collectAsState(initial = emptyList())

    val weekTasks by remember(weekStart, weekEnd) {
        repository.observeTasksInRange(weekStart, weekEnd)
    }.collectAsState(initial = emptyList())

    val todaySessions by remember(today) {
        repository.observeSessionsForDate(today)
    }.collectAsState(initial = emptyList())

    val weekSessions by remember(weekStart, weekEnd) {
        repository.observeSessionsInRange(weekStart, weekEnd)
    }.collectAsState(initial = emptyList())

    val unresolvedSession by repository.observeUnresolvedSession().collectAsState(initial = null)

    val allHabits by repository.observeAllHabits().collectAsState(initial = emptyList())
    val todayHabitCompletions by remember(today) {
        repository.observeHabitCompletionsForDate(today)
    }.collectAsState(initial = emptyList())
    val weekHabitCompletions by remember(weekStart, weekEnd) {
        repository.observeHabitCompletionsInRange(weekStart, weekEnd)
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

    // Build 7-day week strip & weekly focus pill bar entries from real source records
    val weekDays = remember(weekStart) {
        (0L..6L).map { weekStart.plusDays(it) }
    }
    val todayIndexInWeek = remember(weekDays, today) {
        weekDays.indexOf(today).coerceAtLeast(0)
    }
    var selectedWeekIndex by rememberSaveable { mutableIntStateOf(todayIndexInWeek) }

    val weeklyFocusEntries = remember(weekDays, weekSessions) {
        weekDays.map { date ->
            val daySeconds = DomainCalculations.sumCompletedFocusDurationSeconds(
                weekSessions.filter { it.startedLocalDate == date }
            )
            val minutes = daySeconds / 60f
            LaPillBarEntry(
                label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                value = minutes,
                valueLabel = DateAttributionPolicy.formatDurationSeconds(daySeconds)
            )
        }
    }
    val weeklyTotalFocusSeconds = remember(weekSessions) {
        DomainCalculations.sumCompletedFocusDurationSeconds(weekSessions)
    }

    val scope = rememberCoroutineScope()
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val colors = LaTheme.colors
    val spacing = LaTheme.spacing
    val fontScale = LocalDensity.current.fontScale

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentPadding = PaddingValues(bottom = spacing.dockBottomClearance),
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        // 1. White Header Sheet with large bottom corners (Signature Detail #8)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LaTheme.radii.shapeHeaderSheetBottom)
                    .background(colors.surface)
                    .padding(
                        start = spacing.screenInset,
                        end = spacing.screenInset,
                        top = spacing.space3,
                        bottom = spacing.space5
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.tints.lavender.base),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "LA",
                                style = LaTheme.typography.buttonLabel,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = greeting,
                                style = LaTheme.typography.sectionTitle,
                                color = colors.textPrimary
                            )
                            Text(
                                text = DateAttributionPolicy.formatDisplayDate(today),
                                style = LaTheme.typography.label,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LaCircularIconButton(
                            icon = Icons.Outlined.RateReview,
                            contentDescription = "Daily Review",
                            onClick = { onNavigateToDailyReview(today) }
                        )
                        LaCircularIconButton(
                            icon = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            onClick = onNavigateToSettings
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.space4))

                // Home-only Editorial Headline (Section 5)
                Text(
                    text = "Your daily\nrhythm, clearly.",
                    style = LaTheme.typography.editorialHeadline,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(spacing.space4))

                // Week Strip (Section 7.2)
                HomeWeekStrip(
                    weekDays = weekDays,
                    today = today,
                    selectedDay = weekDays.getOrElse(selectedWeekIndex) { today },
                    weekTasks = weekTasks,
                    weekSessions = weekSessions,
                    weekHabitCompletions = weekHabitCompletions,
                    onSelectDay = { idx, date ->
                        selectedWeekIndex = idx
                        if (date != today) {
                            onNavigateToPlanning(date)
                        }
                    }
                )
            }
        }

        if (errorMessage != null) {
            item {
                Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                    ErrorBanner(
                        message = errorMessage!!,
                        onDismiss = { errorMessage = null }
                    )
                }
            }
        }

        // Running / Paused / Interrupted Focus Session Banner
        if (unresolvedSession != null) {
            val active = unresolvedSession!!
            item {
                Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                    LaTintedTile(
                        containerColor = colors.tints.mint.strong,
                        shape = LaTheme.radii.shapeLg,
                        onClick = { onNavigateToFocus(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LaIconChip(
                                    icon = Icons.Outlined.Timer,
                                    onTintedSurface = true
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    val statusText = when (active.status) {
                                        FocusSessionStatus.ACTIVE -> "Focus session running"
                                        FocusSessionStatus.PAUSED -> "Focus session paused"
                                        FocusSessionStatus.INTERRUPTED -> "Interrupted session needs review"
                                        else -> "Focus session"
                                    }
                                    Text(
                                        text = statusText,
                                        style = LaTheme.typography.sectionTitle,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = active.displayTitle,
                                        style = LaTheme.typography.label,
                                        color = colors.onTintSecondary
                                    )
                                }
                            }
                            LaArrowButton(
                                contentDescription = "Open Focus Timer",
                                onClick = { onNavigateToFocus(null) },
                                onTintedSurface = true
                            )
                        }
                    }
                }
            }
        }

        // 2. Lavender Hero Panel with Weekly Focus Pill-Bar Chart (Section 7.2 & 11.2)
        item {
            val selectedDateInHero = weekDays.getOrElse(selectedWeekIndex) { today }
            val selectedEntry = weeklyFocusEntries.getOrNull(selectedWeekIndex)
            val heroSubtitle = if (weeklyTotalFocusSeconds > 0L) {
                "This week: ${DateAttributionPolicy.formatDurationSeconds(weeklyTotalFocusSeconds)} • ${DateAttributionPolicy.formatShortDate(selectedDateInHero)}: ${selectedEntry?.valueLabel ?: "0m"}"
            } else {
                "This week: 0m recorded across 7 days"
            }

            Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                LaHeroPanel(
                    title = "Focus rhythm",
                    subtitle = heroSubtitle,
                    icon = Icons.Outlined.Timer,
                    containerColor = colors.tints.lavender.strong,
                    onActionClick = { onNavigateToFocus(null) },
                    actionContentDescription = "Open Focus Timer"
                ) {
                    LaPillBarChart(
                        entries = weeklyFocusEntries,
                        selectedIndex = selectedWeekIndex,
                        onSelectIndex = { selectedWeekIndex = it },
                        barTint = colors.tints.lavender,
                        onHeroSurface = true,
                        chartHeight = 132.dp,
                        emptyMessage = "No focus sessions recorded this week yet",
                        accessibilitySummary = "Weekly focus time chart: $heroSubtitle"
                    )
                }
            }
        }

        // 3. Asymmetric Bento Grid of 4 Summary Metrics (Section 3.2, 4.4, 7.2)
        item {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenInset)
            ) {
                val shouldStackColumns = maxWidth < 340.dp || fontScale >= 1.35f
                val taskRatio = if (taskSummary.totalCount > 0) {
                    taskSummary.completedCount.toFloat() / taskSummary.totalCount.toFloat()
                } else 0f

                Column(verticalArrangement = Arrangement.spacedBy(spacing.tileGutter)) {
                    if (shouldStackColumns) {
                        LaStatTile(
                            label = "Tasks completed",
                            value = taskSummary.completionRatioText,
                            icon = Icons.AutoMirrored.Outlined.EventNote,
                            supportingText = if (taskSummary.hasTasks) {
                                "${taskSummary.completedCount} of ${taskSummary.totalCount} done today"
                            } else {
                                "No tasks planned"
                            },
                            containerColor = colors.tints.lavender.base,
                            onClick = { onNavigateToPlanning(today) }
                        )
                        LaStatTile(
                            label = "Focused time",
                            value = DateAttributionPolicy.formatDurationSeconds(completedFocusSeconds),
                            icon = Icons.Outlined.Timer,
                            supportingText = when {
                                unresolvedSession != null -> "Session in progress"
                                completedSessionCount > 0 -> "$completedSessionCount session(s) today"
                                else -> "No sessions today"
                            },
                            containerColor = colors.tints.mint.base,
                            onClick = { onNavigateToFocus(null) }
                        )
                        LaStatTile(
                            label = "Today's expenses",
                            value = MoneyUtils.formatMinorUnits(financeSummary.totalExpenseMinor, currencyCode),
                            icon = Icons.Outlined.AccountBalanceWallet,
                            supportingText = if (financeSummary.transactionCount > 0) {
                                "${financeSummary.transactionCount} entry today"
                            } else {
                                "No expenses today"
                            },
                            containerColor = colors.tints.blue.base,
                            onClick = { onNavigateToFinanceDay(today) }
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Left tall tile: Tasks + Goal Gauge micro-visual
                            LaStatTile(
                                label = "Tasks completed",
                                value = taskSummary.completionRatioText,
                                icon = Icons.AutoMirrored.Outlined.EventNote,
                                supportingText = if (taskSummary.hasTasks) {
                                    "${taskSummary.completedCount} of ${taskSummary.totalCount} done today"
                                } else {
                                    "No tasks planned"
                                },
                                containerColor = colors.tints.lavender.base,
                                onClick = { onNavigateToPlanning(today) },
                                modifier = Modifier.weight(1f),
                                microVisual = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LaGoalGauge(
                                            progress = taskRatio,
                                            centerText = if (taskSummary.hasTasks) {
                                                "${(taskRatio * 100).toInt()}%"
                                            } else {
                                                "0%"
                                            },
                                            size = 86.dp
                                        )
                                    }
                                }
                            )

                            // Right column: two stacked tiles of distinct tints (mint + blue)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                            ) {
                                LaStatTile(
                                    label = "Focused time",
                                    value = DateAttributionPolicy.formatDurationSeconds(completedFocusSeconds),
                                    icon = Icons.Outlined.Timer,
                                    supportingText = when {
                                        unresolvedSession != null -> "Session in progress"
                                        completedSessionCount > 0 -> "$completedSessionCount session(s) today"
                                        else -> "No sessions today"
                                    },
                                    containerColor = colors.tints.mint.base,
                                    onClick = { onNavigateToFocus(null) }
                                )
                                LaStatTile(
                                    label = "Today's expenses",
                                    value = MoneyUtils.formatMinorUnits(financeSummary.totalExpenseMinor, currencyCode),
                                    icon = Icons.Outlined.AccountBalanceWallet,
                                    supportingText = if (financeSummary.transactionCount > 0) {
                                        "${financeSummary.transactionCount} transaction(s)"
                                    } else {
                                        "No expenses today"
                                    },
                                    containerColor = colors.tints.blue.base,
                                    onClick = { onNavigateToFinanceDay(today) }
                                )
                            }
                        }
                    }

                    // Full-width Habits Bento Tile in tint.butter.base with track bar micro-visual
                    val habitRatio = if (habitSummary.totalDueCount > 0) {
                        habitSummary.completedDueCount.toFloat() / habitSummary.totalDueCount.toFloat()
                    } else 0f
                    LaStatTile(
                        label = "Habits done",
                        value = habitSummary.completionRatioText,
                        icon = Icons.Outlined.Repeat,
                        supportingText = when {
                            allHabits.isEmpty() -> "Tap to create your first recurring habit"
                            habitSummary.hasDueHabits -> "${habitSummary.completedDueCount} of ${habitSummary.totalDueCount} scheduled for today"
                            else -> "None scheduled on this weekday"
                        },
                        containerColor = colors.tints.butter.base,
                        onClick = { onNavigateToHabits(today) },
                        microVisual = if (habitSummary.hasDueHabits) {
                            {
                                LaHorizontalTrackBar(
                                    label = "Daily habit completion",
                                    valueText = "${(habitRatio * 100).toInt()}%",
                                    progress = habitRatio,
                                    tint = colors.tints.butter,
                                    useInkFill = true
                                )
                            }
                        } else null
                    )
                }
            }
        }

        // 4. Quick Actions: 1 Ink Primary + 2 Tonal Secondary Buttons (Zero mid-word wrapping!)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenInset),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader(title = "Quick actions")
                LaPrimaryButton(
                    text = if (unresolvedSession != null) "Resume focus session" else "Start focus",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = { onNavigateToFocus(null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                ) {
                    LaSecondaryButton(
                        text = "Add task",
                        icon = Icons.Outlined.Add,
                        onClick = { showAddTaskDialog = true },
                        containerColor = colors.surface,
                        modifier = Modifier.weight(1f)
                    )
                    LaSecondaryButton(
                        text = "Add expense",
                        icon = Icons.Outlined.Add,
                        onClick = { showAddExpenseDialog = true },
                        containerColor = colors.surface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Today's Tasks in a Single White Container Panel (Section 3.2 & 9.4)
        item {
            Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                LaContainerPanel(
                    title = "Today's tasks",
                    subtitle = if (todayTasks.isNotEmpty()) {
                        "${taskSummary.completedCount} of ${taskSummary.totalCount} completed"
                    } else null,
                    actionLabel = "View all",
                    onActionClick = { onNavigateToPlanning(today) }
                ) {
                    if (todayTasks.isEmpty()) {
                        EmptyStateCard(
                            title = "No tasks planned for today",
                            message = "Add your key tasks for today to keep your plan clear.",
                            actionLabel = "Add task",
                            onActionClick = { showAddTaskDialog = true },
                            tintSoftColor = colors.tints.lavender.soft
                        )
                    } else {
                        val previewTasks = todayTasks.take(4)
                        previewTasks.forEachIndexed { index, task ->
                            HomeTaskListRow(
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
                            if (index < previewTasks.lastIndex) {
                                LaRowDivider()
                            }
                        }
                        if (todayTasks.size > 4) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "+${todayTasks.size - 4} more task(s) in Daily Planning",
                                style = LaTheme.typography.label,
                                color = colors.accent,
                                modifier = Modifier
                                    .pressScaleClickable { onNavigateToPlanning(today) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. Today's Habits in a Single White Container Panel
        item {
            Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                LaContainerPanel(
                    title = "Today's habits",
                    subtitle = if (habitSummary.hasDueHabits) {
                        "${habitSummary.completedDueCount} of ${habitSummary.totalDueCount} completed"
                    } else null,
                    actionLabel = "View all",
                    onActionClick = { onNavigateToHabits(today) }
                ) {
                    if (allHabits.isEmpty()) {
                        EmptyStateCard(
                            title = "No habits created yet",
                            message = "Set up recurring daily or weekday habits to observe consistency.",
                            actionLabel = "Create habit",
                            onActionClick = { onNavigateToHabits(today) },
                            tintSoftColor = colors.tints.butter.soft
                        )
                    } else if (habitSummary.dueHabits.isEmpty()) {
                        EmptyStateCard(
                            title = "No habits scheduled for today",
                            message = "Your ${habitSummary.totalActiveHabitsCount} active habit(s) are scheduled on other weekdays.",
                            actionLabel = "View schedule",
                            onActionClick = { onNavigateToHabits(today) },
                            tintSoftColor = colors.tints.butter.soft
                        )
                    } else {
                        habitSummary.dueHabits.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScaleClickable {
                                        scope.launch {
                                            repository.setHabitCompletionForDate(
                                                habitId = item.habit.id,
                                                date = today,
                                                completed = !item.isCompleted
                                            ).onFailure { errorMessage = it.message }
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LaAnimatedCheckbox(
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
                                    style = LaTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(LaTheme.radii.shapeFull)
                                        .background(
                                            if (item.isCompleted) colors.tints.mint.base else colors.surfaceVariant
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (item.isCompleted) "Done" else "Due today",
                                        style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary
                                    )
                                }
                            }
                            if (index < habitSummary.dueHabits.lastIndex) {
                                LaRowDivider()
                            }
                        }
                    }
                }
            }
        }

        // 7. Daily Review Tinted Tile (tint.rose.base)
        item {
            Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                LaTintedTile(
                    containerColor = colors.tints.rose.base,
                    onClick = { onNavigateToDailyReview(today) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                                val statusSubtitle = when (todayReview?.status) {
                                    ReviewStatus.COMPLETED -> "Completed for today • Tap to view or edit"
                                    ReviewStatus.DRAFT -> "Draft saved for today • Tap to finish"
                                    null -> "Reflect on today's summary in under a minute"
                                }
                                Text(
                                    text = statusSubtitle,
                                    style = LaTheme.typography.label,
                                    color = colors.onTintSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        LaArrowButton(
                            contentDescription = "Open Daily Review",
                            onClick = { onNavigateToDailyReview(today) },
                            onTintedSurface = true
                        )
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
private fun HomeWeekStrip(
    weekDays: List<LocalDate>,
    today: LocalDate,
    selectedDay: LocalDate,
    weekTasks: List<Task>,
    weekSessions: List<com.lifeanalytics.app.domain.model.FocusSession>,
    weekHabitCompletions: List<com.lifeanalytics.app.domain.model.HabitCompletion>,
    onSelectDay: (Int, LocalDate) -> Unit
) {
    val colors = LaTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekDays.forEachIndexed { idx, date ->
            val isToday = date == today
            val isSelected = date == selectedDay
            val hasCompletedTask = weekTasks.any { it.plannedDate == date && it.isCompleted }
            val hasCompletedFocus = weekSessions.any {
                it.startedLocalDate == date && it.status == FocusSessionStatus.COMPLETED
            }
            val hasCompletedHabit = weekHabitCompletions.any {
                it.completionDate == date && it.isCompleted
            }
            val hasAnyActivity = hasCompletedTask || hasCompletedFocus || hasCompletedHabit

            val tileBg = when {
                isToday -> colors.tints.lavender.strong
                isSelected -> colors.tints.lavender.base
                else -> colors.tints.lavender.soft
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(tileBg)
                    .pressScaleClickable { onSelectDay(idx, date) }
                    .padding(vertical = 8.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    style = LaTheme.typography.chartMicroLabel.copy(
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isToday) colors.textPrimary else colors.onTintSecondary,
                    maxLines = 1
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = LaTheme.typography.body.copy(
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = colors.textPrimary
                )
                if (hasAnyActivity) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(colors.ink),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Activity recorded",
                            tint = colors.onInk,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(6.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeTaskListRow(
    task: Task,
    onToggleComplete: (Boolean) -> Unit,
    onStartFocus: () -> Unit,
    onOpenPlanning: () -> Unit
) {
    val colors = LaTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onOpenPlanning)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LaAnimatedCheckbox(
            checked = task.isCompleted,
            onCheckedChange = onToggleComplete
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = LaTheme.typography.body.copy(
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (task.isCompleted) colors.textSecondary else colors.textPrimary,
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
                style = LaTheme.typography.chartMicroLabel,
                color = colors.textSecondary
            )
        }
        if (!task.isCompleted) {
            Spacer(modifier = Modifier.width(8.dp))
            LaCircularIconButton(
                icon = Icons.Outlined.PlayArrow,
                contentDescription = "Start focus on ${task.title}",
                onClick = onStartFocus,
                size = 44.dp
            )
        }
    }
}
