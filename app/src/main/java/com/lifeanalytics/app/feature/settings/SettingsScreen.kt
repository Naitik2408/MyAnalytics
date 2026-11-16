package com.lifeanalytics.app.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lifeanalytics.app.core.common.SupportedCurrency
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaFilterChip
import com.lifeanalytics.app.core.designsystem.LaIconChip
import com.lifeanalytics.app.core.designsystem.LaPrimaryButton
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaSecondaryButton
import com.lifeanalytics.app.core.designsystem.LaSegmentedControl
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.data.repository.PreferencesRepository
import com.lifeanalytics.app.domain.model.AppPreferences
import com.lifeanalytics.app.domain.model.AppThemeOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    preferencesRepository: PreferencesRepository,
    repository: LifeAnalyticsRepository,
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
                subtitle = "Appearance, currency, and privacy",
                onNavigateBack = onNavigateBack
            )
        }

        // Theme Panel
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

        // Currency Panel (Rule 7: Rupee symbol ₹ and Indian digit grouping)
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
                    text = "• Local-first data: All tasks, focus sessions, habits, finance records, and daily reviews are stored in app-private SQLite (Room) storage on your device.\n" +
                        "• Zero sensitive permissions: V1 does not request camera, microphone, location, contacts, external storage, exact alarms, or internet access.\n" +
                        "• Focus timer accuracy: Active and paused intervals are persisted in local storage so backgrounding or screen lock never loses confirmed focus time.",
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
                subtitle = "Version 1.0.0 (V1) • Native Android (Kotlin & Jetpack Compose)"
            ) {
                Text(
                    text = "Minimum input, maximum meaningful output — one source of truth across Home, Daily Planning, Focus Timer, Daily Habits, Finance, Insights, and Daily Review.",
                    style = typography.label,
                    color = colors.textSecondary
                )
            }
        }
    }

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
}
