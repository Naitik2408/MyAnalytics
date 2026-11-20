package com.lifeanalytics.app.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// =============================================================================
// 03_UI_UX_GUIDELINES.md (v1.2) — Phase 3: Custom Canvas Chart Components
// =============================================================================

data class LaPillBarEntry(
    val label: String,
    val value: Float,
    val valueLabel: String,
    val secondaryValue: Float = 0f
)

/**
 * Section 11.3: 45-degree diagonal hatch pattern clipped inside a capsule RoundRect.
 * Stroke 1.35 dp, gap 3.8 dp, colour `ink` at 70% opacity (light) or 55% (dark).
 */
private fun DrawScope.drawHatchedCapsule(
    rect: Rect,
    cornerRadius: Float,
    baseColor: Color,
    hatchColor: Color
) {
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = rect,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        )
    }
    drawPath(path = path, color = baseColor)

    clipPath(path) {
        val strokePx = 1.35.dp.toPx()
        val stepPx = 5.2.dp.toPx()
        val height = rect.height
        var startX = rect.left - height
        while (startX < rect.right + height) {
            drawLine(
                color = hatchColor,
                start = Offset(startX, rect.bottom),
                end = Offset(startX + height, rect.top),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
            startX += stepPx
        }
    }
}

/**
 * Section 11.2: Pill bar chart (primary).
 * - Full-radius capsules (40-56 dp wide).
 * - Zero baseline: zero value shows only the soft track capsule, never a fake filled bar.
 * - Selected bar: solid `color.ink` with direct value badge.
 * - Other bars: hatched pattern over `tint.strong` (or `tint.base`).
 * - Empty state (Section 11.4): ghost capsules in `tint.soft` at 50% opacity + plain message.
 */
@Composable
fun LaPillBarChart(
    entries: List<LaPillBarEntry>,
    selectedIndex: Int,
    onSelectIndex: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    barTint: TintSteps = LaTheme.colors.tints.lavender,
    onHeroSurface: Boolean = false,
    chartHeight: Dp = 148.dp,
    emptyMessage: String = "No activity recorded in this period yet.",
    accessibilitySummary: String
) {
    val colors = LaTheme.colors
    val reducedMotion = rememberReducedMotion()
    var hasAnimatedIn by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasAnimatedIn = true
    }

    val entranceProgress by animateFloatAsState(
        targetValue = if (hasAnimatedIn || reducedMotion) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else 360,
            easing = FastOutSlowInEasing
        ),
        label = "pill_bar_entrance"
    )

    val hasAnyNonZero = entries.any { it.value > 0f || it.secondaryValue > 0f }
    val maxVal = entries.maxOfOrNull { maxOf(it.value, it.secondaryValue) }?.takeIf { it > 0f } ?: 1f

    val trackColor = if (onHeroSurface) {
        if (colors.isDark) colors.surface.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.52f)
    } else {
        barTint.soft
    }
    val secondaryFillColor = if (onHeroSurface) {
        if (colors.isDark) colors.surface.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.78f)
    } else {
        barTint.strong
    }
    val hatchInkColor = colors.ink.copy(alpha = if (colors.isDark) 0.55f else 0.68f)
    val labelColor = if (onHeroSurface) colors.onTintSecondary else colors.textSecondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilitySummary }
    ) {
        if (!hasAnyNonZero) {
            // Section 11.4 Ghost Pill Bars + 1 plain-language line
            val ghostRatios = listOf(0.35f, 0.58f, 0.42f, 0.72f, 0.48f, 0.62f, 0.38f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = entries.size.coerceAtLeast(5)
                    val slotWidth = size.width / count
                    val barW = (slotWidth * 0.64f).coerceIn(24.dp.toPx(), 48.dp.toPx())
                    val radius = barW / 2f

                    for (i in 0 until count) {
                        val centerX = i * slotWidth + slotWidth / 2f
                        val left = centerX - barW / 2f
                        val ghostRatio = ghostRatios[i % ghostRatios.size]
                        val ghostH = (size.height * ghostRatio).coerceAtLeast(barW)
                        drawRoundRect(
                            color = trackColor.copy(alpha = 0.55f),
                            topLeft = Offset(left, size.height - ghostH),
                            size = Size(barW, ghostH),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(LaTheme.radii.shapeFull)
                        .background(
                            if (onHeroSurface) {
                                colors.surface.copy(alpha = 0.88f)
                            } else {
                                barTint.soft.copy(alpha = 0.92f)
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = emptyMessage,
                        style = LaTheme.typography.label,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Direct value labels row above bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                entries.forEachIndexed { idx, entry ->
                    val isSelected = idx == selectedIndex
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (entry.value > 0f || isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(LaTheme.radii.shapeFull)
                                    .background(
                                        if (isSelected) colors.ink else Color.Transparent
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = entry.valueLabel,
                                    style = LaTheme.typography.chartMicroLabel.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) colors.onInk else colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .pointerInput(entries.size, onSelectIndex) {
                        if (onSelectIndex != null && entries.isNotEmpty()) {
                            detectTapGestures { tapOffset ->
                                val slotW = size.width.toFloat() / entries.size
                                val tappedIdx = (tapOffset.x / slotW).toInt().coerceIn(0, entries.lastIndex)
                                onSelectIndex(tappedIdx)
                            }
                        }
                    }
            ) {
                val count = entries.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barW = (slotWidth * 0.66f).coerceIn(22.dp.toPx(), 52.dp.toPx())
                val radius = barW / 2f

                entries.forEachIndexed { idx, entry ->
                    val centerX = idx * slotWidth + slotWidth / 2f
                    val left = centerX - barW / 2f

                    // Full-height soft capsule track
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barW, size.height),
                        cornerRadius = CornerRadius(radius, radius)
                    )

                    // Optional secondary (e.g., planned) capsule
                    if (entry.secondaryValue > 0f && entry.secondaryValue > entry.value) {
                        val secRatio = (entry.secondaryValue / maxVal).coerceIn(0f, 1f) * entranceProgress
                        val secH = (size.height * secRatio).coerceAtLeast(barW)
                        drawRoundRect(
                            color = secondaryFillColor.copy(alpha = 0.45f),
                            topLeft = Offset(left, size.height - secH),
                            size = Size(barW, secH),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }

                    // Primary value capsule: zero value draws NO filled bar (Section 11.2)
                    if (entry.value > 0f) {
                        val rawRatio = (entry.value / maxVal).coerceIn(0f, 1f) * entranceProgress
                        // Small non-zero value is at least 1 bar-width high so it remains a clean capsule
                        val barH = (size.height * rawRatio).coerceAtLeast(barW).coerceAtMost(size.height)
                        val barRect = Rect(
                            left = left,
                            top = size.height - barH,
                            right = left + barW,
                            bottom = size.height
                        )

                        if (idx == selectedIndex) {
                            drawRoundRect(
                                color = colors.ink,
                                topLeft = barRect.topLeft,
                                size = barRect.size,
                                cornerRadius = CornerRadius(radius, radius)
                            )
                        } else {
                            drawHatchedCapsule(
                                rect = barRect,
                                cornerRadius = radius,
                                baseColor = secondaryFillColor,
                                hatchColor = hatchInkColor
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day / Bucket Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            entries.forEachIndexed { idx, entry ->
                val isSelected = idx == selectedIndex
                Text(
                    text = entry.label,
                    style = LaTheme.typography.chartMicroLabel.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) colors.textPrimary else labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onSelectIndex != null) {
                                Modifier.pressScaleClickable { onSelectIndex(idx) }
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Section 11.2: Track Bars ("fill inside a pill")
// -----------------------------------------------------------------------------

@Composable
fun LaHorizontalTrackBar(
    label: String,
    valueText: String,
    progress: Float,
    modifier: Modifier = Modifier,
    tint: TintSteps = LaTheme.colors.tints.butter,
    useInkFill: Boolean = false
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = if (rememberReducedMotion()) 0 else 320),
        label = "track_bar_fill"
    )
    val fillColor = if (useInkFill) LaTheme.colors.ink else tint.strong

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = LaTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                color = LaTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = valueText,
                style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.SemiBold),
                color = LaTheme.colors.textSecondary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(tint.soft)
        ) {
            if (animatedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceAtLeast(0.08f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(fillColor)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Section 11.2: Ring (Donut) Chart with Number Badges & Legend
// -----------------------------------------------------------------------------

data class LaRingSlice(
    val label: String,
    val amountValue: Float,
    val formattedValue: String,
    val percentage: Int
)

@Composable
fun LaRingChart(
    slices: List<LaRingSlice>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No category breakdown recorded for this period.",
    accessibilitySummary: String
) {
    val colors = LaTheme.colors
    val palette = listOf(
        colors.ink,
        colors.tints.lavender.strong,
        colors.tints.blue.strong,
        colors.tints.mint.strong,
        colors.tints.butter.strong,
        colors.tints.rose.strong
    )
    val total = slices.sumOf { it.amountValue.toDouble() }.toFloat()

    if (slices.isEmpty() || total <= 0f) {
        // Ghost Ring Empty State (Section 11.4)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .semantics { contentDescription = emptyMessage },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Canvas(modifier = Modifier.size(116.dp)) {
                val stroke = 24.dp.toPx()
                val diameter = min(size.width, size.height) - stroke
                drawArc(
                    color = colors.tints.blue.soft.copy(alpha = 0.7f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f),
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(
                text = emptyMessage,
                style = LaTheme.typography.body,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
        }
        return
    }

    val density = LocalDensity.current
    val ringBoxSize = 152.dp
    val strokeDp = 28.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilitySummary },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(ringBoxSize),
            contentAlignment = Alignment.Center
        ) {
            val gapDegrees = if (slices.size > 1) 10f else 0f
            val availableDegrees = (360f - gapDegrees * slices.size).coerceAtLeast(180f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = strokeDp.toPx()
                val diameter = min(size.width, size.height) - strokePx - 8.dp.toPx()
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                var startAngle = -90f
                slices.forEachIndexed { idx, slice ->
                    val sweep = ((slice.amountValue / total) * availableDegrees).coerceAtLeast(12f)
                    drawArc(
                        color = palette[idx % palette.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    startAngle += sweep + gapDegrees
                }
            }

            // Signature Detail #11: White circular rank/percentage badges on the top 3 segments
            val radiusPx = with(density) { ((ringBoxSize - strokeDp - 8.dp) / 2).toPx() }
            var currentAngle = -90f
            slices.take(4).forEachIndexed { idx, slice ->
                val sweep = ((slice.amountValue / total) * availableDegrees).coerceAtLeast(12f)
                val midAngleRad = Math.toRadians((currentAngle + sweep / 2f).toDouble())
                val offsetX = (cos(midAngleRad) * radiusPx).roundToInt()
                val offsetY = (sin(midAngleRad) * radiusPx).roundToInt()
                currentAngle += sweep + gapDegrees

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX, offsetY) }
                        .size(26.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = colors.ink.copy(alpha = 0.16f),
                            spotColor = colors.ink.copy(alpha = 0.16f)
                        )
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${idx + 1}",
                        style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111A1C)
                    )
                }
            }
        }

        // Legend column beside the ring
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEachIndexed { idx, slice ->
                val sliceColor = palette[idx % palette.size]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(sliceColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${idx + 1}. ${slice.label}",
                            style = LaTheme.typography.label,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${slice.formattedValue} (${slice.percentage}%)",
                        style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Section 11.2: Mini Calendar / Heat Tiles
// -----------------------------------------------------------------------------

data class LaHeatDayCell(
    val dayLabel: String,
    val intensityLevel: Int // 0 = no data, 1 = low, 2 = medium, 3 = high, 4 = complete/max
)

@Composable
fun LaMiniHeatCalendar(
    cells: List<LaHeatDayCell>,
    modifier: Modifier = Modifier,
    tint: TintSteps = LaTheme.colors.tints.lavender,
    accessibilitySummary: String
) {
    val colors = LaTheme.colors
    val allEmpty = cells.all { it.intensityLevel <= 0 }
    val rows = cells.chunked(7)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilitySummary },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { rowCells ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowCells.forEach { cell ->
                    val cellBg = when {
                        allEmpty -> colors.surfaceVariant.copy(alpha = 0.38f)
                        cell.intensityLevel <= 0 -> colors.surfaceVariant.copy(alpha = 0.55f)
                        cell.intensityLevel == 1 -> tint.soft
                        cell.intensityLevel == 2 -> tint.base
                        cell.intensityLevel == 3 -> tint.strong
                        else -> colors.ink
                    }
                    val textCol = when {
                        allEmpty -> colors.textSecondary.copy(alpha = 0.45f)
                        cell.intensityLevel <= 0 -> colors.textSecondary
                        cell.intensityLevel >= 4 -> colors.onInk
                        else -> colors.textPrimary
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(LaTheme.radii.heatCell))
                            .background(cellBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cell.dayLabel,
                            style = LaTheme.typography.chartMicroLabel.copy(
                                fontWeight = if (cell.intensityLevel >= 3) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = textCol
                        )
                    }
                }
                if (rowCells.size < 7) {
                    repeat(7 - rowCells.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Less",
                style = LaTheme.typography.chartMicroLabel,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            listOf(
                colors.surfaceVariant.copy(alpha = 0.55f),
                tint.soft,
                tint.base,
                tint.strong,
                colors.ink
            ).forEach { stepColor ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(11.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(stepColor)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "More",
                style = LaTheme.typography.chartMicroLabel,
                color = colors.textSecondary
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Section 11.2: Session Timeline (horizontal pill segments on a time ruler)
// -----------------------------------------------------------------------------

data class LaTimelineSegment(
    val startHourFraction: Float, // 0f..24f
    val endHourFraction: Float,   // 0f..24f
    val label: String,
    val isHighlighted: Boolean = false
)

@Composable
fun LaSessionTimeline(
    segments: List<LaTimelineSegment>,
    modifier: Modifier = Modifier,
    tint: TintSteps = LaTheme.colors.tints.mint,
    emptyMessage: String = "No focus intervals recorded on the timeline yet.",
    accessibilitySummary: String
) {
    val colors = LaTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilitySummary }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(CircleShape)
                .background(tint.soft),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Hourly ruler tick marks
                for (hour in 0..24 step 3) {
                    val x = (hour / 24f) * size.width
                    drawLine(
                        color = colors.outline.copy(alpha = 0.7f),
                        start = Offset(x, size.height * 0.2f),
                        end = Offset(x, size.height * 0.8f),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Active focused intervals (excluding pauses)
                segments.forEach { seg ->
                    val startFrac = (seg.startHourFraction / 24f).coerceIn(0f, 1f)
                    val endFrac = (seg.endHourFraction / 24f).coerceIn(startFrac, 1f)
                    val left = startFrac * size.width
                    val width = ((endFrac - startFrac) * size.width).coerceAtLeast(10.dp.toPx())
                    val pillH = size.height * 0.68f
                    val top = (size.height - pillH) / 2f
                    drawRoundRect(
                        color = if (seg.isHighlighted) colors.ink else tint.strong,
                        topLeft = Offset(left, top),
                        size = Size(width.coerceAtMost(size.width - left), pillH),
                        cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
                    )
                }
            }

            if (segments.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = LaTheme.typography.chartMicroLabel,
                    color = colors.onTintSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { tickLabel ->
                Text(
                    text = tickLabel,
                    style = LaTheme.typography.chartMicroLabel,
                    color = colors.textSecondary
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Section 11.2: Goal Gauge (270-degree arc with rounded caps and end dot knob)
// -----------------------------------------------------------------------------

@Composable
fun LaGoalGauge(
    progress: Float,
    centerText: String,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    trackColor: Color = Color.White.copy(alpha = 0.55f),
    activeColor: Color = LaTheme.colors.ink
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = if (rememberReducedMotion()) 0 else 360),
        label = "goal_gauge"
    )
    val colors = LaTheme.colors

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 9.dp.toPx()
            val diameter = min(this.size.width, this.size.height) - strokePx * 1.6f
            val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
            val startAngle = 135f
            val maxSweep = 270f

            // Background 270° track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = maxSweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (animatedProgress > 0f) {
                val sweep = (maxSweep * animatedProgress).coerceAtLeast(6f)
                drawArc(
                    color = activeColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Knob dot at end of arc
                val endRad = Math.toRadians((startAngle + sweep).toDouble())
                val radius = diameter / 2f
                val cx = this.size.width / 2f + (cos(endRad) * radius).toFloat()
                val cy = this.size.height / 2f + (sin(endRad) * radius).toFloat()
                drawCircle(
                    color = colors.surface,
                    radius = strokePx * 0.72f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = activeColor,
                    radius = strokePx * 0.45f,
                    center = Offset(cx, cy)
                )
            }
        }

        Text(
            text = centerText,
            style = LaTheme.typography.label.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
    }
}

// -----------------------------------------------------------------------------
// 19 November 2026 Update: Performance-Over-Time Line Chart
// -----------------------------------------------------------------------------

@Composable
fun LaPerformanceLineChart(
    points: List<com.lifeanalytics.app.domain.analytics.PerformanceTrendPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tint: TintSteps = LaTheme.colors.tints.lavender,
    chartHeight: Dp = 152.dp,
    emptyMessage: String = "Classify habits or tasks into a Life Area to see your performance trend over time.",
    accessibilitySummary: String
) {
    val colors = LaTheme.colors
    val hasAnyScore = points.any { it.overallScore != null }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilitySummary }
    ) {
        if (!hasAnyScore) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .clip(LaTheme.radii.shapeMd)
                    .background(tint.soft.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    style = LaTheme.typography.label,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        val safeSelectedIdx = selectedIndex.coerceIn(0, (points.size - 1).coerceAtLeast(0))
        val selectedPoint = points.getOrNull(safeSelectedIdx)

        // Selected point readout pill
        if (selectedPoint != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedPoint.label,
                    style = LaTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                val badgeText = selectedPoint.overallScore?.let { "Overall: $it%" } ?: "No scored data"
                Box(
                    modifier = Modifier
                        .clip(LaTheme.radii.shapeFull)
                        .background(if (selectedPoint.overallScore != null) colors.ink else colors.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.Bold),
                        color = if (selectedPoint.overallScore != null) colors.onInk else colors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(points.size) {
                    if (points.isNotEmpty()) {
                        detectTapGestures { tapOffset ->
                            val slotW = size.width / points.size.coerceAtLeast(1)
                            val idx = (tapOffset.x / slotW).toInt().coerceIn(0, points.lastIndex)
                            onSelectIndex(idx)
                        }
                    }
                }
        ) {
            val count = points.size.coerceAtLeast(1)
            val slotW = size.width / count
            val topPad = 14.dp.toPx()
            val bottomPad = 14.dp.toPx()
            val usableH = (size.height - topPad - bottomPad).coerceAtLeast(10f)

            // Horizontal reference lines (0%, 50%, 100%)
            listOf(0f, 0.5f, 1f).forEach { frac ->
                val y = topPad + usableH * (1f - frac)
                drawLine(
                    color = colors.outline.copy(alpha = 0.45f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Compute coordinates for points that have a valid score
            val scoredCoords = mutableListOf<Pair<Int, Offset>>()
            points.forEachIndexed { idx, pt ->
                val x = idx * slotW + slotW / 2f
                val score = pt.overallScore
                if (score != null) {
                    val norm = (score.coerceIn(0, 100)) / 100f
                    val y = topPad + usableH * (1f - norm)
                    scoredCoords.add(idx to Offset(x, y))
                } else {
                    // Draw a subtle missing-data marker on the baseline
                    val baseY = topPad + usableH
                    drawCircle(
                        color = colors.outline,
                        radius = 3.5.dp.toPx(),
                        center = Offset(x, baseY),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // Fill area and connecting line between consecutive scored points
            if (scoredCoords.size >= 2) {
                val fillPath = Path().apply {
                    val first = scoredCoords.first().second
                    val last = scoredCoords.last().second
                    moveTo(first.x, topPad + usableH)
                    scoredCoords.forEach { (_, pt) -> lineTo(pt.x, pt.y) }
                    lineTo(last.x, topPad + usableH)
                    close()
                }
                drawPath(
                    path = fillPath,
                    color = tint.base.copy(alpha = 0.35f)
                )

                val linePath = Path().apply {
                    scoredCoords.forEachIndexed { i, (_, pt) ->
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = colors.ink,
                    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Draw nodes
            scoredCoords.forEach { (idx, center) ->
                val isSel = idx == safeSelectedIdx
                drawCircle(
                    color = colors.surface,
                    radius = if (isSel) 7.5.dp.toPx() else 5.5.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = if (isSel) colors.ink else tint.strong,
                    radius = if (isSel) 5.5.dp.toPx() else 3.8.dp.toPx(),
                    center = center
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // X-axis bucket labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            points.forEachIndexed { idx, pt ->
                val isSel = idx == safeSelectedIdx
                Text(
                    text = pt.label,
                    style = LaTheme.typography.chartMicroLabel.copy(
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSel) colors.textPrimary else colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .weight(1f)
                        .pressScaleClickable { onSelectIndex(idx) }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 19 November 2026 Update: 365-Day (366 in Leap Years) Year View Calendar
// -----------------------------------------------------------------------------

/**
 * Interactive 365/366-Day Year View (`docs/04_HOME_DASHBOARD.md` & `docs/09_LIFE_ANALYTICS.md`):
 * - One dot/cell per calendar day of the year (20 columns × ~19 rows).
 * - Visually distinguishes 3 distinct states:
 *   1. `ELAPSED_SCORED`: Filled rounded cell with lavender-to-ink color intensity (levels 1..4).
 *   2. `ELAPSED_NO_DATA`: Distinct missing-data style (visible neutral ring outline with center dot),
 *      NEVER using the lowest-performance color (`tint.soft`).
 *   3. `FUTURE`: Subtle unfilled/ghost dot (`outline` at 24% opacity) for remaining days in the year.
 * - Tapping any day cell invokes [onSelectDay] with the tapped [com.lifeanalytics.app.domain.analytics.YearDayCellModel].
 */
@Composable
fun LaYear365Calendar(
    calendar: com.lifeanalytics.app.domain.analytics.YearCalendarModel,
    selectedDate: java.time.LocalDate?,
    onSelectDay: (com.lifeanalytics.app.domain.analytics.YearDayCellModel) -> Unit,
    modifier: Modifier = Modifier,
    tint: TintSteps = LaTheme.colors.tints.lavender,
    showSummaryHeader: Boolean = true
) {
    val colors = LaTheme.colors
    val columns = 20
    val rows = (calendar.days.size + columns - 1) / columns

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${calendar.year} year view: ${calendar.elapsedDaysCount} of ${calendar.totalDaysInYear} days elapsed, ${calendar.scoredDaysCount} days with recorded performance scores."
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showSummaryHeader) {
            // Summary strip: Elapsed vs Remaining + Scored days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${calendar.elapsedDaysCount} / ${calendar.totalDaysInYear} days elapsed",
                    style = LaTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                Text(
                    text = "${calendar.scoredDaysCount} scored day(s)",
                    style = LaTheme.typography.chartMicroLabel,
                    color = colors.textSecondary
                )
            }
        }

        // Interactive Canvas Grid for 365/366 days
        val cellHeightDp = 15.dp
        val gridHeight = cellHeightDp * rows

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .pointerInput(calendar.year, calendar.days.size) {
                    detectTapGestures { tapOffset ->
                        val colW = size.width / columns
                        val rowH = size.height / rows
                        val col = (tapOffset.x / colW).toInt().coerceIn(0, columns - 1)
                        val row = (tapOffset.y / rowH).toInt().coerceIn(0, rows - 1)
                        val idx = row * columns + col
                        if (idx in calendar.days.indices) {
                            onSelectDay(calendar.days[idx])
                        }
                    }
                }
        ) {
            val colW = size.width / columns
            val rowH = size.height / rows
            val cellSize = min(colW, rowH) * 0.76f
            val cornerPx = cellSize * 0.34f

            calendar.days.forEachIndexed { idx, dayCell ->
                val row = idx / columns
                val col = idx % columns
                val cx = col * colW + colW / 2f
                val cy = row * rowH + rowH / 2f
                val topLeft = Offset(cx - cellSize / 2f, cy - cellSize / 2f)
                val isSelected = selectedDate == dayCell.date

                when (dayCell.state) {
                    com.lifeanalytics.app.domain.analytics.YearCalendarDayState.ELAPSED_SCORED -> {
                        val fillCol = when (dayCell.intensityLevel) {
                            1 -> tint.soft
                            2 -> tint.base
                            3 -> tint.strong
                            else -> colors.ink
                        }
                        drawRoundRect(
                            color = fillCol,
                            topLeft = topLeft,
                            size = Size(cellSize, cellSize),
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )
                    }

                    com.lifeanalytics.app.domain.analytics.YearCalendarDayState.ELAPSED_NO_DATA -> {
                        // Distinct missing-data style for elapsed days: outlined square + micro center dot (never tint.soft)
                        drawRoundRect(
                            color = colors.textSecondary.copy(alpha = 0.48f),
                            topLeft = topLeft,
                            size = Size(cellSize, cellSize),
                            cornerRadius = CornerRadius(cornerPx, cornerPx),
                            style = Stroke(width = 1.25.dp.toPx())
                        )
                        drawCircle(
                            color = colors.textSecondary.copy(alpha = 0.45f),
                            radius = cellSize * 0.14f,
                            center = Offset(cx, cy)
                        )
                    }

                    com.lifeanalytics.app.domain.analytics.YearCalendarDayState.FUTURE -> {
                        // Future days remain neutral/unfilled subtle ghost dots
                        drawCircle(
                            color = colors.outline.copy(alpha = 0.42f),
                            radius = cellSize * 0.22f,
                            center = Offset(cx, cy)
                        )
                    }
                }

                if (isSelected) {
                    drawRoundRect(
                        color = colors.ink,
                        topLeft = Offset(topLeft.x - 1.5.dp.toPx(), topLeft.y - 1.5.dp.toPx()),
                        size = Size(cellSize + 3.dp.toPx(), cellSize + 3.dp.toPx()),
                        cornerRadius = CornerRadius(cornerPx + 1.5.dp.toPx(), cornerPx + 1.5.dp.toPx()),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }
            }
        }

        // Legend distinguishing: Elapsed (no data), Scored intensity (Low..High), and Future
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Elapsed no-data legend item
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(11.dp)) {
                        drawRoundRect(
                            color = colors.textSecondary.copy(alpha = 0.55f),
                            size = size,
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                        drawCircle(
                            color = colors.textSecondary.copy(alpha = 0.55f),
                            radius = size.minDimension * 0.16f
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "No score",
                        style = LaTheme.typography.chartMicroLabel,
                        color = colors.textSecondary
                    )
                }

                // Future legend item
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(11.dp)) {
                        drawCircle(
                            color = colors.outline.copy(alpha = 0.55f),
                            radius = size.minDimension * 0.26f
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Future",
                        style = LaTheme.typography.chartMicroLabel,
                        color = colors.textSecondary
                    )
                }
            }

            // Scored intensity scale
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Score",
                    style = LaTheme.typography.chartMicroLabel,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                listOf(tint.soft, tint.base, tint.strong, colors.ink).forEach { col ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.5.dp)
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(col)
                    )
                }
            }
        }
    }
}

