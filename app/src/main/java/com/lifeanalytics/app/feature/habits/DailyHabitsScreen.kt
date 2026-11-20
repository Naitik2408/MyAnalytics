package com.lifeanalytics.app.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.designsystem.ConfirmDeleteDialog
import com.lifeanalytics.app.core.designsystem.DateSelectorBar
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaAnimatedCheckbox
import com.lifeanalytics.app.core.designsystem.LaCircularIconButton
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaFilterChip
import com.lifeanalytics.app.core.designsystem.LaMicroProgressBar
import com.lifeanalytics.app.core.designsystem.LaModuleEmptyState
import com.lifeanalytics.app.core.designsystem.LaPrimaryButton
import com.lifeanalytics.app.core.designsystem.LaRowDivider
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaSecondaryButton
import com.lifeanalytics.app.core.designsystem.LaStatTile
import com.lifeanalytics.app.core.designsystem.LaTextButton
import com.lifeanalytics.app.core.designsystem.LaTextField
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.LaTintedTile
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.HabitForDateItem
import com.lifeanalytics.app.domain.model.HabitScheduleType
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import com.lifeanalytics.app.domain.usecase.HabitConsistencyItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun DailyHabitsScreen(
    repository: LifeAnalyticsRepository,
    initialDate: LocalDate = DateAttributionPolicy.todayLocalDate(),
    onNavigateBack: (() -> Unit)? = null
) {
    val colors = LaTheme.colors
    val tints = colors.tints
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing
    val radii = LaTheme.radii
    val snackbarHostState = remember { SnackbarHostState() }

    val today = remember { DateAttributionPolicy.todayLocalDate() }
    var selectedDateEpochDay by rememberSaveable { mutableStateOf(initialDate.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedDateEpochDay)
    val isFutureDate = selectedDate.isAfter(today)

    val allHabits by repository.observeAllHabits().collectAsState(initial = emptyList())
    val completionsForDate by remember(selectedDate) {
        repository.observeHabitCompletionsForDate(selectedDate)
    }.collectAsState(initial = emptyList())

    val thirtyDaysAgo = remember(today) { today.minusDays(29) }
    val recentCompletions by remember(thirtyDaysAgo, today) {
        repository.observeHabitCompletionsInRange(thirtyDaysAgo, today)
    }.collectAsState(initial = emptyList())

    val dailySummary = remember(allHabits, completionsForDate, selectedDate) {
        DomainCalculations.evaluateHabitsForDate(allHabits, completionsForDate, selectedDate)
    }

    val consistency30d = remember(allHabits, recentCompletions, thirtyDaysAgo, today) {
        DomainCalculations.calculateHabitPeriodConsistency(
            habits = allHabits,
            completions = recentCompletions,
            startDate = thirtyDaysAgo,
            endDate = today,
            today = today
        )
    }
    val consistencyByHabitId = remember(consistency30d) {
        consistency30d.perHabitConsistency.associateBy { it.habit.id }
    }

    val inactiveHabits = remember(allHabits) {
        allHabits.filter { !it.isActive }
    }

    val scope = rememberCoroutineScope()
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.canvas),
            contentPadding = PaddingValues(
                start = spacing.screenInset,
                end = spacing.screenInset,
                top = spacing.space3,
                bottom = spacing.dockBottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            item {
                LaScreenHeader(
                    title = "Daily Habits",
                    subtitle = DateAttributionPolicy.formatDisplayDate(selectedDate),
                    onNavigateBack = onNavigateBack,
                    trailingContent = {
                        LaPrimaryButton(
                            text = "Add habit",
                            icon = Icons.Outlined.Add,
                            onClick = { showAddHabitDialog = true }
                        )
                    }
                )
            }

            item {
                DateSelectorBar(
                    selectedDate = selectedDate,
                    today = today,
                    onDateSelected = { selectedDateEpochDay = it.toEpochDay() }
                )
            }

            if (errorMessage != null) {
                item {
                    ErrorBanner(
                        message = errorMessage!!,
                        onDismiss = { errorMessage = null }
                    )
                }
            }

            // 2-column Bento Summary (Rule 2, Rule 6, Rule 8)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                ) {
                    LaStatTile(
                        label = "Scheduled on date",
                        value = if (dailySummary.hasDueHabits) dailySummary.completionRatioText else "0",
                        supportingText = if (dailySummary.hasDueHabits) {
                            "${dailySummary.completedDueCount} of ${dailySummary.totalDueCount} completed"
                        } else {
                            "0 due on this weekday"
                        },
                        icon = Icons.Outlined.Repeat,
                        containerColor = tints.butter.base,
                        isEmptyValue = !dailySummary.hasDueHabits,
                        minHeight = 148.dp,
                        modifier = Modifier.weight(1f),
                        microVisual = if (dailySummary.hasDueHabits) {
                            {
                                LaMicroProgressBar(
                                    progress = dailySummary.completedDueCount.toFloat() / dailySummary.totalDueCount.toFloat(),
                                    trackColor = colors.surface.copy(alpha = 0.65f),
                                    fillColor = colors.ink
                                )
                            }
                        } else null
                    )
                    LaStatTile(
                        label = "Last 30 days",
                        value = consistency30d.consistencyPercent?.let { "$it%" } ?: "—",
                        supportingText = if (consistency30d.totalDueOccurrences > 0) {
                            "${consistency30d.completedDueOccurrences} / ${consistency30d.totalDueOccurrences} due days done"
                        } else {
                            "No due days yet"
                        },
                        icon = Icons.Outlined.CalendarMonth,
                        containerColor = tints.lavender.base,
                        isEmptyValue = consistency30d.consistencyPercent == null,
                        minHeight = 148.dp,
                        modifier = Modifier.weight(1f),
                        microVisual = if (consistency30d.consistencyPercent != null) {
                            {
                                LaMicroProgressBar(
                                    progress = (consistency30d.consistencyPercent ?: 0) / 100f,
                                    trackColor = colors.surface.copy(alpha = 0.65f),
                                    fillColor = colors.ink
                                )
                            }
                        } else null
                    )
                }
            }

            if (isFutureDate) {
                item {
                    LaTintedTile(
                        containerColor = tints.butter.soft,
                        shape = radii.shapeSm
                    ) {
                        Text(
                            text = "Viewing a future date. Scheduled habits are shown below, and completion can be checked on or before today.",
                            style = typography.label,
                            color = colors.onTintSecondary
                        )
                    }
                }
            }

            if (allHabits.isEmpty()) {
                item {
                    LaContainerPanel {
                        LaModuleEmptyState(
                            icon = Icons.Outlined.Repeat,
                            iconTint = tints.butter.base,
                            title = "No habits created yet",
                            message = "Create recurring habits with daily, weekday, weekend, or custom schedules to track consistency.",
                            actionLabel = "Add habit",
                            onActionClick = { showAddHabitDialog = true }
                        )
                    }
                }
            } else {
                item {
                    SectionHeader(
                        title = "Scheduled for ${DateAttributionPolicy.formatShortDate(selectedDate)} (${dailySummary.dueHabits.size})"
                    )
                }

                if (dailySummary.dueHabits.isEmpty()) {
                    item {
                        LaContainerPanel {
                            LaModuleEmptyState(
                                icon = Icons.Outlined.Repeat,
                                iconTint = tints.butter.base,
                                title = "No habits scheduled for this date",
                                message = "Your active habits are scheduled on other weekdays."
                            )
                        }
                    }
                } else {
                    item {
                        LaContainerPanel {
                            dailySummary.dueHabits.forEachIndexed { index, item ->
                                HabitDateRow(
                                    item = item,
                                    consistency = consistencyByHabitId[item.habit.id],
                                    isFutureDate = isFutureDate,
                                    onToggleCompleted = { checked ->
                                        scope.launch {
                                            repository.setHabitCompletionForDate(
                                                habitId = item.habit.id,
                                                date = selectedDate,
                                                completed = checked
                                            ).onFailure { err ->
                                                errorMessage = err.message ?: "Unable to update habit."
                                            }
                                        }
                                    },
                                    onEdit = { habitToEdit = item.habit }
                                )
                                if (index < dailySummary.dueHabits.lastIndex) {
                                    LaRowDivider()
                                }
                            }
                        }
                    }
                }

                if (dailySummary.otherActiveHabits.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(spacing.space1))
                        SectionHeader(
                            title = "Not scheduled on this date (${dailySummary.otherActiveHabits.size})"
                        )
                    }
                    item {
                        LaContainerPanel {
                            dailySummary.otherActiveHabits.forEachIndexed { index, item ->
                                HabitNotScheduledRow(
                                    item = item,
                                    consistency = consistencyByHabitId[item.habit.id],
                                    onEdit = { habitToEdit = item.habit }
                                )
                                if (index < dailySummary.otherActiveHabits.lastIndex) {
                                    LaRowDivider()
                                }
                            }
                        }
                    }
                }

                if (inactiveHabits.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(spacing.space1))
                        SectionHeader(
                            title = "Inactive habits (${inactiveHabits.size})"
                        )
                    }
                    item {
                        LaContainerPanel {
                            inactiveHabits.forEachIndexed { index, habit ->
                                InactiveHabitRow(
                                    habit = habit,
                                    consistency = consistencyByHabitId[habit.id],
                                    onDelete = { habitToDelete = habit }
                                )
                                if (index < inactiveHabits.lastIndex) {
                                    LaRowDivider()
                                }
                            }
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

    if (showAddHabitDialog) {
        HabitFormDialog(
            initialHabit = null,
            effectiveDate = today,
            onDismiss = { showAddHabitDialog = false },
            onSave = { name, weekdays, lifeArea ->
                scope.launch {
                    repository.createHabit(
                        name = name,
                        weekdays = weekdays,
                        effectiveFrom = selectedDate.coerceAtMost(today),
                        lifeArea = lifeArea
                    ).onSuccess {
                        showAddHabitDialog = false
                        snackbarHostState.showSnackbar("Habit saved")
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Could not save habit."
                    }
                }
            },
            onDeactivate = null,
            onRequestDelete = null
        )
    }

    if (habitToEdit != null) {
        val editing = habitToEdit!!
        HabitFormDialog(
            initialHabit = editing,
            effectiveDate = today,
            onDismiss = { habitToEdit = null },
            onSave = { name, weekdays, lifeArea ->
                scope.launch {
                    repository.updateHabit(
                        habitId = editing.id,
                        name = name,
                        weekdays = weekdays,
                        effectiveDate = today,
                        lifeArea = lifeArea
                    ).onSuccess {
                        habitToEdit = null
                        snackbarHostState.showSnackbar("Habit updated")
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Could not update habit."
                    }
                }
            },
            onDeactivate = if (editing.isActive) {
                {
                    scope.launch {
                        repository.deactivateHabit(editing.id, today)
                            .onSuccess {
                                habitToEdit = null
                                snackbarHostState.showSnackbar("Habit deactivated")
                            }
                            .onFailure { err -> errorMessage = err.message }
                    }
                }
            } else null,
            onRequestDelete = {
                habitToEdit = null
                habitToDelete = editing
            }
        )
    }

    if (habitToDelete != null) {
        val deleting = habitToDelete!!
        ConfirmDeleteDialog(
            title = "Delete habit permanently?",
            message = "Deleting \"${deleting.name}\" will remove the habit and all of its historical completion records. To stop tracking it going forward while keeping past history, use Deactivate instead.",
            confirmLabel = "Delete permanently",
            onConfirm = {
                scope.launch {
                    repository.deleteHabitPermanently(deleting.id)
                    habitToDelete = null
                    snackbarHostState.showSnackbar("Habit deleted")
                }
            },
            onDismiss = { habitToDelete = null }
        )
    }
}

@Composable
private fun HabitDateRow(
    item: HabitForDateItem,
    consistency: HabitConsistencyItem?,
    isFutureDate: Boolean,
    onToggleCompleted: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    val colors = LaTheme.colors
    val tints = colors.tints
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing
    val radii = LaTheme.radii

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isFutureDate) { onToggleCompleted(!item.isCompleted) }
            .padding(vertical = spacing.space1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .semantics { role = Role.Checkbox }
                .padding(end = spacing.space2)
        ) {
            LaAnimatedCheckbox(
                checked = item.isCompleted,
                enabled = !isFutureDate,
                onCheckedChange = onToggleCompleted
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.habit.name,
                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(spacing.space1))
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.habit.lifeArea?.let { area ->
                    Surface(
                        shape = radii.shapeFull,
                        color = tints.lavender.base
                    ) {
                        Text(
                            text = area.shortName,
                            style = typography.chartMicroLabel,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                val scheduleLabel = formatScheduleLabel(item.schedulePeriod?.weekdays.orEmpty())
                Surface(
                    shape = radii.shapeFull,
                    color = tints.butter.base
                ) {
                    Text(
                        text = scheduleLabel,
                        style = typography.chartMicroLabel,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                if (consistency != null && consistency.totalDueOccurrences > 0) {
                    Text(
                        text = "30d: ${consistency.completedDueOccurrences}/${consistency.totalDueOccurrences} (${consistency.consistencyPercent}%)",
                        style = typography.label,
                        color = colors.textSecondary
                    )
                }
            }
        }
        LaCircularIconButton(
            icon = Icons.Outlined.Edit,
            contentDescription = "Edit ${item.habit.name}",
            onClick = onEdit,
            containerColor = colors.surfaceVariant,
            contentColor = colors.textSecondary
        )
    }
}

@Composable
private fun HabitNotScheduledRow(
    item: HabitForDateItem,
    consistency: HabitConsistencyItem?,
    onEdit: () -> Unit
) {
    val colors = LaTheme.colors
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.habit.name,
                style = typography.body.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            val areaPrefix = item.habit.lifeArea?.let { "${it.displayName} • " } ?: ""
            val scheduleLabel = formatScheduleLabel(item.schedulePeriod?.weekdays.orEmpty())
            val consistencyText = if (consistency != null && consistency.totalDueOccurrences > 0) {
                " • 30d: ${consistency.completedDueOccurrences}/${consistency.totalDueOccurrences} (${consistency.consistencyPercent}%)"
            } else ""
            Text(
                text = "${areaPrefix}Not scheduled today ($scheduleLabel)$consistencyText",
                style = typography.label,
                color = colors.textSecondary
            )
        }
        LaCircularIconButton(
            icon = Icons.Outlined.Edit,
            contentDescription = "Edit ${item.habit.name}",
            onClick = onEdit,
            containerColor = colors.surfaceVariant,
            contentColor = colors.textSecondary
        )
    }
}

@Composable
private fun InactiveHabitRow(
    habit: Habit,
    consistency: HabitConsistencyItem?,
    onDelete: () -> Unit
) {
    val colors = LaTheme.colors
    val tints = colors.tints
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = typography.body,
                color = colors.textSecondary
            )
            val areaPrefix = habit.lifeArea?.let { "${it.displayName} • " } ?: ""
            val deactivatedText = habit.deactivatedFromLocalDate?.let {
                "Deactivated ${DateAttributionPolicy.formatShortDate(it)}"
            } ?: "Inactive"
            val consistencyText = if (consistency != null && consistency.totalDueOccurrences > 0) {
                " • 30d: ${consistency.completedDueOccurrences}/${consistency.totalDueOccurrences}"
            } else ""
            Text(
                text = "$areaPrefix$deactivatedText$consistencyText",
                style = typography.label,
                color = colors.textSecondary
            )
        }
        LaCircularIconButton(
            icon = Icons.Outlined.DeleteOutline,
            contentDescription = "Delete ${habit.name}",
            onClick = onDelete,
            containerColor = tints.rose.soft,
            contentColor = colors.error
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HabitFormDialog(
    initialHabit: Habit?,
    effectiveDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (name: String, weekdays: Set<DayOfWeek>, lifeArea: com.lifeanalytics.app.domain.model.LifeArea) -> Unit,
    onDeactivate: (() -> Unit)?,
    onRequestDelete: (() -> Unit)?,
    defaultLifeArea: com.lifeanalytics.app.domain.model.LifeArea? = null
) {
    val colors = LaTheme.colors
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing
    val radii = LaTheme.radii
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentPeriod = initialHabit?.effectiveScheduleOn(effectiveDate)
        ?: initialHabit?.schedulePeriods?.lastOrNull()
    var name by rememberSaveable { mutableStateOf(initialHabit?.name ?: "") }
    var selectedLifeArea by rememberSaveable {
        mutableStateOf<com.lifeanalytics.app.domain.model.LifeArea?>(initialHabit?.lifeArea ?: defaultLifeArea)
    }
    var selectedDays by remember {
        mutableStateOf(
            currentPeriod?.weekdays?.takeIf { it.isNotEmpty() } ?: DayOfWeek.entries.toSet()
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val everydaySet = DayOfWeek.entries.toSet()
    val weekdaysSet = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )
    val weekendsSet = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = radii.shapeXlTop,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.cardPaddingLarge)
                .padding(bottom = spacing.space6)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            Text(
                text = if (initialHabit == null) "Add habit" else "Edit habit",
                style = typography.sectionTitle,
                color = colors.textPrimary
            )

            LaTextField(
                value = name,
                onValueChange = {
                    name = it
                    validationError = null
                },
                label = "Habit name",
                placeholder = "e.g., Read 20 pages, Exercise, English practice",
                singleLine = true
            )

            Text(
                text = "Life Area *",
                style = typography.label,
                color = colors.textSecondary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                com.lifeanalytics.app.domain.model.LifeArea.entries.forEach { area ->
                    LaFilterChip(
                        label = area.displayName,
                        selected = selectedLifeArea == area,
                        onClick = {
                            selectedLifeArea = area
                            validationError = null
                        }
                    )
                }
            }

            Text(
                text = "Quick schedule",
                style = typography.label,
                color = colors.textSecondary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                LaFilterChip(
                    label = "Every day",
                    selected = selectedDays == everydaySet,
                    onClick = {
                        selectedDays = everydaySet
                        validationError = null
                    }
                )
                LaFilterChip(
                    label = "Weekdays",
                    selected = selectedDays == weekdaysSet,
                    onClick = {
                        selectedDays = weekdaysSet
                        validationError = null
                    }
                )
                LaFilterChip(
                    label = "Weekends",
                    selected = selectedDays == weekendsSet,
                    onClick = {
                        selectedDays = weekendsSet
                        validationError = null
                    }
                )
            }

            Text(
                text = "Scheduled weekdays",
                style = typography.label,
                color = colors.textSecondary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DayOfWeek.entries.forEach { day ->
                    val isSelected = selectedDays.contains(day)
                    LaFilterChip(
                        label = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        selected = isSelected,
                        onClick = {
                            selectedDays = if (isSelected) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                            validationError = null
                        }
                    )
                }
            }

            if (initialHabit != null) {
                Text(
                    text = "Schedule edits apply from today forward and preserve past schedule history.",
                    style = typography.label,
                    color = colors.textSecondary
                )
            }

            if (onDeactivate != null || onRequestDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (onDeactivate != null) {
                        LaTextButton(
                            text = "Deactivate habit",
                            onClick = onDeactivate
                        )
                    }
                    if (onRequestDelete != null) {
                        LaTextButton(
                            text = "Delete",
                            onClick = onRequestDelete,
                            color = colors.error
                        )
                    }
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = typography.label,
                    color = colors.error
                )
            }

            Spacer(modifier = Modifier.height(spacing.space2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                LaSecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                LaPrimaryButton(
                    text = if (initialHabit == null) "Save habit" else "Update habit",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isEmpty()) {
                            validationError = "Enter a habit name to continue."
                            return@LaPrimaryButton
                        }
                        val area = selectedLifeArea
                        if (area == null) {
                            validationError = "Select a Life Area (Health & Lifestyle, Career & Productivity, or Finance & Wealth)."
                            return@LaPrimaryButton
                        }
                        if (selectedDays.isEmpty()) {
                            validationError = "Select at least one scheduled weekday."
                            return@LaPrimaryButton
                        }
                        onSave(trimmed, selectedDays, area)
                    }
                )
            }
        }
    }
}

fun formatScheduleLabel(weekdays: Set<DayOfWeek>): String {
    if (weekdays.isEmpty()) return "Not scheduled"
    return when (HabitScheduleType.fromWeekdays(weekdays)) {
        HabitScheduleType.EVERY_DAY -> "Every day"
        HabitScheduleType.WEEKDAYS -> "Weekdays"
        HabitScheduleType.WEEKENDS -> "Weekends"
        HabitScheduleType.CUSTOM -> weekdays
            .sortedBy { it.value }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
    }
}
