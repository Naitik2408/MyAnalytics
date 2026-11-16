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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaAnimatedCheckbox
import com.lifeanalytics.app.core.designsystem.LaArrowButton
import com.lifeanalytics.app.core.designsystem.LaCircularIconButton
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaFirstRunChecklistCard
import com.lifeanalytics.app.core.designsystem.LaHeroPanel
import com.lifeanalytics.app.core.designsystem.LaIconChip
import com.lifeanalytics.app.core.designsystem.LaMicroDots
import com.lifeanalytics.app.core.designsystem.LaMicroPills
import com.lifeanalytics.app.core.designsystem.LaMicroProgressBar
import com.lifeanalytics.app.core.designsystem.LaModuleEmptyState
import com.lifeanalytics.app.core.designsystem.LaPillBarChart
import com.lifeanalytics.app.core.designsystem.LaPillBarEntry
import com.lifeanalytics.app.core.designsystem.LaPrimaryButton
import com.lifeanalytics.app.core.designsystem.LaRowDivider
import com.lifeanalytics.app.core.designsystem.LaSecondaryButton
import com.lifeanalytics.app.core.designsystem.LaStatTile
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.LaTintedTile
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

    val isFirstRunEmpty = weekTasks.isEmpty() &&
        allHabits.isEmpty() &&
        weekSessions.isEmpty() &&
        todayTransactions.isEmpty() &&
        todayReview == null

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val colors = LaTheme.colors
    val spacing = LaTheme.spacing
    val fontScale = LocalDensity.current.fontScale

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = spacing.dockBottomClearance),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            // 1. White Header Sheet (<= 220 dp total height per v1.3 Rules 1 & 9)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LaTheme.radii.shapeHeaderSheetBottom)
                        .background(colors.surface)
                        .padding(
                            start = spacing.screenInset,
                            end = spacing.screenInset,
                            top = spacing.space2,
                            bottom = spacing.space4
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
                            // Neutral person icon instead of fake "LA" initials (Step 2 Home)
                            LaIconChip(
                                icon = Icons.Outlined.Person,
                                backgroundColor = colors.tints.lavender.base,
                                size = 42.dp
                            )
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

                    Spacer(modifier = Modifier.height(spacing.space2))

                    // Home-only Editorial Headline (compact 1-2 lines per Rule 9)
                    Text(
                        text = "Your daily rhythm, clearly.",
                        style = LaTheme.typography.editorialHeadline,
                        color = colors.textPrimary,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(spacing.space3))

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

            // 2. Lavender Hero Panel (<= 200 dp per Rule 1; flat equal-height ghost bars & no second caption pill per Rule 5)
            item {
                val selectedDateInHero = weekDays.getOrElse(selectedWeekIndex) { today }
                val selectedEntry = weeklyFocusEntries.getOrNull(selectedWeekIndex)
                val heroSubtitle = if (weeklyTotalFocusSeconds > 0L) {
                    "This week: ${DateAttributionPolicy.formatDurationSeconds(weeklyTotalFocusSeconds)} • ${DateAttributionPolicy.formatShortDate(selectedDateInHero)}: ${selectedEntry?.valueLabel ?: "0m"}"
                } else {
                    "No focus sessions this week yet"
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
                            chartHeight = if (weeklyTotalFocusSeconds > 0L) 104.dp else 84.dp,
                            emptyMessage = "", // Container subtitle already states empty status (Rule 5)
                            accessibilitySummary = "Weekly focus time chart: $heroSubtitle"
                        )
                    }
                }
            }

            // 3. Primary Quick Actions in First Viewport (v1.3 Rule 1: "Start focus" reachable without scrolling)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screenInset),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

            // 4. First-Run "Get started" Card (Level A — Rule 4, shown only when all 4 modules are empty)
            if (isFirstRunEmpty) {
                item {
                    Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                        LaFirstRunChecklistCard(
                            onAddFirstTask = { showAddTaskDialog = true },
                            onPickDailyHabit = { onNavigateToHabits(today) },
                            onStartFocusSession = { onNavigateToFocus(null) }
                        )
                    }
                }
            }

            // 5. 2 × 2 Bento Grid of Summary Metrics (v1.3 Rules 2, 6, 8)
            item {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screenInset)
                ) {
                    val shouldStackColumns = maxWidth < 330.dp || fontScale >= 1.35f
                    val taskRatio = if (taskSummary.totalCount > 0) {
                        taskSummary.completedCount.toFloat() / taskSummary.totalCount.toFloat()
                    } else 0f
                    val maxWeeklyFocusVal = weeklyFocusEntries.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
                    val focusMicroFractions = weeklyFocusEntries.map { (it.value / maxWeeklyFocusVal).coerceIn(0f, 1f) }

                    val tasksTile: @Composable (Modifier) -> Unit = { mod ->
                        LaStatTile(
                            label = "Tasks completed",
                            value = if (taskSummary.hasTasks) taskSummary.completionRatioText else "0",
                            icon = Icons.AutoMirrored.Outlined.EventNote,
                            supportingText = if (taskSummary.hasTasks) {
                                "${taskSummary.completedCount} of ${taskSummary.totalCount} done today"
                            } else {
                                "No tasks planned"
                            },
                            containerColor = colors.tints.lavender.base,
                            isEmptyValue = !taskSummary.hasTasks,
                            minHeight = 148.dp,
                            onClick = { onNavigateToPlanning(today) },
                            modifier = mod,
                            microVisual = if (taskSummary.hasTasks) {
                                {
                                    LaMicroProgressBar(progress = taskRatio)
                                }
                            } else null
                        )
                    }

                    val focusTile: @Composable (Modifier) -> Unit = { mod ->
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
                            isEmptyValue = completedFocusSeconds == 0L,
                            minHeight = 148.dp,
                            onClick = { onNavigateToFocus(null) },
                            modifier = mod,
                            microVisual = if (weeklyTotalFocusSeconds > 0L) {
                                {
                                    LaMicroPills(fractions = focusMicroFractions)
                                }
                            } else null
                        )
                    }

                    val expensesTile: @Composable (Modifier) -> Unit = { mod ->
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
                            isEmptyValue = financeSummary.totalExpenseMinor == 0L,
                            minHeight = 148.dp,
                            onClick = { onNavigateToFinanceDay(today) },
                            modifier = mod,
                            microVisual = if (financeSummary.totalExpenseMinor > 0L) {
                                {
                                    LaMicroPills(
                                        fractions = todayTransactions
                                            .filter { it.type == TransactionType.EXPENSE }
                                            .take(6)
                                            .let { txs ->
                                                val maxAmt = txs.maxOfOrNull { it.amountMinor }?.toFloat()?.coerceAtLeast(1f) ?: 1f
                                                txs.map { (it.amountMinor / maxAmt).coerceIn(0.25f, 1f) }
                                            }
                                    )
                                }
                            } else null
                        )
                    }

                    val habitsTile: @Composable (Modifier) -> Unit = { mod ->
                        LaStatTile(
                            label = "Habits done",
                            value = if (habitSummary.hasDueHabits) {
                                "${habitSummary.completedDueCount} / ${habitSummary.totalDueCount}"
                            } else {
                                "0"
                            },
                            icon = Icons.Outlined.Repeat,
                            supportingText = when {
                                allHabits.isEmpty() -> "No habits scheduled"
                                habitSummary.hasDueHabits -> "${habitSummary.completedDueCount} of ${habitSummary.totalDueCount} due today"
                                else -> "None due today"
                            },
                            containerColor = colors.tints.butter.base,
                            isEmptyValue = !habitSummary.hasDueHabits,
                            minHeight = 148.dp,
                            onClick = { onNavigateToHabits(today) },
                            modifier = mod,
                            microVisual = if (habitSummary.hasDueHabits) {
                                {
                                    LaMicroDots(
                                        states = habitSummary.dueHabits.map { it.isCompleted }
                                    )
                                }
                            } else null
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(spacing.tileGutter)) {
                        if (shouldStackColumns) {
                            tasksTile(Modifier.fillMaxWidth())
                            focusTile(Modifier.fillMaxWidth())
                            expensesTile(Modifier.fillMaxWidth())
                            habitsTile(Modifier.fillMaxWidth())
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                            ) {
                                tasksTile(Modifier.weight(1f))
                                focusTile(Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                            ) {
                                expensesTile(Modifier.weight(1f))
                                habitsTile(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 6. Today's Tasks in a Single White Container Panel (Level B Empty State per Rule 4)
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
                            LaModuleEmptyState(
                                icon = Icons.AutoMirrored.Outlined.EventNote,
                                title = "No tasks planned for today",
                                message = "Add your key tasks for today to keep your plan clear.",
                                actionLabel = "Add task",
                                onActionClick = { showAddTaskDialog = true },
                                chipColor = colors.tints.lavender.soft
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

            // 7. Today's Habits in a Single White Container Panel (Level B Empty State with distinct Repeat icon)
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
                            LaModuleEmptyState(
                                icon = Icons.Outlined.Repeat,
                                title = "No habits created yet",
                                message = "Set up recurring daily or weekday habits to observe consistency.",
                                actionLabel = "Create habit",
                                onActionClick = { onNavigateToHabits(today) },
                                chipColor = colors.tints.butter.soft
                            )
                        } else if (habitSummary.dueHabits.isEmpty()) {
                            LaModuleEmptyState(
                                icon = Icons.Outlined.Repeat,
                                title = "No habits scheduled for today",
                                message = "Your ${habitSummary.totalActiveHabitsCount} active habit(s) are scheduled on other weekdays.",
                                actionLabel = "View schedule",
                                onActionClick = { onNavigateToHabits(today) },
                                chipColor = colors.tints.butter.soft
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

            // 8. Daily Review Tinted Tile (tint.rose.base)
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = spacing.dockBottomClearance)
        )
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
                    scope.launch { snackbarHostState.showSnackbar("Task saved") }
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
                        snackbarHostState.showSnackbar("Expense saved")
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
                    .height(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(tileBg)
                    .pressScaleClickable { onSelectDay(idx, date) }
                    .padding(vertical = 6.dp, horizontal = 2.dp),
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
