package com.lifeanalytics.app.feature.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaFilterChip
import com.lifeanalytics.app.core.designsystem.LaHeroPanel
import com.lifeanalytics.app.core.designsystem.LaModuleEmptyState
import com.lifeanalytics.app.core.designsystem.LaPrimaryButton
import com.lifeanalytics.app.core.designsystem.LaRowDivider
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaSecondaryButton
import com.lifeanalytics.app.core.designsystem.LaSessionTimeline
import com.lifeanalytics.app.core.designsystem.LaTextField
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.LaTimelineSegment
import com.lifeanalytics.app.core.designsystem.LaTintedTile
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.FocusSession
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.FocusSessionType
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class StartFocusMode(val label: String) {
    EXISTING_TASK("Planned task"),
    NEW_TASK("New task"),
    GENERAL("General focus")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(
    repository: LifeAnalyticsRepository,
    today: LocalDate,
    preselectedTaskId: String? = null,
    onConsumedPreselectedTask: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val colors = LaTheme.colors
    val tints = colors.tints
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing
    val radii = LaTheme.radii
    val scope = rememberCoroutineScope()

    val unresolvedSession by repository.observeUnresolvedSession()
        .collectAsStateWithLifecycle(initialValue = null)
    val allSessions by repository.observeAllSessions()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val todayTasks by repository.observeTasksForDate(today)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val incompleteTasks = remember(todayTasks) { todayTasks.filter { !it.isCompleted } }

    var startMode by rememberSaveable { mutableStateOf(StartFocusMode.EXISTING_TASK) }
    var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var newTaskTitle by rememberSaveable { mutableStateOf("") }
    var newTaskLifeArea by rememberSaveable { mutableStateOf<com.lifeanalytics.app.domain.model.LifeArea?>(null) }
    var newTaskCategory by rememberSaveable { mutableStateOf(TaskCategory.STUDY) }
    var generalDescription by rememberSaveable { mutableStateOf("") }
    var selectedPresetMinutes by rememberSaveable { mutableStateOf(25) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isActionInProgress by remember { mutableStateOf(false) }

    var liveNowEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        repository.checkOrConfirmActiveSession()
    }

    LaunchedEffect(preselectedTaskId, unresolvedSession) {
        if (preselectedTaskId != null) {
            if (unresolvedSession != null) {
                errorMessage = "A focus session is already in progress. Return to or finish the current session first."
                onConsumedPreselectedTask()
            } else {
                startMode = StartFocusMode.EXISTING_TASK
                selectedTaskId = preselectedTaskId
                onConsumedPreselectedTask()
            }
        }
    }

    LaunchedEffect(unresolvedSession?.id, unresolvedSession?.status) {
        val session = unresolvedSession ?: return@LaunchedEffect
        if (session.status == FocusSessionStatus.ACTIVE) {
            var tickCount = 0
            while (true) {
                liveNowEpochMillis = System.currentTimeMillis()
                tickCount++
                if (tickCount % 15 == 0) {
                    repository.checkOrConfirmActiveSession()
                }
                delay(1_000L)
            }
        } else {
            liveNowEpochMillis = System.currentTimeMillis()
        }
    }

    val historySessions = remember(allSessions) {
        allSessions.filter { it.status == FocusSessionStatus.COMPLETED }
    }
    val todayCompletedSessions = remember(historySessions, today) {
        historySessions.filter { it.startedLocalDate == today }
    }
    val todayFocusedSeconds = remember(todayCompletedSessions) {
        DomainCalculations.sumCompletedFocusDurationSeconds(todayCompletedSessions)
    }

    val timelineSegments = remember(todayCompletedSessions) {
        val zone = ZoneId.systemDefault()
        todayCompletedSessions.mapIndexed { idx, session ->
            val startZdt = Instant.ofEpochMilli(session.startedAtEpochMillis).atZone(zone)
            val endMillis = session.endedAtEpochMillis
                ?: (session.startedAtEpochMillis + session.focusedDurationSeconds * 1000L)
            val endZdt = Instant.ofEpochMilli(endMillis).atZone(zone)
            val startHour = startZdt.hour + startZdt.minute / 60f
            val endHour = (endZdt.hour + endZdt.minute / 60f).coerceAtLeast(startHour + 0.25f)
            LaTimelineSegment(
                startHourFraction = startHour,
                endHourFraction = endHour.coerceAtMost(24f),
                label = session.displayTitle,
                isHighlighted = idx == 0
            )
        }
    }

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
                title = "Active Work & Focus",
                subtitle = "Today's recorded focus: ${DateAttributionPolicy.formatSummaryDuration(todayFocusedSeconds)}",
                onNavigateBack = onNavigateBack
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

        if (statusMessage != null) {
            item {
                LaTintedTile(
                    containerColor = tints.mint.base,
                    shape = radii.shapeMd
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Session saved",
                                style = typography.cardTitle,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = statusMessage!!,
                                style = typography.body,
                                color = colors.onTintSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(spacing.space2))
                        LaSecondaryButton(
                            text = "Done",
                            onClick = { statusMessage = null },
                            containerColor = colors.surface.copy(alpha = 0.85f),
                            contentColor = colors.textPrimary
                        )
                    }
                }
            }
        }

        val currentSession = unresolvedSession
        if (currentSession != null) {
            if (currentSession.status == FocusSessionStatus.INTERRUPTED) {
                item {
                    InterruptedSessionRecoveryCard(
                        session = currentSession,
                        enabled = !isActionInProgress,
                        onFinishAtLastConfirmed = {
                            isActionInProgress = true
                            scope.launch {
                                repository.recoverSessionFinishAtLastConfirmed(currentSession.id)
                                    .onSuccess { finished ->
                                        statusMessage = "Saved recovered session (${DateAttributionPolicy.formatSummaryDuration(finished.focusedDurationSeconds)})."
                                    }
                                    .onFailure { errorMessage = it.message }
                                isActionInProgress = false
                            }
                        },
                        onResumeFromNow = {
                            isActionInProgress = true
                            scope.launch {
                                repository.recoverSessionResumeFromNow(currentSession.id)
                                    .onSuccess {
                                        statusMessage = "Session resumed from now. The unverified interruption gap was excluded."
                                    }
                                    .onFailure { errorMessage = it.message }
                                isActionInProgress = false
                            }
                        }
                    )
                }
            } else {
                item {
                    ActiveFocusTimerCard(
                        session = currentSession,
                        nowEpochMillis = liveNowEpochMillis,
                        enabled = !isActionInProgress,
                        onPause = {
                            isActionInProgress = true
                            scope.launch {
                                repository.pauseFocusSession(currentSession.id)
                                    .onFailure { errorMessage = it.message }
                                isActionInProgress = false
                            }
                        },
                        onResume = {
                            isActionInProgress = true
                            scope.launch {
                                repository.resumeFocusSession(currentSession.id)
                                    .onFailure { errorMessage = it.message }
                                isActionInProgress = false
                            }
                        },
                        onFinish = {
                            isActionInProgress = true
                            scope.launch {
                                repository.finishFocusSession(currentSession.id)
                                    .onSuccess { finished ->
                                        statusMessage = "${finished.displayTitle} · ${DateAttributionPolicy.formatSummaryDuration(finished.focusedDurationSeconds)} recorded."
                                    }
                                    .onFailure { errorMessage = it.message }
                                isActionInProgress = false
                            }
                        }
                    )
                }
            }
        } else {
            item {
                LaHeroPanel(
                    title = "Start a focus session",
                    subtitle = "Paused time is excluded automatically",
                    icon = Icons.Outlined.Timer,
                    containerColor = tints.mint.strong
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                        verticalArrangement = Arrangement.spacedBy(spacing.space2)
                    ) {
                        StartFocusMode.entries.forEach { mode ->
                            LaFilterChip(
                                label = mode.label,
                                selected = startMode == mode,
                                onClick = {
                                    startMode = mode
                                    errorMessage = null
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.space3))

                    Text(
                        text = "Session pace reference",
                        style = typography.label,
                        color = colors.onTintSecondary
                    )
                    Spacer(modifier = Modifier.height(spacing.space1))
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                        listOf(15, 25, 45, 60).forEach { preset ->
                            val isPresetSelected = selectedPresetMinutes == preset
                            Surface(
                                shape = radii.shapeFull,
                                color = if (isPresetSelected) colors.ink else colors.surface.copy(alpha = 0.72f),
                                modifier = Modifier.clickable { selectedPresetMinutes = preset }
                            ) {
                                Text(
                                    text = "${preset}m",
                                    style = typography.label.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isPresetSelected) colors.onInk else colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = spacing.space3, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.space3))

                    when (startMode) {
                        StartFocusMode.EXISTING_TASK -> {
                            if (incompleteTasks.isEmpty()) {
                                Text(
                                    text = "No incomplete tasks planned for today. Switch to New task or General focus.",
                                    style = typography.body,
                                    color = colors.onTintSecondary
                                )
                                Spacer(modifier = Modifier.height(spacing.space3))
                                LaPrimaryButton(
                                    text = "Start general focus",
                                    icon = Icons.Outlined.PlayArrow,
                                    enabled = !isActionInProgress,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        isActionInProgress = true
                                        errorMessage = null
                                        scope.launch {
                                            repository.startFocusSession(
                                                taskId = null,
                                                generalDescription = "General focus"
                                            ).onFailure {
                                                errorMessage = it.message
                                            }
                                            isActionInProgress = false
                                        }
                                    }
                                )
                            } else {
                                val effectiveTaskId = selectedTaskId?.takeIf { id ->
                                    incompleteTasks.any { it.id == id }
                                } ?: incompleteTasks.first().id

                                Text(
                                    text = "Select a planned task:",
                                    style = typography.label,
                                    color = colors.onTintSecondary
                                )
                                Spacer(modifier = Modifier.height(spacing.space2))

                                Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                                    incompleteTasks.forEach { task ->
                                        val isSelected = task.id == effectiveTaskId
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedTaskId = task.id },
                                            shape = radii.shapeSm,
                                            color = if (isSelected) colors.surface else colors.surface.copy(alpha = 0.65f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = spacing.space4, vertical = spacing.space3),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = task.title,
                                                        style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                                        color = colors.textPrimary
                                                    )
                                                    Text(
                                                        text = task.category.displayName,
                                                        style = typography.label,
                                                        color = colors.textSecondary
                                                    )
                                                }
                                                if (isSelected) {
                                                    Surface(
                                                        shape = radii.shapeFull,
                                                        color = colors.ink
                                                    ) {
                                                        Text(
                                                            text = "Selected",
                                                            style = typography.chartMicroLabel,
                                                            color = colors.onInk,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(spacing.space4))
                                LaPrimaryButton(
                                    text = "Start focus",
                                    icon = Icons.Outlined.PlayArrow,
                                    enabled = !isActionInProgress,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        isActionInProgress = true
                                        errorMessage = null
                                        scope.launch {
                                            repository.startFocusSession(taskId = effectiveTaskId)
                                                .onFailure { errorMessage = it.message }
                                            isActionInProgress = false
                                        }
                                    }
                                )
                            }
                        }

                        StartFocusMode.NEW_TASK -> {
                            Surface(
                                shape = radii.shapeMd,
                                color = colors.surface.copy(alpha = 0.85f)
                            ) {
                                Column(modifier = Modifier.padding(spacing.space3)) {
                                    LaTextField(
                                        value = newTaskTitle,
                                        onValueChange = {
                                            newTaskTitle = it
                                            errorMessage = null
                                        },
                                        label = "New task name",
                                        placeholder = "e.g. Write design summary",
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(spacing.space2))
                                    Text(
                                        text = "Life Area *",
                                        style = typography.label,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(spacing.space1))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                                        verticalArrangement = Arrangement.spacedBy(spacing.space2)
                                    ) {
                                        com.lifeanalytics.app.domain.model.LifeArea.entries.forEach { area ->
                                            LaFilterChip(
                                                label = area.displayName,
                                                selected = newTaskLifeArea == area,
                                                onClick = {
                                                    newTaskLifeArea = area
                                                    errorMessage = null
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(spacing.space2))
                                    Text(
                                        text = "Category",
                                        style = typography.label,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(spacing.space1))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                                        verticalArrangement = Arrangement.spacedBy(spacing.space2)
                                    ) {
                                        TaskCategory.entries.forEach { cat ->
                                            LaFilterChip(
                                                label = cat.displayName,
                                                selected = newTaskCategory == cat,
                                                onClick = { newTaskCategory = cat }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(spacing.space3))
                            LaPrimaryButton(
                                text = "Save task and start focus",
                                icon = Icons.Outlined.PlayArrow,
                                enabled = !isActionInProgress,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val chosenArea = newTaskLifeArea
                                    if (chosenArea == null) {
                                        errorMessage = "Select a Life Area before saving the new task."
                                        return@LaPrimaryButton
                                    }
                                    isActionInProgress = true
                                    errorMessage = null
                                    scope.launch {
                                        repository.createTaskAndStartFocus(
                                            title = newTaskTitle,
                                            category = newTaskCategory,
                                            plannedDate = today,
                                            lifeArea = chosenArea
                                        ).onSuccess {
                                            newTaskTitle = ""
                                        }.onFailure {
                                            errorMessage = it.message
                                        }
                                        isActionInProgress = false
                                    }
                                }
                            )
                        }

                        StartFocusMode.GENERAL -> {
                            Surface(
                                shape = radii.shapeMd,
                                color = colors.surface.copy(alpha = 0.85f)
                            ) {
                                Column(modifier = Modifier.padding(spacing.space3)) {
                                    LaTextField(
                                        value = generalDescription,
                                        onValueChange = {
                                            generalDescription = it
                                            errorMessage = null
                                        },
                                        label = "Activity description (optional)",
                                        placeholder = "e.g. Reading or Deep work",
                                        singleLine = true
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(spacing.space3))
                            LaPrimaryButton(
                                text = "Start general focus",
                                icon = Icons.Outlined.PlayArrow,
                                enabled = !isActionInProgress,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    isActionInProgress = true
                                    errorMessage = null
                                    scope.launch {
                                        repository.startFocusSession(
                                            taskId = null,
                                            generalDescription = generalDescription
                                        ).onSuccess {
                                            generalDescription = ""
                                        }.onFailure {
                                            errorMessage = it.message
                                        }
                                        isActionInProgress = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Today's Session Timeline (Section 11.2.5)
        item {
            LaContainerPanel(
                title = "Today's focus timeline",
                subtitle = "Today's sessions, excluding pauses"
            ) {
                LaSessionTimeline(
                    segments = timelineSegments,
                    tint = tints.mint,
                    accessibilitySummary = "Today's focus timeline with ${timelineSegments.size} completed session(s)."
                )
            }
        }

        item {
            SectionHeader(
                title = "Focus history (Today: ${DateAttributionPolicy.formatSummaryDuration(todayFocusedSeconds)})"
            )
        }

        if (historySessions.isEmpty()) {
            item {
                LaContainerPanel {
                    LaModuleEmptyState(
                        icon = Icons.Outlined.Timer,
                        iconTint = tints.mint.base,
                        title = "No focus sessions yet",
                        message = "Start a session linked to a planned task or as general focus. Completed sessions will appear here."
                    )
                }
            }
        } else {
            item {
                LaContainerPanel {
                    historySessions.forEachIndexed { index, session ->
                        FocusHistoryRow(session = session)
                        if (index < historySessions.lastIndex) {
                            LaRowDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFocusTimerCard(
    session: FocusSession,
    nowEpochMillis: Long,
    enabled: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit
) {
    val colors = LaTheme.colors
    val tints = colors.tints
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing
    val radii = LaTheme.radii

    val isPaused = session.status == FocusSessionStatus.PAUSED
    val effectiveEndMillis = if (isPaused) {
        session.pauses.lastOrNull { it.resumedAtEpochMillis == null }?.pausedAtEpochMillis ?: nowEpochMillis
    } else {
        maxOf(nowEpochMillis, session.startedAtEpochMillis)
    }
    val liveFocusedSeconds = DomainCalculations.calculateFocusedDurationSeconds(
        startedAtEpochMillis = session.startedAtEpochMillis,
        endOrNowEpochMillis = effectiveEndMillis,
        pauses = session.pauses
    )

    LaHeroPanel(
        title = if (isPaused) "PAUSED — Paused time is not counted" else "FOCUS SESSION RUNNING",
        subtitle = "Started at ${DateAttributionPolicy.formatInstantTime(session.startedAtEpochMillis)}",
        icon = Icons.Outlined.Timer,
        containerColor = tints.mint.strong
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = DateAttributionPolicy.formatLiveTimer(liveFocusedSeconds),
                style = typography.timerValue,
                color = colors.textPrimary
            )
            Text(
                text = "Focused duration (pauses excluded)",
                style = typography.label,
                color = colors.onTintSecondary
            )

            Spacer(modifier = Modifier.height(spacing.space4))

            Text(
                text = "Working on",
                style = typography.label,
                color = colors.onTintSecondary
            )
            Text(
                text = session.displayTitle,
                style = typography.sectionTitle,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (session.sessionType == FocusSessionType.TASK_LINKED) "Planned task" else "General focus",
                style = typography.label,
                color = colors.onTintSecondary
            )

            Spacer(modifier = Modifier.height(spacing.space5))

            // Section 17: Active Timer Hero uses primary ink button for Pause/Resume and tonal button for Finish
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3)
            ) {
                if (isPaused) {
                    LaPrimaryButton(
                        text = "Resume session",
                        icon = Icons.Outlined.PlayArrow,
                        enabled = enabled,
                        onClick = onResume,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LaPrimaryButton(
                        text = "Pause session",
                        icon = Icons.Outlined.Pause,
                        enabled = enabled,
                        onClick = onPause,
                        modifier = Modifier.weight(1f)
                    )
                }

                LaSecondaryButton(
                    text = "Finish session",
                    icon = Icons.Outlined.Stop,
                    enabled = enabled,
                    onClick = onFinish,
                    containerColor = colors.surface.copy(alpha = 0.85f),
                    contentColor = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InterruptedSessionRecoveryCard(
    session: FocusSession,
    enabled: Boolean,
    onFinishAtLastConfirmed: () -> Unit,
    onResumeFromNow: () -> Unit
) {
    val colors = LaTheme.colors
    val tints = colors.tints
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing
    val radii = LaTheme.radii

    LaTintedTile(
        containerColor = tints.butter.base,
        shape = radii.shapeLg
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = "Session interrupted",
                tint = colors.warning
            )
            Spacer(modifier = Modifier.width(spacing.space2))
            Text(
                text = "Resume your focus session?",
                style = typography.sectionTitle,
                color = colors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(spacing.space2))
        Text(
            text = session.interruptionReason
                ?: "Your previous focus session was interrupted and its timer could not be verified across the gap.",
            style = typography.body,
            color = colors.onTintSecondary
        )
        Spacer(modifier = Modifier.height(spacing.space2))
        Text(
            text = "Activity: ${session.displayTitle} · Confirmed focused time: ${DateAttributionPolicy.formatSummaryDuration(session.focusedDurationSeconds)}",
            style = typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(spacing.space1))
        Text(
            text = "Unverified interruption gaps are never counted as focused time.",
            style = typography.label,
            color = colors.onTintSecondary
        )
        Spacer(modifier = Modifier.height(spacing.space4))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            LaSecondaryButton(
                text = "Finish at confirmed time",
                enabled = enabled,
                onClick = onFinishAtLastConfirmed,
                containerColor = colors.surface.copy(alpha = 0.85f),
                contentColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            LaPrimaryButton(
                text = "Resume from now",
                enabled = enabled,
                onClick = onResumeFromNow,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FocusHistoryRow(session: FocusSession) {
    val colors = LaTheme.colors
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing

    val startTimeText = DateAttributionPolicy.formatInstantTime(session.startedAtEpochMillis)
    val endTimeText = session.endedAtEpochMillis?.let { DateAttributionPolicy.formatInstantTime(it) } ?: ""
    val typeText = if (session.sessionType == FocusSessionType.TASK_LINKED) "Task-linked" else "General focus"
    val statusBadge = if (session.interruptionReason != null) "Recovered" else "Completed"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.space2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.displayTitle,
                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${DateAttributionPolicy.formatShortDate(session.startedLocalDate)} · $startTimeText${if (endTimeText.isNotEmpty()) " – $endTimeText" else ""} · $typeText",
                style = typography.label,
                color = colors.textSecondary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = DateAttributionPolicy.formatSummaryDuration(session.focusedDurationSeconds),
                style = typography.sectionTitle,
                color = colors.textPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircleOutline,
                    contentDescription = null,
                    tint = if (session.interruptionReason != null) colors.warning else colors.success,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 3.dp)
                )
                Text(
                    text = statusBadge,
                    style = typography.label,
                    color = colors.textSecondary
                )
            }
        }
    }
}
