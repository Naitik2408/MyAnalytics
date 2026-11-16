package com.lifeanalytics.app.feature.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.lifeanalytics.app.core.designsystem.EmptyStateCard
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LocalExtendedColors
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.FocusSession
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.FocusSessionType
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class StartFocusMode(val label: String) {
    EXISTING_TASK("Choose a planned task"),
    NEW_TASK("Create a task and start"),
    GENERAL("Start general focus")
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    repository: LifeAnalyticsRepository,
    today: LocalDate,
    preselectedTaskId: String? = null,
    onConsumedPreselectedTask: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
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
    var newTaskCategory by rememberSaveable { mutableStateOf(TaskCategory.STUDY) }
    var generalDescription by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isActionInProgress by remember { mutableStateOf(false) }

    // Check session interruption on launch and keep heartbeat updated while active
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
    val todayFocusedSeconds = remember(historySessions, today) {
        DomainCalculations.sumCompletedFocusDurationSeconds(
            historySessions.filter { it.startedLocalDate == today }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Work & Focus") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
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
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = { statusMessage = null }) {
                            Text("OK")
                        }
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
                                        statusMessage = "Focus session saved: ${DateAttributionPolicy.formatSummaryDuration(finished.focusedDurationSeconds)} recorded."
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Start a focus session",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Paused time is excluded automatically. Recorded focus time measures active timer intervals.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StartFocusMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = startMode == mode,
                                    onClick = {
                                        startMode = mode
                                        errorMessage = null
                                    },
                                    label = { Text(mode.label) }
                                )
                            }
                        }

                        when (startMode) {
                            StartFocusMode.EXISTING_TASK -> {
                                if (incompleteTasks.isEmpty()) {
                                    Text(
                                        text = "No incomplete tasks planned for today. You can create a task and start, or start general focus.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    val effectiveTaskId = selectedTaskId?.takeIf { id ->
                                        incompleteTasks.any { it.id == id }
                                    } ?: incompleteTasks.first().id

                                    Text(
                                        text = "Select a planned task:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    incompleteTasks.forEach { task ->
                                        val isSelected = task.id == effectiveTaskId
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedTaskId = task.id },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = task.title,
                                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = task.category.displayName,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (isSelected) {
                                                    Text(
                                                        text = "Selected",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        enabled = !isActionInProgress,
                                        onClick = {
                                            isActionInProgress = true
                                            errorMessage = null
                                            scope.launch {
                                                repository.startFocusSession(taskId = effectiveTaskId)
                                                    .onFailure { errorMessage = it.message }
                                                isActionInProgress = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Start focus on selected task")
                                    }
                                }
                            }

                            StartFocusMode.NEW_TASK -> {
                                OutlinedTextField(
                                    value = newTaskTitle,
                                    onValueChange = {
                                        newTaskTitle = it
                                        errorMessage = null
                                    },
                                    label = { Text("New task name (required)") },
                                    placeholder = { Text("e.g. Write design summary") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TaskCategory.entries.forEach { cat ->
                                        FilterChip(
                                            selected = newTaskCategory == cat,
                                            onClick = { newTaskCategory = cat },
                                            label = { Text(cat.displayName) }
                                        )
                                    }
                                }
                                Button(
                                    enabled = !isActionInProgress,
                                    onClick = {
                                        isActionInProgress = true
                                        errorMessage = null
                                        scope.launch {
                                            repository.createTaskAndStartFocus(
                                                title = newTaskTitle,
                                                category = newTaskCategory,
                                                plannedDate = today
                                            ).onSuccess {
                                                newTaskTitle = ""
                                            }.onFailure {
                                                errorMessage = it.message
                                            }
                                            isActionInProgress = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save task and start focus")
                                }
                            }

                            StartFocusMode.GENERAL -> {
                                OutlinedTextField(
                                    value = generalDescription,
                                    onValueChange = {
                                        generalDescription = it
                                        errorMessage = null
                                    },
                                    label = { Text("Activity description (optional)") },
                                    placeholder = { Text("e.g. Reading or Deep work") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    enabled = !isActionInProgress,
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
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start general focus")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Focus history (Today: ${DateAttributionPolicy.formatSummaryDuration(todayFocusedSeconds)})"
            )
        }

        if (historySessions.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No focus sessions yet",
                    message = "Start a session linked to a planned task or as general focus. Completed sessions will appear here."
                )
            }
        } else {
            items(historySessions, key = { it.id }) { session ->
                FocusHistoryRow(session = session)
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isPaused) {
                    LocalExtendedColors.current.warning.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                }
            ) {
                Text(
                    text = if (isPaused) "PAUSED — Paused time is not counted" else "FOCUS SESSION RUNNING",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isPaused) LocalExtendedColors.current.warning else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = DateAttributionPolicy.formatLiveTimer(liveFocusedSeconds),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Focused duration (pauses excluded)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Working on",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = session.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (session.sessionType == FocusSessionType.TASK_LINKED) "Planned task" else "General focus",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isPaused) {
                    Button(
                        enabled = enabled,
                        onClick = onResume,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume session")
                    }
                } else {
                    OutlinedButton(
                        enabled = enabled,
                        onClick = onPause,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause session")
                    }
                }

                Button(
                    enabled = enabled,
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Finish session")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Started at ${DateAttributionPolicy.formatInstantTime(session.startedAtEpochMillis)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val warningColor = LocalExtendedColors.current.warning
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, warningColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Session interrupted",
                    tint = warningColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Resume your focus session?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = session.interruptionReason
                    ?: "Your previous focus session was interrupted and its timer could not be verified across the gap.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Activity: ${session.displayTitle} · Confirmed focused time: ${DateAttributionPolicy.formatSummaryDuration(session.focusedDurationSeconds)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Unverified interruption gaps are never counted as focused time.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    enabled = enabled,
                    onClick = onFinishAtLastConfirmed,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Finish at confirmed time")
                }
                Button(
                    enabled = enabled,
                    onClick = onResumeFromNow,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resume from now")
                }
            }
        }
    }
}

@Composable
private fun FocusHistoryRow(session: FocusSession) {
    val extendedColors = LocalExtendedColors.current
    val startTimeText = DateAttributionPolicy.formatInstantTime(session.startedAtEpochMillis)
    val endTimeText = session.endedAtEpochMillis?.let { DateAttributionPolicy.formatInstantTime(it) } ?: ""
    val typeText = if (session.sessionType == FocusSessionType.TASK_LINKED) "Task-linked" else "General focus"
    val statusBadge = if (session.interruptionReason != null) "Recovered" else "Completed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.displayTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${DateAttributionPolicy.formatShortDate(session.startedLocalDate)} · $startTimeText${if (endTimeText.isNotEmpty()) " – $endTimeText" else ""} · $typeText",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.interruptionReason != null) {
                    Text(
                        text = statusBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = extendedColors.warning
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = DateAttributionPolicy.formatSummaryDuration(session.focusedDurationSeconds),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = extendedColors.success,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = statusBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
