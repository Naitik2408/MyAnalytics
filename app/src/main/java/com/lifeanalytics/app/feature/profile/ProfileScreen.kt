package com.lifeanalytics.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.designsystem.ConfirmDeleteDialog
import com.lifeanalytics.app.core.designsystem.LaIconChip
import com.lifeanalytics.app.core.designsystem.LaRowDivider
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.pressScaleClickable
import com.lifeanalytics.app.data.backup.BackupManager
import kotlinx.coroutines.launch

/**
 * Profile / Account hub — the single entry point for secondary pages.
 * Reuses the existing routes (review, insights, planning, habits, activity, settings);
 * it does not duplicate any of those screens.
 */
@Composable
fun ProfileScreen(
    backupManager: BackupManager?,
    onNavigateBack: () -> Unit,
    onOpenDailyReview: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = LaTheme.colors
    val spacing = LaTheme.spacing
    val scope = rememberCoroutineScope()

    val statusFlow = remember(backupManager) { backupManager?.statusState }
    val status = statusFlow?.collectAsState()?.value
    val isSignedIn = status?.isSignedIn == true
    val email = status?.userEmail?.takeIf { it.isNotBlank() }
    var confirmSignOut by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.screenInset,
                end = spacing.screenInset,
                top = spacing.space2,
                bottom = spacing.space6
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            item {
                LaScreenHeader(title = "Profile", onNavigateBack = onNavigateBack)
            }

            // Top section: avatar + account identity
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LaTheme.radii.shapeLg)
                        .background(colors.tints.lavender.strong)
                        .padding(spacing.space4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LaIconChip(
                        icon = Icons.Outlined.Person,
                        backgroundColor = colors.surface,
                        size = 60.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = email?.substringBefore('@')?.replaceFirstChar { it.uppercase() } ?: "Your profile",
                            style = LaTheme.typography.sectionTitle,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = email ?: "Local account · data stays on this device",
                            style = LaTheme.typography.label,
                            color = colors.onTintSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Navigation section
            item {
                ProfileSection(title = "Pages") {
                    ProfileNavRow(Icons.Outlined.RateReview, "Daily review", colors.tints.lavender.base, onOpenDailyReview)
                    LaRowDivider()
                    ProfileNavRow(Icons.Outlined.Insights, "Insights", colors.tints.blue.base, onOpenInsights)
                    LaRowDivider()
                    ProfileNavRow(Icons.AutoMirrored.Outlined.EventNote, "Tasks planner", colors.tints.mint.base, onOpenTasks)
                    LaRowDivider()
                    ProfileNavRow(Icons.Outlined.Repeat, "Habits", colors.tints.butter.base, onOpenHabits)
                    LaRowDivider()
                    ProfileNavRow(Icons.Outlined.Settings, "Settings", colors.surfaceVariant, onOpenSettings)
                }
            }

            // Account section
            item {
                ProfileSection(title = "Account") {
                    if (isSignedIn) {
                        ProfileNavRow(
                            icon = Icons.AutoMirrored.Outlined.Logout,
                            label = "Sign out of cloud backup",
                            chipColor = colors.tints.rose.base,
                            onClick = { confirmSignOut = true },
                            showChevron = false
                        )
                    } else {
                        ProfileNavRow(
                            icon = Icons.Outlined.CloudQueue,
                            label = "Cloud backup & sign in",
                            chipColor = colors.tints.blue.base,
                            onClick = onOpenSettings
                        )
                    }
                }
            }
        }
    }

    if (confirmSignOut && backupManager != null) {
        ConfirmDeleteDialog(
            title = "Sign out?",
            message = "You will be signed out of cloud backup. Your data on this device is not affected.",
            confirmLabel = "Sign out",
            onConfirm = {
                confirmSignOut = false
                scope.launch { backupManager.signOut() }
            },
            onDismiss = { confirmSignOut = false }
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = LaTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = LaTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LaTheme.radii.shapeLg)
                .background(colors.surface)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ProfileNavRow(
    icon: ImageVector,
    label: String,
    chipColor: Color,
    onClick: () -> Unit,
    showChevron: Boolean = true
) {
    val colors = LaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(LaTheme.radii.shapeMd)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .pressScaleClickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LaIconChip(icon = icon, backgroundColor = chipColor, size = 40.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = LaTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
