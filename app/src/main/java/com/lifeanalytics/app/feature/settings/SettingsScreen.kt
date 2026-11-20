package com.lifeanalytics.app.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.common.SupportedCurrency
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaFilterChip
import com.lifeanalytics.app.core.designsystem.LaIconChip
import com.lifeanalytics.app.core.designsystem.LaPrimaryButton
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaSecondaryButton
import com.lifeanalytics.app.core.designsystem.LaSegmentedControl
import com.lifeanalytics.app.core.designsystem.LaTextField
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.pressScaleClickable
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.backup.BackupManager
import com.lifeanalytics.app.data.backup.BackupPayload
import com.lifeanalytics.app.data.backup.BackupRecordCounts
import com.lifeanalytics.app.data.backup.BackupSerializer
import com.lifeanalytics.app.data.backup.CloudBackupMetadata
import com.lifeanalytics.app.data.backup.RestoreMode
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.data.repository.PreferencesRepository
import com.lifeanalytics.app.domain.model.AppPreferences
import com.lifeanalytics.app.domain.model.AppThemeOption
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    preferencesRepository: PreferencesRepository,
    repository: LifeAnalyticsRepository,
    backupManager: BackupManager? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    val colors = LaTheme.colors
    val tints = colors.tints
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing
    val radii = LaTheme.radii

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCurrencyChange by remember { mutableStateOf<SupportedCurrency?>(null) }

    val backupStatus = backupManager?.statusState?.collectAsState()?.value

    // Sheet / Dialog states
    var showAuthSheet by remember { mutableStateOf(false) }
    var showPassphraseDialog by remember { mutableStateOf(false) }
    var restorePreviewPayload by remember { mutableStateOf<BackupPayload?>(null) }
    var pendingRestoreSourceDescription by remember { mutableStateOf("") }
    var askPassphraseForRestore by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var pendingArchiveJsonToDecrypt by remember { mutableStateOf<String?>(null) }
    var pendingCloudMetadataToDecrypt by remember { mutableStateOf<CloudBackupMetadata?>(null) }
    var localExportPendingJson by remember { mutableStateOf<String?>(null) }

    // Android Storage Access Framework (SAF) file pickers for local .labackup export/import
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && localExportPendingJson != null && backupManager != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(localExportPendingJson!!.toByteArray(StandardCharsets.UTF_8))
                    }
                    backupManager.recordLocalExportSuccess(System.currentTimeMillis())
                }.onFailure { err ->
                    backupManager.updateStatusMessage(null, "Local export failed: ${err.message}")
                }
                localExportPendingJson = null
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && backupManager != null) {
            scope.launch {
                runCatching {
                    val rawJson = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
                    } ?: throw IllegalStateException("Could not read selected backup file.")

                    val cached = backupManager.getCachedPassphrase()
                    if (!cached.isNullOrBlank()) {
                        val payloadResult = backupManager.inspectAndDecryptArchive(rawJson, cached)
                        payloadResult.fold(
                            onSuccess = { payload ->
                                restorePreviewPayload = payload
                                pendingRestoreSourceDescription = "Local File (${payload.manifest.deviceLabel})"
                            },
                            onFailure = {
                                pendingArchiveJsonToDecrypt = rawJson
                            }
                        )
                    } else {
                        pendingArchiveJsonToDecrypt = rawJson
                    }
                }.onFailure { err ->
                    backupManager.updateStatusMessage(null, "Could not open backup file: ${err.message}")
                }
            }
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
                title = "Settings",
                subtitle = "Appearance, cloud backup, and privacy",
                onNavigateBack = onNavigateBack
            )
        }

        // Status or Error Banner
        if (backupStatus?.lastBackupError != null) {
            item {
                ErrorBanner(
                    message = backupStatus.lastBackupError,
                    onDismiss = { backupManager.updateStatusMessage(null, null) }
                )
            }
        } else if (backupStatus?.statusMessage != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(radii.shapeSm)
                        .background(tints.mint.soft)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = backupStatus.statusMessage,
                            style = typography.label,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }

        // Appearance & Theme Panel
        item {
            LaContainerPanel(
                title = "Appearance & Theme",
                subtitle = "Choose how Life Analytics adapts to light or dark mode. Light is the default visual theme."
            ) {
                LaSegmentedControl(
                    options = AppThemeOption.entries.toList(),
                    selectedOption = preferences.themeOption,
                    onOptionSelected = { option ->
                        scope.launch {
                            preferencesRepository.setThemeOption(option)
                        }
                    },
                    labelProvider = { it.displayName }
                )
            }
        }

        // Display Currency Panel
        item {
            LaContainerPanel(
                title = "Display Currency",
                subtitle = "All amounts use Indian Rupee (₹) with Indian digit grouping (e.g. ₹12,34,567)."
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    val currency = SupportedCurrency.INR
                    LaFilterChip(
                        label = currency.displayName,
                        selected = true,
                        onClick = {
                            scope.launch {
                                preferencesRepository.setCurrencyCode(currency.code)
                            }
                        }
                    )
                }
            }
        }

        // =====================================================================
        // CLOUD BACKUP & RESTORE (Supabase & Local .labackup)
        // =====================================================================
        if (backupManager != null && backupStatus != null) {
            item {
                LaContainerPanel(
                    title = "Cloud Backup & Recovery",
                    subtitle = "Protect your data with client-side authenticated encryption (AES-256-GCM). Uninstall the app, reinstall anytime, and restore."
                ) {
                    // Auth state banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(radii.shapeSm)
                            .background(if (backupStatus.isSignedIn) tints.lavender.soft else colors.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (backupStatus.isSignedIn) Icons.Outlined.CloudDone else Icons.Outlined.Person,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (backupStatus.isSignedIn) {
                                        "Connected: ${backupStatus.userEmail}"
                                    } else {
                                        "Cloud Backup: Not signed in"
                                    },
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (backupStatus.isSignedIn) {
                                        "Supabase Cloud Project (njfaommiwnylhdowbbic)"
                                    } else {
                                        "Sign in to store encrypted backups in cloud"
                                    },
                                    style = typography.chartMicroLabel,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        if (backupStatus.isSignedIn) {
                            LaSecondaryButton(
                                text = "Sign Out",
                                onClick = { scope.launch { backupManager.signOut() } }
                            )
                        } else {
                            LaPrimaryButton(
                                text = "Sign In / Up",
                                onClick = { showAuthSheet = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.space3))

                    // Backup Recovery Passphrase state
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(radii.shapeSm)
                            .background(colors.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (backupStatus.hasCachedPassphrase) {
                                        "Recovery Passphrase: Configured"
                                    } else {
                                        "Recovery Passphrase: Not Set"
                                    },
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Used to encrypt/decrypt backups with AES-256-GCM",
                                    style = typography.chartMicroLabel,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        LaSecondaryButton(
                            text = if (backupStatus.hasCachedPassphrase) "Change" else "Set Passphrase",
                            onClick = { showPassphraseDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.space3))

                    // Auto-Backup Daily toggle
                    if (backupStatus.isSignedIn) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(radii.shapeSm)
                                .background(colors.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Backup Daily",
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Runs in background when connected to network & battery is not low",
                                    style = typography.chartMicroLabel,
                                    color = colors.textSecondary
                                )
                            }
                            Switch(
                                checked = backupStatus.autoBackupDailyEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !backupStatus.hasCachedPassphrase) {
                                        showPassphraseDialog = true
                                    } else {
                                        backupManager.setAutoBackupDaily(enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.onInk,
                                    checkedTrackColor = colors.ink
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(spacing.space3))
                    }

                    // Last Backup Summary
                    if (backupStatus.lastBackupEpochMillis != null) {
                        val formattedDate = remember(backupStatus.lastBackupEpochMillis) {
                            val instant = Instant.ofEpochMilli(backupStatus.lastBackupEpochMillis)
                            val zdt = instant.atZone(ZoneId.systemDefault())
                            "${DateAttributionPolicy.formatDisplayDate(zdt.toLocalDate())} at " +
                                String.format("%02d:%02d", zdt.hour, zdt.minute)
                        }
                        Text(
                            text = "Last verified backup: $formattedDate (${backupStatus.lastBackupDestination ?: "Cloud"})",
                            style = typography.chartMicroLabel,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(spacing.space3))
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                    ) {
                        if (backupStatus.isBusy) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = colors.ink,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Processing...",
                                    style = typography.label,
                                    color = colors.textSecondary
                                )
                            }
                        } else {
                            LaPrimaryButton(
                                text = "Back Up Now",
                                icon = Icons.Outlined.CloudUpload,
                                onClick = {
                                    if (!backupStatus.hasCachedPassphrase) {
                                        showPassphraseDialog = true
                                    } else if (!backupStatus.isSignedIn) {
                                        showAuthSheet = true
                                    } else {
                                        scope.launch { backupManager.performCloudBackup() }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.space3))

                    // Local Export / Import SAF row (offline guarantee)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                    ) {
                        LaSecondaryButton(
                            text = "Export .labackup",
                            icon = Icons.Outlined.FileUpload,
                            onClick = {
                                val cached = backupManager.getCachedPassphrase()
                                if (cached.isNullOrBlank()) {
                                    showPassphraseDialog = true
                                } else {
                                    scope.launch {
                                        backupManager.createVerifiedEncryptedArchive(cached)
                                            .onSuccess { (_, json) ->
                                                localExportPendingJson = json
                                                val dateIso = DateAttributionPolicy.formatIsoDate(
                                                    DateAttributionPolicy.todayLocalDate()
                                                )
                                                exportFileLauncher.launch("life_analytics_backup_$dateIso.labackup")
                                            }
                                            .onFailure { err ->
                                                backupManager.updateStatusMessage(null, err.message)
                                            }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        LaSecondaryButton(
                            text = "Import .labackup",
                            icon = Icons.Outlined.FileDownload,
                            onClick = {
                                importFileLauncher.launch(arrayOf("*/*", "application/octet-stream"))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Cloud Backups List
                    if (backupStatus.isSignedIn && backupStatus.cloudBackups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(spacing.space4))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Available Cloud Backups (${backupStatus.cloudBackups.size}/${BackupManager.MAX_RETAINED_CLOUD_BACKUPS})",
                                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                            LaSecondaryButton(
                                text = "Refresh",
                                icon = Icons.Outlined.Refresh,
                                onClick = { scope.launch { backupManager.refreshCloudBackupsList() } }
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.space2))

                        backupStatus.cloudBackups.forEach { meta ->
                            CloudBackupRow(
                                metadata = meta,
                                onRestoreClick = {
                                    val cached = backupManager.getCachedPassphrase()
                                    if (!cached.isNullOrBlank()) {
                                        scope.launch {
                                            backupManager.downloadAndDecryptCloudBackup(meta, cached)
                                                .onSuccess { payload ->
                                                    restorePreviewPayload = payload
                                                    pendingRestoreSourceDescription = "Cloud Backup (${meta.deviceLabel})"
                                                }
                                                .onFailure {
                                                    pendingCloudMetadataToDecrypt = meta
                                                }
                                        }
                                    } else {
                                        pendingCloudMetadataToDecrypt = meta
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(spacing.space2))
                        }
                    }
                }
            }
        }

        // Isolated Historical Sample Dataset (June 1 – November 18, 2026) Panel
        item {
            var demoStatusText by remember { mutableStateOf<String?>(null) }
            var isSeedBusy by remember { mutableStateOf(false) }

            LaContainerPanel(
                title = "Historical Sample Data (Jun 1 – Nov 18, 2026)",
                subtitle = "Generates isolated historical completions for your existing habits (no fake habits created), sample tasks, and varied finance records across Jun 1 – Nov 18, 2026. Personal records are never modified."
            ) {
                if (demoStatusText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(radii.shapeSm)
                            .background(tints.mint.soft)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = demoStatusText!!,
                            style = typography.label,
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.space2))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    LaPrimaryButton(
                        text = "Rebuild Sample Data",
                        enabled = !isSeedBusy,
                        onClick = {
                            isSeedBusy = true
                            scope.launch {
                                repository.rebuildHistoricalSampleData(preferences.currencyCode)
                                    .onSuccess { summary ->
                                        preferencesRepository.setSampleHistoryEnabled(true)
                                        demoStatusText = "Built ${summary.rangeLabel}: ${summary.existingHabitsUsed} existing habit(s), ${summary.habitCompletionsSeeded} completions, ${summary.tasksSeeded} sample tasks, ${summary.transactionsSeeded} finance entries."
                                    }
                                    .onFailure { err ->
                                        demoStatusText = "Could not build sample data: ${err.message}"
                                    }
                                isSeedBusy = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    LaSecondaryButton(
                        text = "Remove Sample Data",
                        enabled = !isSeedBusy,
                        onClick = {
                            isSeedBusy = true
                            scope.launch {
                                repository.removeLegacyOctoberDemoData()
                                repository.removeHistoricalSampleData()
                                    .onSuccess {
                                        preferencesRepository.setSampleHistoryEnabled(false)
                                        demoStatusText = "Removed all sample records. Personal habits, tasks, and transactions were preserved."
                                    }
                                    .onFailure { err ->
                                        demoStatusText = "Could not remove sample data: ${err.message}"
                                    }
                                isSeedBusy = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Privacy & Permissions Panel
        item {
            LaContainerPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LaIconChip(
                        icon = Icons.Outlined.Lock,
                        containerColor = tints.lavender.base,
                        iconTint = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(spacing.space3))
                    Text(
                        text = "Privacy, Storage & Permissions",
                        style = typography.sectionTitle,
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(spacing.space3))
                Text(
                    text = "• Local-first data: All tasks, habits, finance records, and daily reviews are stored in app-private SQLite (Room) storage on your device.\n" +
                        "• Authenticated cloud backup: Backups are encrypted on your phone using AES-256-GCM before upload to Supabase private storage. No unencrypted records leave your device.\n" +
                        "• Zero sensitive permissions: Life Analytics does not request camera, microphone, location, contacts, external storage, or exact alarms.",
                    style = typography.body,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(spacing.space4))
                LaSecondaryButton(
                    text = "Open Android App Settings",
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    onClick = {
                        runCatching {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }

        // About Panel
        item {
            LaContainerPanel(
                title = "About Life Analytics",
                subtitle = "Version 1.1.0 (V1) • Native Android (Kotlin & Jetpack Compose)"
            ) {
                Text(
                    text = "Minimum input, maximum meaningful output — one source of truth across Home, Daily Planning, Daily Habits, Finance, Insights, and Daily Review.",
                    style = typography.label,
                    color = colors.textSecondary
                )
            }
        }
    }

    // =========================================================================
    // DIALOGS & BOTTOM SHEETS
    // =========================================================================

    // 1. Currency Change Confirmation Dialog
    if (pendingCurrencyChange != null) {
        val target = pendingCurrencyChange!!
        AlertDialog(
            onDismissRequest = { pendingCurrencyChange = null },
            shape = radii.shapeLg,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Change display currency to ${target.code}?",
                    style = typography.sectionTitle,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "You already have recorded finance transactions. Changing the display currency updates the currency symbol for summaries and new entries, without performing foreign-exchange conversion on past amounts.",
                    style = typography.body,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                LaPrimaryButton(
                    text = "Change to ${target.symbol}",
                    onClick = {
                        scope.launch {
                            preferencesRepository.setCurrencyCode(target.code)
                            pendingCurrencyChange = null
                        }
                    }
                )
            },
            dismissButton = {
                LaSecondaryButton(
                    text = "Cancel",
                    onClick = { pendingCurrencyChange = null }
                )
            }
        )
    }

    // 2. Set/Change Backup Recovery Passphrase Dialog
    if (showPassphraseDialog && backupManager != null) {
        var inputPassphrase by remember { mutableStateOf("") }
        var inputConfirm by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPassphraseDialog = false },
            shape = radii.shapeLg,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Backup Recovery Passphrase",
                    style = typography.sectionTitle,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This passphrase derives your AES-256 encryption key. It survives uninstalls so you can decrypt your backup on a fresh installation. Store it safely.",
                        style = typography.body,
                        color = colors.textSecondary
                    )
                    LaTextField(
                        value = inputPassphrase,
                        onValueChange = {
                            inputPassphrase = it
                            errorMessage = null
                        },
                        label = "Passphrase (minimum 8 characters)"
                    )
                    LaTextField(
                        value = inputConfirm,
                        onValueChange = {
                            inputConfirm = it
                            errorMessage = null
                        },
                        label = "Confirm Passphrase"
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = typography.label,
                            color = colors.error
                        )
                    }
                }
            },
            confirmButton = {
                LaPrimaryButton(
                    text = "Save Passphrase",
                    onClick = {
                        if (inputPassphrase.trim().length < 8) {
                            errorMessage = "Passphrase must be at least 8 characters."
                        } else if (inputPassphrase != inputConfirm) {
                            errorMessage = "Passphrases do not match."
                        } else {
                            backupManager.saveRecoveryPassphrase(inputPassphrase)
                            showPassphraseDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                LaSecondaryButton(
                    text = "Cancel",
                    onClick = { showPassphraseDialog = false }
                )
            }
        )
    }

    // 3. Supabase Auth BottomSheet (Email + Password Sign In / Sign Up)
    if (showAuthSheet && backupManager != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var isSignUpMode by remember { mutableStateOf(false) }
        var emailInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var localAuthError by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = { showAuthSheet = false },
            sheetState = sheetState,
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenInset)
                    .padding(bottom = spacing.dockBottomClearance),
                verticalArrangement = Arrangement.spacedBy(spacing.space3)
            ) {
                Text(
                    text = if (isSignUpMode) "Create Cloud Backup Account" else "Sign In to Cloud Backup",
                    style = typography.sectionTitle,
                    color = colors.textPrimary
                )
                Text(
                    text = "Stores your encrypted backups in your Supabase project (njfaommiwnylhdowbbic). All backups are encrypted before leaving your phone.",
                    style = typography.label,
                    color = colors.textSecondary
                )

                LaTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        localAuthError = null
                    },
                    label = "Email Address"
                )

                LaTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        localAuthError = null
                    },
                    label = "Account Password (min 6 characters)"
                )

                if (localAuthError != null) {
                    Text(
                        text = localAuthError!!,
                        style = typography.label,
                        color = colors.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    LaPrimaryButton(
                        text = if (isSignUpMode) "Create Account" else "Sign In",
                        onClick = {
                            if (!emailInput.contains("@") || emailInput.isBlank()) {
                                localAuthError = "Enter a valid email address."
                            } else if (passwordInput.length < 6) {
                                localAuthError = "Password must be at least 6 characters."
                            } else {
                                scope.launch {
                                    val res = if (isSignUpMode) {
                                        backupManager.signUp(emailInput, passwordInput)
                                    } else {
                                        backupManager.signIn(emailInput, passwordInput)
                                    }
                                    res.fold(
                                        onSuccess = { showAuthSheet = false },
                                        onFailure = { localAuthError = it.message }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                LaSecondaryButton(
                    text = if (isSignUpMode) "Already have an account? Sign In" else "New user? Create an Account",
                    onClick = {
                        isSignUpMode = !isSignUpMode
                        localAuthError = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // 4. Prompt Passphrase for Decrypting Pending Import / Cloud Backup
    if (pendingArchiveJsonToDecrypt != null && backupManager != null) {
        var passphraseInput by remember { mutableStateOf("") }
        var decryptError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { pendingArchiveJsonToDecrypt = null },
            shape = radii.shapeLg,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Enter Backup Recovery Passphrase",
                    style = typography.sectionTitle,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter the passphrase used when creating this backup to decrypt and verify its contents.",
                        style = typography.body,
                        color = colors.textSecondary
                    )
                    LaTextField(
                        value = passphraseInput,
                        onValueChange = {
                            passphraseInput = it
                            decryptError = null
                        },
                        label = "Recovery Passphrase"
                    )
                    if (decryptError != null) {
                        Text(
                            text = decryptError!!,
                            style = typography.label,
                            color = colors.error
                        )
                    }
                }
            },
            confirmButton = {
                LaPrimaryButton(
                    text = "Decrypt & Inspect",
                    onClick = {
                        val payloadResult = backupManager.inspectAndDecryptArchive(
                            pendingArchiveJsonToDecrypt!!,
                            passphraseInput
                        )
                        payloadResult.fold(
                            onSuccess = { payload ->
                                restorePreviewPayload = payload
                                pendingRestoreSourceDescription = "Decrypted File (${payload.manifest.deviceLabel})"
                                pendingArchiveJsonToDecrypt = null
                            },
                            onFailure = { decryptError = it.message }
                        )
                    }
                )
            },
            dismissButton = {
                LaSecondaryButton(
                    text = "Cancel",
                    onClick = { pendingArchiveJsonToDecrypt = null }
                )
            }
        )
    }

    if (pendingCloudMetadataToDecrypt != null && backupManager != null) {
        val meta = pendingCloudMetadataToDecrypt!!
        var passphraseInput by remember { mutableStateOf("") }
        var decryptError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { pendingCloudMetadataToDecrypt = null },
            shape = radii.shapeLg,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Decrypt Cloud Backup",
                    style = typography.sectionTitle,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your Backup Recovery Passphrase to download and verify this cloud backup.",
                        style = typography.body,
                        color = colors.textSecondary
                    )
                    LaTextField(
                        value = passphraseInput,
                        onValueChange = {
                            passphraseInput = it
                            decryptError = null
                        },
                        label = "Recovery Passphrase"
                    )
                    if (decryptError != null) {
                        Text(
                            text = decryptError!!,
                            style = typography.label,
                            color = colors.error
                        )
                    }
                }
            },
            confirmButton = {
                LaPrimaryButton(
                    text = "Download & Verify",
                    onClick = {
                        scope.launch {
                            backupManager.downloadAndDecryptCloudBackup(meta, passphraseInput)
                                .fold(
                                    onSuccess = { payload ->
                                        restorePreviewPayload = payload
                                        pendingRestoreSourceDescription = "Cloud Backup (${meta.deviceLabel})"
                                        pendingCloudMetadataToDecrypt = null
                                    },
                                    onFailure = { decryptError = it.message }
                                )
                        }
                    }
                )
            },
            dismissButton = {
                LaSecondaryButton(
                    text = "Cancel",
                    onClick = { pendingCloudMetadataToDecrypt = null }
                )
            }
        )
    }

    // 5. Restore Preview Dialog (Replace Local vs Merge With Local)
    if (restorePreviewPayload != null && backupManager != null) {
        val payload = restorePreviewPayload!!
        var selectedMode by remember { mutableStateOf(RestoreMode.REPLACE_LOCAL) }
        var isRestoring by remember { mutableStateOf(false) }

        val formattedDate = remember(payload.manifest.createdAtEpochMillis) {
            val instant = Instant.ofEpochMilli(payload.manifest.createdAtEpochMillis)
            val zdt = instant.atZone(ZoneId.systemDefault())
            "${DateAttributionPolicy.formatDisplayDate(zdt.toLocalDate())} at " +
                String.format("%02d:%02d", zdt.hour, zdt.minute)
        }

        AlertDialog(
            onDismissRequest = { if (!isRestoring) restorePreviewPayload = null },
            shape = radii.shapeLg,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Restore Backup Preview",
                    style = typography.sectionTitle,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Source: $pendingRestoreSourceDescription\nCreated: $formattedDate\nApp Version: ${payload.manifest.appVersion}",
                        style = typography.label,
                        color = colors.textSecondary
                    )

                    // Record Counts Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(radii.shapeSm)
                            .background(colors.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        val c = payload.manifest.recordCounts
                        Text(
                            text = "• Tasks: ${c.tasks}\n" +
                                "• Focus Sessions: ${c.focusSessions} (${c.focusPauseIntervals} pauses)\n" +
                                "• Habits: ${c.habits} (${c.habitSchedulePeriods} schedules, ${c.habitCompletions} completions)\n" +
                                "• Finance: ${c.financeTransactions} transactions\n" +
                                "• Daily Reviews: ${c.dailyReviews}\n" +
                                "• Total: ${c.totalRecords} records",
                            style = typography.body.copy(fontWeight = FontWeight.Medium),
                            color = colors.textPrimary
                        )
                    }

                    Text(
                        text = "Choose Restore Mode:",
                        style = typography.label,
                        color = colors.textPrimary
                    )

                    RestoreMode.entries.forEach { mode ->
                        val isSelected = mode == selectedMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(radii.shapeSm)
                                .background(if (isSelected) tints.lavender.soft else colors.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) colors.ink else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = radii.shapeSm
                                )
                                .pressScaleClickable { selectedMode = mode }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.displayName,
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = mode.description,
                                    style = typography.chartMicroLabel,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colors.ink,
                        strokeWidth = 2.dp
                    )
                } else {
                    LaPrimaryButton(
                        text = "Restore Records",
                        onClick = {
                            isRestoring = true
                            scope.launch {
                                backupManager.restoreValidatedPayload(payload, selectedMode)
                                    .fold(
                                        onSuccess = { counts ->
                                            isRestoring = false
                                            restorePreviewPayload = null
                                            backupManager.updateStatusMessage(
                                                "Successfully restored ${counts.totalRecords} records (${selectedMode.displayName})."
                                            )
                                        },
                                        onFailure = { err ->
                                            isRestoring = false
                                            backupManager.updateStatusMessage(null, "Restore failed: ${err.message}")
                                        }
                                    )
                            }
                        }
                    )
                }
            },
            dismissButton = {
                if (!isRestoring) {
                    LaSecondaryButton(
                        text = "Cancel",
                        onClick = { restorePreviewPayload = null }
                    )
                }
            }
        )
    }
}

@Composable
private fun CloudBackupRow(
    metadata: CloudBackupMetadata,
    onRestoreClick: () -> Unit
) {
    val colors = LaTheme.colors
    val typography = LaTheme.typography
    val radii = LaTheme.radii

    val dateFormatted = remember(metadata.createdAtEpochMillis) {
        val instant = Instant.ofEpochMilli(metadata.createdAtEpochMillis)
        val zdt = instant.atZone(ZoneId.systemDefault())
        "${DateAttributionPolicy.formatDisplayDate(zdt.toLocalDate())} ${String.format("%02d:%02d", zdt.hour, zdt.minute)}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(radii.shapeSm)
            .background(colors.surfaceVariant.copy(alpha = 0.65f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateFormatted,
                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary
            )
            Text(
                text = "${metadata.recordCounts.totalRecords} records • ${metadata.deviceLabel} • v${metadata.appVersion}",
                style = typography.chartMicroLabel,
                color = colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        LaSecondaryButton(
            text = "Restore",
            icon = Icons.Outlined.CloudDownload,
            onClick = onRestoreClick
        )
    }
}
