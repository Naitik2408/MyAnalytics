package com.lifeanalytics.app.core.designsystem

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// =============================================================================
// 03_UI_UX_GUIDELINES.md (v1.2) — Phase 2: Core Reusable Components
// =============================================================================

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun Modifier.pressScaleClickable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 100),
        label = "press_scale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

// -----------------------------------------------------------------------------
// 9.1 Buttons
// -----------------------------------------------------------------------------

@Composable
fun LaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = LaTheme.colors.ink,
    contentColor: Color = LaTheme.colors.onInk
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 100),
        label = "primary_btn_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = LaTheme.radii.shapeFull,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = LaTheme.colors.surfaceVariant,
            disabledContentColor = LaTheme.colors.textSecondary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = LaTheme.typography.buttonLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = LaTheme.colors.surfaceVariant,
    contentColor: Color = LaTheme.colors.textPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 100),
        label = "secondary_btn_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = LaTheme.radii.shapeFull,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = LaTheme.colors.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = LaTheme.colors.textSecondary
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = LaTheme.typography.buttonLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LaTheme.colors.accent
) {
    TextButton(
        onClick = onClick,
        shape = LaTheme.radii.shapeFull,
        modifier = modifier.defaultMinSize(minHeight = 48.dp)
    ) {
        Text(
            text = text,
            style = LaTheme.typography.buttonLabel,
            color = color
        )
    }
}

/**
 * Signature Detail #5: Circular utility button (48 dp circle on surfaceVariant).
 */
@Composable
fun LaCircularIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LaTheme.colors.surfaceVariant,
    contentColor: Color = LaTheme.colors.textPrimary,
    size: Dp = 46.dp
) {
    Box(
        modifier = modifier
            .size(size.coerceAtLeast(44.dp))
            .clip(CircleShape)
            .background(containerColor)
            .semantics { this.contentDescription = contentDescription }
            .pressScaleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(21.dp)
        )
    }
}

/**
 * Signature Detail #6: Arrow affordance (36-40 dp circle with `↗` icon).
 */
@Composable
fun LaArrowButton(
    contentDescription: String,
    onClick: (() -> Unit)? = null,
    onTintedSurface: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bg = if (onTintedSurface) {
        if (LaTheme.colors.isDark) {
            LaTheme.colors.surface.copy(alpha = 0.65f)
        } else {
            Color.White.copy(alpha = 0.78f)
        }
    } else {
        LaTheme.colors.surfaceVariant
    }

    val boxModifier = if (onClick != null) {
        modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
            .semantics { this.contentDescription = contentDescription }
            .pressScaleClickable(onClick = onClick)
    } else {
        modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.NorthEast,
            contentDescription = null,
            tint = LaTheme.colors.textPrimary,
            modifier = Modifier.size(17.dp)
        )
    }
}

/**
 * Signature Detail #4: Icon chip (38 dp circular chip holding an icon, never floating bare).
 */
@Composable
fun LaIconChip(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onTintedSurface: Boolean = true,
    iconTint: Color = LaTheme.colors.textPrimary,
    containerColor: Color? = null,
    backgroundColor: Color? = null,
    size: Dp = 38.dp
) {
    val resolvedContainer = backgroundColor ?: containerColor ?: if (onTintedSurface) {
        if (LaTheme.colors.isDark) {
            LaTheme.colors.surface.copy(alpha = 0.72f)
        } else {
            Color.White.copy(alpha = 0.85f)
        }
    } else {
        LaTheme.colors.surfaceVariant
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(resolvedContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(19.dp)
        )
    }
}

// -----------------------------------------------------------------------------
// 9.2 Tiles, Micro-Visuals, and Panels (Borderless tonal separation)
// -----------------------------------------------------------------------------

private fun isTextStateOrZeroEmptyValue(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return true
    if (trimmed in setOf("0", "0m", "0h 0m", "₹0", "—", "No data", "No due days")) return true
    return !trimmed.any { it.isDigit() }
}

/**
 * Signature Detail #10 & v1.3 Rules 2, 6, 8: Stat tile pattern
 * Top row = icon chip (left) + optional arrow/action (right)
 * Small label -> Big numeric value (or balanced empty-value text style) -> optional micro-visual.
 * Normalizes "0 / 0" to "0" per Rule 6.
 */
@Composable
fun LaStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    supportingText: String? = null,
    containerColor: Color = LaTheme.colors.surface,
    onTintedSurface: Boolean = containerColor != LaTheme.colors.surface,
    valueColor: Color = LaTheme.colors.textPrimary,
    isEmptyValue: Boolean = false,
    minHeight: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    trailingHeader: (@Composable () -> Unit)? = null,
    microVisual: (@Composable () -> Unit)? = null
) {
    val normalizedValue = if (value.trim() == "0 / 0") "0" else value
    val secondaryColor = if (onTintedSurface) {
        LaTheme.colors.onTintSecondary
    } else {
        LaTheme.colors.textSecondary
    }
    val useEmptyTextStyle = isEmptyValue || isTextStateOrZeroEmptyValue(normalizedValue)

    val heightMod = if (minHeight > 0.dp) {
        Modifier.defaultMinSize(minHeight = minHeight)
    } else {
        Modifier
    }

    val tileModifier = if (onClick != null) {
        modifier
            .then(heightMod)
            .clip(LaTheme.radii.shapeMd)
            .background(containerColor)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $normalizedValue${supportingText?.let { ", $it" } ?: ""}"
            }
            .pressScaleClickable(onClick = onClick)
    } else {
        modifier
            .then(heightMod)
            .clip(LaTheme.radii.shapeMd)
            .background(containerColor)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $normalizedValue${supportingText?.let { ", $it" } ?: ""}"
            }
    }

    Column(
        modifier = tileModifier
            .fillMaxWidth()
            .padding(LaTheme.spacing.tilePadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (icon != null || onClick != null || trailingHeader != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    LaIconChip(
                        icon = icon,
                        onTintedSurface = onTintedSurface
                    )
                } else {
                    Spacer(modifier = Modifier.size(38.dp))
                }
                if (trailingHeader != null) {
                    trailingHeader()
                } else if (onClick != null) {
                    LaArrowButton(
                        contentDescription = "Open $label",
                        onClick = null,
                        onTintedSurface = onTintedSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Column {
            Text(
                text = label,
                style = LaTheme.typography.label,
                color = secondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = normalizedValue,
                style = if (useEmptyTextStyle) {
                    LaTheme.typography.metricEmptyValue
                } else {
                    LaTheme.typography.metricValue
                },
                color = if (useEmptyTextStyle) secondaryColor else valueColor
            )
            if (!supportingText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = supportingText,
                    style = LaTheme.typography.chartMicroLabel,
                    color = secondaryColor
                )
            }
        }

        if (microVisual != null) {
            Spacer(modifier = Modifier.height(10.dp))
            microVisual()
        }
    }
}

/**
 * v1.3 Rule 8: Micro progress bar inside a stat tile when data > 0.
 */
@Composable
fun LaMicroProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = LaTheme.colors.surface.copy(alpha = 0.65f),
    fillColor: Color = LaTheme.colors.ink
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped.coerceAtLeast(0.08f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(fillColor)
            )
        }
    }
}

/**
 * v1.3 Rule 8: 5-7 mini vertical pill bars inside a stat tile when data > 0.
 */
@Composable
fun LaMicroPills(
    fractions: List<Float>,
    modifier: Modifier = Modifier,
    pillColor: Color = LaTheme.colors.ink.copy(alpha = 0.65f),
    highlightColor: Color = LaTheme.colors.ink,
    highlightIndex: Int = fractions.lastIndex
) {
    val maxVal = (fractions.maxOrNull() ?: 1f).coerceAtLeast(0.01f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        fractions.forEachIndexed { idx, v ->
            val fraction = if (v <= 0f) 0.22f else (v / maxVal).coerceIn(0.28f, 1f)
            val color = when {
                v <= 0f -> pillColor.copy(alpha = 0.25f)
                idx == highlightIndex -> highlightColor
                else -> pillColor
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((22.dp * fraction).coerceAtLeast(5.dp))
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * v1.3 Rule 8: 7-day completion dot strip inside a habit stat tile when data > 0.
 */
@Composable
fun LaMicroDots(
    states: List<Boolean>,
    modifier: Modifier = Modifier,
    activeColor: Color = LaTheme.colors.ink,
    inactiveColor: Color = LaTheme.colors.surface.copy(alpha = 0.65f)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        states.take(7).forEach { done ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (done) activeColor else inactiveColor)
            )
        }
    }
}

/**
 * Section 9.2: Hero panel (`tint.lavender` strong or module strong tint, `radius.lg = 32.dp`, `20.dp` padding).
 */
@Composable
fun LaHeroPanel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    containerColor: Color = LaTheme.colors.tints.lavender.strong,
    onActionClick: (() -> Unit)? = null,
    actionContentDescription: String = "Open $title",
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LaTheme.radii.shapeLg)
            .background(containerColor)
            .padding(LaTheme.spacing.heroPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    LaIconChip(
                        icon = icon,
                        onTintedSurface = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = LaTheme.typography.sectionTitle,
                        color = LaTheme.colors.textPrimary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = LaTheme.typography.label,
                            color = LaTheme.colors.onTintSecondary
                        )
                    }
                }
            }
            if (onActionClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                LaArrowButton(
                    contentDescription = actionContentDescription,
                    onClick = onActionClick,
                    onTintedSurface = true
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

/**
 * Section 9.2: Container panel (`color.surface`, `radius.md = 24.dp`, holds lists and charts).
 */
@Composable
fun LaContainerPanel(
    title: String? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    containerColor: Color = LaTheme.colors.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LaTheme.radii.shapeMd)
            .background(containerColor)
            .padding(LaTheme.spacing.tilePadding)
    ) {
        if (title != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = LaTheme.typography.sectionTitle,
                        color = LaTheme.colors.textPrimary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = LaTheme.typography.label,
                            color = LaTheme.colors.textSecondary
                        )
                    }
                }
                if (actionLabel != null && onActionClick != null) {
                    LaTextButton(
                        text = actionLabel,
                        onClick = onActionClick
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        content()
    }
}

@Composable
fun LaTintedTile(
    containerColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = LaTheme.radii.shapeMd,
    padding: Dp = LaTheme.spacing.tilePadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseMod = if (onClick != null) {
        modifier
            .clip(shape)
            .background(containerColor)
            .pressScaleClickable(onClick = onClick)
            .padding(padding)
    } else {
        modifier
            .clip(shape)
            .background(containerColor)
            .padding(padding)
    }
    Column(
        modifier = baseMod,
        content = content
    )
}

// -----------------------------------------------------------------------------
// 9.5 Chips, Segmented Controls, and Date Range Pill
// -----------------------------------------------------------------------------

@Composable
fun LaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    selectedColor: Color = LaTheme.colors.tints.lavender.strong
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) selectedColor else LaTheme.colors.surface,
        animationSpec = tween(180),
        label = "chip_bg"
    )
    val borderMod = if (selected) {
        Modifier
    } else {
        Modifier.border(1.dp, LaTheme.colors.outline, LaTheme.radii.shapeFull)
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(LaTheme.radii.shapeFull)
            .then(borderMod)
            .background(bgColor)
            .semantics { this.selected = selected }
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = LaTheme.colors.textPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = LaTheme.typography.label.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = LaTheme.colors.textPrimary,
            maxLines = 1
        )
    }
}

@Composable
fun <T> LaSegmentedControl(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(LaTheme.radii.shapeFull)
            .background(LaTheme.colors.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val bg by animateColorAsState(
                targetValue = if (isSelected) LaTheme.colors.ink else Color.Transparent,
                animationSpec = tween(200),
                label = "seg_bg"
            )
            val textCol by animateColorAsState(
                targetValue = if (isSelected) LaTheme.colors.onInk else LaTheme.colors.textSecondary,
                animationSpec = tween(200),
                label = "seg_fg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(LaTheme.radii.shapeFull)
                    .background(bg)
                    .semantics { this.selected = isSelected }
                    .pressScaleClickable { onOptionSelected(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelProvider(option),
                    style = LaTheme.typography.label.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = textCol,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LaDateRangePill(
    title: String,
    badgeText: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onCenterClick: (() -> Unit)? = null,
    onResetToCurrent: (() -> Unit)? = null,
    resetLabel: String = "Today"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LaTheme.radii.shapeFull)
            .background(LaTheme.colors.surface)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LaCircularIconButton(
            icon = Icons.Outlined.ChevronLeft,
            contentDescription = "Previous period",
            onClick = onPrevious,
            size = 44.dp
        )

        val centerMod = if (onCenterClick != null) {
            Modifier
                .weight(1f)
                .clip(LaTheme.radii.shapeFull)
                .pressScaleClickable(onClick = onCenterClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        } else {
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        }

        Column(
            modifier = centerMod,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onCenterClick != null) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = LaTheme.colors.accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    style = LaTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                    color = LaTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!badgeText.isNullOrBlank()) {
                Text(
                    text = badgeText,
                    style = LaTheme.typography.chartMicroLabel,
                    color = LaTheme.colors.textSecondary,
                    maxLines = 1
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onResetToCurrent != null) {
                Box(
                    modifier = Modifier
                        .clip(LaTheme.radii.shapeFull)
                        .background(LaTheme.colors.tints.lavender.soft)
                        .pressScaleClickable(onClick = onResetToCurrent)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = resetLabel,
                        style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.SemiBold),
                        color = LaTheme.colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            LaCircularIconButton(
                icon = Icons.Outlined.ChevronRight,
                contentDescription = "Next period",
                onClick = onNext,
                size = 44.dp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 8.1 Floating Navigation Dock
// -----------------------------------------------------------------------------

data class LaDockItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun LaFloatingDock(
    items: List<LaDockItem>,
    currentRoute: String,
    onItemSelected: (LaDockItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val shadowColor = LaTheme.colors.ink.copy(alpha = if (LaTheme.colors.isDark) 0.35f else 0.12f)
    val canvasColor = LaTheme.colors.canvas

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // v1.3 Rule 3: 32 dp vertical fade scrim from canvas (0%) to canvas (92%) directly above the dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            canvasColor.copy(alpha = 0f),
                            canvasColor.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(canvasColor.copy(alpha = 0.94f))
                .padding(
                    start = LaTheme.spacing.screenInset,
                    end = LaTheme.spacing.screenInset,
                    bottom = navBarInset + LaTheme.spacing.dockBottomMargin
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LaTheme.spacing.dockHeight)
                    .shadow(
                        elevation = 24.dp,
                        shape = LaTheme.radii.shapeFull,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
                    .clip(LaTheme.radii.shapeFull)
                    .background(LaTheme.colors.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) LaTheme.colors.ink else Color.Transparent,
                        animationSpec = tween(200),
                        label = "dock_pill_bg"
                    )
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            LaTheme.colors.onInk
                        } else {
                            LaTheme.colors.textPrimary.copy(alpha = 0.72f)
                        },
                        animationSpec = tween(200),
                        label = "dock_icon_color"
                    )
                    val labelColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            LaTheme.colors.onInk
                        } else {
                            LaTheme.colors.textSecondary
                        },
                        animationSpec = tween(200),
                        label = "dock_label_color"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minWidth = 48.dp, minHeight = 52.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(pillBg)
                            .semantics {
                                this.selected = isSelected
                                this.contentDescription = item.label
                            }
                            .pressScaleClickable { onItemSelected(item) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = LaTheme.typography.chartMicroLabel.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = labelColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 9.3, 9.4 & 13: Inputs, Animated Checkbox, List Rows, Skeleton & Empty State
// -----------------------------------------------------------------------------

@Composable
fun LaAnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedColor: Color = LaTheme.colors.ink
) {
    val reducedMotion = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.94f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "checkbox_pulse"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            !enabled && checked -> checkedColor.copy(alpha = 0.45f)
            checked -> checkedColor
            else -> Color.Transparent
        },
        animationSpec = tween(if (reducedMotion) 0 else 180),
        label = "checkbox_bg"
    )
    val borderColor = when {
        checked -> Color.Transparent
        !enabled -> LaTheme.colors.outline.copy(alpha = 0.5f)
        else -> LaTheme.colors.textSecondary.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .then(
                    if (!checked) {
                        Modifier.border(1.75.dp, borderColor, RoundedCornerShape(8.dp))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = LaTheme.colors.onInk,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun LaRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 6.dp),
        thickness = 1.dp,
        color = LaTheme.colors.outline.copy(alpha = 0.55f)
    )
}

@Composable
fun LaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 4,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = LaTheme.typography.label,
            color = if (isError) LaTheme.colors.error else LaTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isNotEmpty()) {
                {
                    Text(
                        text = placeholder,
                        style = LaTheme.typography.body,
                        color = LaTheme.colors.textSecondary.copy(alpha = 0.65f)
                    )
                }
            } else null,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            isError = isError,
            textStyle = LaTheme.typography.body.copy(color = LaTheme.colors.textPrimary),
            keyboardOptions = keyboardOptions,
            shape = LaTheme.radii.shapeSm,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = LaTheme.colors.surfaceVariant,
                unfocusedContainerColor = LaTheme.colors.surfaceVariant,
                errorContainerColor = LaTheme.colors.surfaceVariant,
                focusedBorderColor = LaTheme.colors.accent,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = LaTheme.colors.error,
                cursorColor = LaTheme.colors.accent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .onFocusChanged { isFocused = it.isFocused }
        )
    }
}

@Composable
fun LaSkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    shape: Shape = LaTheme.radii.shapeMd
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(LaTheme.colors.tints.lavender.soft.copy(alpha = 0.7f))
    )
}

@Composable
fun LaScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
                LaCircularIconButton(
                    icon = Icons.Outlined.ChevronLeft,
                    contentDescription = "Back",
                    onClick = onNavigateBack
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    style = LaTheme.typography.screenTitle,
                    color = LaTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = LaTheme.typography.label,
                        color = LaTheme.colors.textSecondary
                    )
                }
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = trailingContent
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Section 14.1 (v1.3 Rule 4): 3-Level Empty-State System
// -----------------------------------------------------------------------------

/**
 * Level A — First-run "Get started" card on Home when all modules are empty.
 */
@Composable
fun LaFirstRunChecklistCard(
    onAddFirstTask: () -> Unit,
    onPickDailyHabit: () -> Unit,
    onStartFocusSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaContainerPanel(
        title = "Get started",
        subtitle = "Three quick steps to set up your daily rhythm.",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FirstRunStepRow(
                stepNumber = "1",
                title = "Add your first task",
                subtitle = "Plan what matters today",
                icon = Icons.Outlined.EventNote,
                chipBg = LaTheme.colors.tints.lavender.soft,
                onClick = onAddFirstTask
            )
            FirstRunStepRow(
                stepNumber = "2",
                title = "Pick one daily habit",
                subtitle = "Track a simple recurring routine",
                icon = Icons.Outlined.Repeat,
                chipBg = LaTheme.colors.tints.butter.soft,
                onClick = onPickDailyHabit
            )
            FirstRunStepRow(
                stepNumber = "3",
                title = "Start a 25-min focus session",
                subtitle = "Record uninterrupted work",
                icon = Icons.Outlined.Timer,
                chipBg = LaTheme.colors.tints.mint.soft,
                onClick = onStartFocusSession
            )
        }
    }
}

@Composable
private fun FirstRunStepRow(
    stepNumber: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    chipBg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LaTheme.radii.shapeSm)
            .background(LaTheme.colors.surfaceVariant.copy(alpha = 0.55f))
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LaIconChip(
                icon = icon,
                backgroundColor = chipBg,
                size = 38.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "$stepNumber. $title",
                    style = LaTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = LaTheme.colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = LaTheme.typography.chartMicroLabel,
                    color = LaTheme.colors.textSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = title,
            tint = LaTheme.colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Level B — Module empty state inside a container panel.
 * One module-specific icon chip, one title line, one sentence, one button.
 * Never nests a tinted box inside a white card.
 */
@Composable
fun LaModuleEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    chipColor: Color = LaTheme.colors.tints.lavender.soft,
    iconTint: Color? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LaIconChip(
                icon = icon,
                backgroundColor = iconTint ?: chipColor,
                size = 40.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = LaTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = LaTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = LaTheme.typography.label,
                    color = LaTheme.colors.textSecondary
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(12.dp))
            LaSecondaryButton(
                text = actionLabel,
                onClick = onActionClick,
                containerColor = LaTheme.colors.surfaceVariant
            )
        }
    }
}

/**
 * Level C — Consolidated period-empty card for Insights or Finance when an entire period has no data.
 */
@Composable
fun LaPeriodEmptyCard(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    chipColor: Color = LaTheme.colors.tints.lavender.soft,
    modifier: Modifier = Modifier
) {
    LaContainerPanel(modifier = modifier) {
        LaModuleEmptyState(
            icon = icon,
            title = title,
            message = message,
            actionLabel = actionLabel,
            onActionClick = onActionClick,
            chipColor = chipColor
        )
        if (secondaryActionLabel != null && onSecondaryActionClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            LaTextButton(
                text = secondaryActionLabel,
                onClick = onSecondaryActionClick
            )
        }
    }
}

/**
 * Level C (partial data) — Collapsed one-line row when only some sections lack data
 * (e.g. "Habit consistency — no data yet").
 */
@Composable
fun LaCollapsedEmptyRow(
    sectionTitle: String,
    emptyText: String = "no data yet",
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LaTheme.radii.shapeMd)
            .background(LaTheme.colors.surface)
            .padding(horizontal = LaTheme.spacing.tilePadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LaTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = "$sectionTitle — $emptyText",
                style = LaTheme.typography.body,
                color = LaTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Backwards-compatible wrappers upgraded to v1.3 visual language
// -----------------------------------------------------------------------------

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    valueColor: Color = LaTheme.colors.textPrimary,
    containerColor: Color = LaTheme.colors.surface,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    LaStatTile(
        label = label,
        value = value,
        modifier = modifier,
        icon = icon,
        supportingText = supportingText,
        containerColor = containerColor,
        valueColor = valueColor,
        onClick = onClick
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = LaTheme.typography.sectionTitle,
            color = LaTheme.colors.textPrimary
        )
        if (actionLabel != null && onActionClick != null) {
            LaTextButton(
                text = actionLabel,
                onClick = onActionClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorBar(
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val isToday = selectedDate == today

    LaDateRangePill(
        title = DateAttributionPolicy.formatDisplayDate(selectedDate),
        badgeText = if (isToday) "Today" else "Tap to pick date",
        onPrevious = { onDateSelected(selectedDate.minusDays(1)) },
        onNext = { onDateSelected(selectedDate.plusDays(1)) },
        onCenterClick = { showDatePicker = true },
        onResetToCurrent = if (!isToday) ({ onDateSelected(today) }) else null,
        resetLabel = "Today",
        modifier = modifier
    )

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                LaTextButton(
                    text = "Select",
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onDateSelected(picked)
                        }
                        showDatePicker = false
                    }
                )
            },
            dismissButton = {
                LaTextButton(
                    text = "Cancel",
                    color = LaTheme.colors.textSecondary,
                    onClick = { showDatePicker = false }
                )
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    icon: ImageVector = Icons.Outlined.EventNote,
    tintSoftColor: Color = LaTheme.colors.tints.lavender.soft,
    @Suppress("UNUSED_PARAMETER") ghostVisual: (@Composable () -> Unit)? = null
) {
    LaModuleEmptyState(
        icon = icon,
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = actionLabel,
        onActionClick = onActionClick,
        chipColor = tintSoftColor
    )
}

@Composable
fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = LaTheme.radii.shapeSm,
        color = LaTheme.colors.tints.rose.soft,
        border = BorderStroke(1.dp, LaTheme.colors.error.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "Error",
                    tint = LaTheme.colors.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message,
                    style = LaTheme.typography.body,
                    color = LaTheme.colors.textPrimary
                )
            }
            LaTextButton(
                text = "Dismiss",
                color = LaTheme.colors.error,
                onClick = onDismiss
            )
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = LaTheme.radii.shapeMd,
        containerColor = LaTheme.colors.surface,
        title = {
            Text(
                text = title,
                style = LaTheme.typography.sectionTitle,
                color = LaTheme.colors.textPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = LaTheme.typography.body,
                color = LaTheme.colors.textSecondary
            )
        },
        confirmButton = {
            LaPrimaryButton(
                text = confirmLabel,
                onClick = onConfirm,
                containerColor = LaTheme.colors.error,
                contentColor = Color.White
            )
        },
        dismissButton = {
            LaSecondaryButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}

// -----------------------------------------------------------------------------
// 9.7 Growth Direction Components (docs/14_GROWTH_ENGINE.md)
// -----------------------------------------------------------------------------

@Composable
fun LaVerdictChip(
    verdict: com.lifeanalytics.app.domain.growth.GrowthVerdict?,
    modifier: Modifier = Modifier
) {
    if (verdict == null) return
    val (bgColor, fgColor) = when (verdict) {
        com.lifeanalytics.app.domain.growth.GrowthVerdict.IMPROVING ->
            LaTheme.colors.tints.mint.strong to LaTheme.colors.textPrimary
        com.lifeanalytics.app.domain.growth.GrowthVerdict.STEADY ->
            LaTheme.colors.surfaceVariant to LaTheme.colors.textSecondary
        com.lifeanalytics.app.domain.growth.GrowthVerdict.SLIPPING ->
            LaTheme.colors.tints.rose.strong to LaTheme.colors.textPrimary
    }
    Box(
        modifier = modifier
            .clip(LaTheme.radii.shapeFull)
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = verdict.label,
            style = LaTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
            color = fgColor
        )
    }
}

@Composable
fun LaOverallVerdictBadge(
    verdict: com.lifeanalytics.app.domain.growth.OverallVerdict,
    modifier: Modifier = Modifier
) {
    val (bgColor, fgColor) = when (verdict) {
        com.lifeanalytics.app.domain.growth.OverallVerdict.IMPROVING ->
            LaTheme.colors.tints.mint.strong to LaTheme.colors.textPrimary
        com.lifeanalytics.app.domain.growth.OverallVerdict.STEADY ->
            LaTheme.colors.surfaceVariant to LaTheme.colors.textSecondary
        com.lifeanalytics.app.domain.growth.OverallVerdict.SLIPPING ->
            LaTheme.colors.tints.rose.strong to LaTheme.colors.textPrimary
        com.lifeanalytics.app.domain.growth.OverallVerdict.MIXED ->
            LaTheme.colors.tints.butter.strong to LaTheme.colors.textPrimary
        com.lifeanalytics.app.domain.growth.OverallVerdict.BUILDING_BASELINE ->
            LaTheme.colors.surfaceVariant to LaTheme.colors.textSecondary
    }
    Box(
        modifier = modifier
            .clip(LaTheme.radii.shapeFull)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = verdict.label,
            style = LaTheme.typography.label.copy(fontWeight = FontWeight.Bold),
            color = fgColor
        )
    }
}

@Composable
fun LaDirectionCard(
    directionResult: com.lifeanalytics.app.domain.growth.GrowthDirectionResult,
    selectedWindow: com.lifeanalytics.app.domain.growth.GrowthWindow,
    onSelectWindow: (com.lifeanalytics.app.domain.growth.GrowthWindow) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToInsights: (() -> Unit)? = null
) {
    val colors = LaTheme.colors
    val spacing = LaTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LaTheme.radii.shapeMd)
            .background(colors.surface)
            .padding(spacing.tilePadding)
    ) {
        // Top row: Section title, window selector chips, and overall verdict
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Direction",
                    style = LaTheme.typography.sectionTitle,
                    color = colors.textPrimary
                )
                Text(
                    text = "vs ${directionResult.previousPeriodLabel}",
                    style = LaTheme.typography.label,
                    color = colors.textSecondary
                )
            }
            LaOverallVerdictBadge(verdict = directionResult.overallVerdict)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Window selector tabs/chips (Day, Week, Month, Year)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            com.lifeanalytics.app.domain.growth.GrowthWindow.values().forEach { win ->
                val isSelected = win == selectedWindow
                val bg = if (isSelected) colors.ink else colors.surfaceVariant
                val fg = if (isSelected) colors.onInk else colors.textSecondary
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(LaTheme.radii.shapeFull)
                        .background(bg)
                        .pressScaleClickable(onClick = { onSelectWindow(win) })
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = win.displayName,
                        style = LaTheme.typography.label.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = fg,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Locked state or Pillars list
        when (val lock = directionResult.lockStatus) {
            is com.lifeanalytics.app.domain.growth.WindowLockStatus.Locked -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LaTheme.radii.shapeSm)
                        .background(colors.surfaceVariant)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Locked",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = lock.reason,
                        style = LaTheme.typography.body,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lock.unlockHint,
                        style = LaTheme.typography.label,
                        color = colors.textSecondary
                    )
                }
            }
            com.lifeanalytics.app.domain.growth.WindowLockStatus.Unlocked -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LaPillarVerdictRow(pillar = directionResult.habitsPillar)
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.5f), thickness = 0.5.dp)
                    LaPillarVerdictRow(pillar = directionResult.tasksPillar)
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.5f), thickness = 0.5.dp)
                    LaPillarVerdictRow(pillar = directionResult.financePillar)
                }
            }
        }

        if (onNavigateToInsights != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScaleClickable(onClick = onNavigateToInsights),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View in Insights",
                    style = LaTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun LaPillarVerdictRow(
    pillar: com.lifeanalytics.app.domain.growth.PillarVerdictResult,
    modifier: Modifier = Modifier
) {
    val colors = LaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pillar.pillarName,
                    style = LaTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = pillar.currentDisplayValue,
                    style = LaTheme.typography.label,
                    color = colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = pillar.reason,
                style = LaTheme.typography.label,
                color = colors.textSecondary
            )
            if (!pillar.supportingText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = pillar.supportingText,
                    style = LaTheme.typography.chartMicroLabel,
                    color = colors.onTintSecondary
                )
            }
        }

        if (pillar.verdict != null) {
            Spacer(modifier = Modifier.width(8.dp))
            LaVerdictChip(verdict = pillar.verdict)
        }
    }
}

