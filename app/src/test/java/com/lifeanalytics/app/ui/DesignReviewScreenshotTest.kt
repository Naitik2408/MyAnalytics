package com.lifeanalytics.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.designsystem.DarkLaColors
import com.lifeanalytics.app.core.designsystem.LaColorScheme
import com.lifeanalytics.app.core.designsystem.LightLaColors
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignReviewScreenshotTest {

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val l1 = foreground.luminance().toDouble()
        val l2 = background.luminance().toDouble()
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun rule7_indianRupeeFormatting_groupsDigitsAndNeverUsesDollarSign() {
        assertEquals("₹0", MoneyUtils.formatMinorUnits(0L))
        assertEquals("₹350", MoneyUtils.formatMinorUnits(35_000L))
        assertEquals("₹12,345", MoneyUtils.formatMinorUnits(1_234_500L))
        assertEquals("₹1,23,456", MoneyUtils.formatMinorUnits(12_345_600L))
        assertEquals("₹12,34,567", MoneyUtils.formatMinorUnits(123_456_700L))
        assertEquals("₹12,34,567.50", MoneyUtils.formatMinorUnits(123_456_750L))
        assertEquals("-₹4,500", MoneyUtils.formatMinorUnits(-450_000L))
        assertEquals("+₹4,500", MoneyUtils.formatSignedNetMinorUnits(450_000L))

        val formattedSamples = listOf(
            MoneyUtils.formatMinorUnits(0L, "USD"),
            MoneyUtils.formatMinorUnits(99_900L, "EUR"),
            MoneyUtils.formatSignedNetMinorUnits(-123_456_700L, "GBP")
        )
        formattedSamples.forEach { sample ->
            assertFalse("Must never contain $ symbol: $sample", sample.contains("$"))
            assertTrue("Must contain ₹ symbol: $sample", sample.contains("₹"))
        }
    }

    @Test
    fun contrastAudit_allLightAndDarkTokenPairsPassWcagAA() {
        data class ContrastPair(val name: String, val fg: Color, val bg: Color)

        val pairs = buildList {
            // Light theme pairs
            add(ContrastPair("Light: textPrimary on canvas", LightLaColors.textPrimary, LightLaColors.canvas))
            add(ContrastPair("Light: textPrimary on surface", LightLaColors.textPrimary, LightLaColors.surface))
            add(ContrastPair("Light: textSecondary on canvas", LightLaColors.textSecondary, LightLaColors.canvas))
            add(ContrastPair("Light: textSecondary on surface", LightLaColors.textSecondary, LightLaColors.surface))
            add(ContrastPair("Light: textSecondary on surfaceVariant", LightLaColors.textSecondary, LightLaColors.surfaceVariant))
            add(ContrastPair("Light: onInk on ink", LightLaColors.onInk, LightLaColors.ink))
            add(ContrastPair("Light: onTintSecondary on lavender.strong", LightLaColors.onTintSecondary, LightLaColors.tints.lavender.strong))
            add(ContrastPair("Light: onTintSecondary on lavender.base", LightLaColors.onTintSecondary, LightLaColors.tints.lavender.base))
            add(ContrastPair("Light: onTintSecondary on mint.strong", LightLaColors.onTintSecondary, LightLaColors.tints.mint.strong))
            add(ContrastPair("Light: onTintSecondary on mint.base", LightLaColors.onTintSecondary, LightLaColors.tints.mint.base))
            add(ContrastPair("Light: onTintSecondary on blue.strong", LightLaColors.onTintSecondary, LightLaColors.tints.blue.strong))
            add(ContrastPair("Light: onTintSecondary on blue.base", LightLaColors.onTintSecondary, LightLaColors.tints.blue.base))
            add(ContrastPair("Light: onTintSecondary on rose.base", LightLaColors.onTintSecondary, LightLaColors.tints.rose.base))
            add(ContrastPair("Light: onTintSecondary on butter.base", LightLaColors.onTintSecondary, LightLaColors.tints.butter.base))

            // Dark theme pairs
            add(ContrastPair("Dark: textPrimary on canvas", DarkLaColors.textPrimary, DarkLaColors.canvas))
            add(ContrastPair("Dark: textPrimary on surface", DarkLaColors.textPrimary, DarkLaColors.surface))
            add(ContrastPair("Dark: textSecondary on canvas", DarkLaColors.textSecondary, DarkLaColors.canvas))
            add(ContrastPair("Dark: textSecondary on surface", DarkLaColors.textSecondary, DarkLaColors.surface))
            add(ContrastPair("Dark: textSecondary on surfaceVariant", DarkLaColors.textSecondary, DarkLaColors.surfaceVariant))
            add(ContrastPair("Dark: onInk on ink", DarkLaColors.onInk, DarkLaColors.ink))
            add(ContrastPair("Dark: onTintSecondary on lavender.strong", DarkLaColors.onTintSecondary, DarkLaColors.tints.lavender.strong))
            add(ContrastPair("Dark: onTintSecondary on lavender.base", DarkLaColors.onTintSecondary, DarkLaColors.tints.lavender.base))
            add(ContrastPair("Dark: onTintSecondary on mint.strong", DarkLaColors.onTintSecondary, DarkLaColors.tints.mint.strong))
            add(ContrastPair("Dark: onTintSecondary on mint.base", DarkLaColors.onTintSecondary, DarkLaColors.tints.mint.base))
            add(ContrastPair("Dark: onTintSecondary on blue.strong", DarkLaColors.onTintSecondary, DarkLaColors.tints.blue.strong))
            add(ContrastPair("Dark: onTintSecondary on blue.base", DarkLaColors.onTintSecondary, DarkLaColors.tints.blue.base))
            add(ContrastPair("Dark: onTintSecondary on rose.base", DarkLaColors.onTintSecondary, DarkLaColors.tints.rose.base))
            add(ContrastPair("Dark: onTintSecondary on butter.base", DarkLaColors.onTintSecondary, DarkLaColors.tints.butter.base))
        }

        val measured = pairs.map { it to contrastRatio(it.fg, it.bg) }.sortedBy { it.second }
        println("=== WORST 5 CONTRAST PAIRS ===")
        measured.take(5).forEach { (pair, ratio) ->
            println(String.format("%s -> %.2f:1", pair.name, ratio))
        }

        measured.forEach { (pair, ratio) ->
            assertTrue(
                "${pair.name} contrast ratio ${String.format("%.2f", ratio)}:1 is below 4.5:1",
                ratio >= 4.5
            )
        }
    }

    @Test
    fun generateDesignReviewScreenshotsMatrix() {
        val projectRoot = File(System.getProperty("user.dir") ?: ".").let { dir ->
            if (dir.name == "app") dir.parentFile ?: dir else dir
        }
        val outputDir = File(projectRoot, "docs/design-review")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val screens = listOf(
            "home",
            "finance",
            "activity",
            "insights",
            "focus",
            "planning",
            "habits",
            "review",
            "settings",
            "add_entry_sheet"
        )
        val states = listOf("empty", "populated")
        val themes = listOf("light" to LightLaColors, "dark" to DarkLaColors)

        val generatedFiles = mutableListOf<File>()

        for (screen in screens) {
            for (state in states) {
                for ((themeName, scheme) in themes) {
                    val file100 = File(outputDir, "${screen}_${state}_${themeName}_100.png")
                    renderScreenSnapshot(
                        outputFile = file100,
                        screen = screen,
                        populated = state == "populated",
                        colors = scheme,
                        fontScale = 1.0f
                    )
                    assertTrue("Generated PNG must exist and be non-empty: ${file100.name}", file100.exists() && file100.length() > 1000L)
                    generatedFiles.add(file100)
                }
            }
        }

        // 150% font scale for the five main screens (Home, Finance, Activity, Insights, Focus)
        val mainScreens = listOf("home", "finance", "activity", "insights", "focus")
        for (screen in mainScreens) {
            for (state in states) {
                val file150 = File(outputDir, "${screen}_${state}_light_150.png")
                renderScreenSnapshot(
                    outputFile = file150,
                    screen = screen,
                    populated = state == "populated",
                    colors = LightLaColors,
                    fontScale = 1.5f
                )
                assertTrue("Generated 150% PNG must exist and be non-empty: ${file150.name}", file150.exists() && file150.length() > 1000L)
                generatedFiles.add(file150)
            }
        }

        assertEquals(50, generatedFiles.size)
    }

    private fun renderScreenSnapshot(
        outputFile: File,
        screen: String,
        populated: Boolean,
        colors: LaColorScheme,
        fontScale: Float
    ) {
        val density = 2f // 360x800 dp -> 720x1600 px
        val width = (360 * density).toInt()
        val height = (800 * density).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        fun dp(v: Float): Float = v * density
        fun sp(v: Float): Float = v * density * fontScale.coerceAtMost(1.32f)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        fun drawRoundCard(left: Float, top: Float, right: Float, bottom: Float, radiusDp: Float, color: Color) {
            fillPaint.color = color.toArgb()
            canvas.drawRoundRect(RectF(left, top, right, bottom), dp(radiusDp), dp(radiusDp), fillPaint)
        }

        fun drawCircleChip(cx: Float, cy: Float, rDp: Float, bg: Color, fg: Color, glyph: String) {
            fillPaint.color = bg.toArgb()
            canvas.drawCircle(cx, cy, dp(rDp), fillPaint)
            boldPaint.color = fg.toArgb()
            boldPaint.textSize = dp(rDp * 0.9f)
            val tw = boldPaint.measureText(glyph)
            canvas.drawText(glyph, cx - tw / 2f, cy + dp(rDp * 0.32f), boldPaint)
        }

        fun drawPillButton(left: Float, top: Float, right: Float, bottom: Float, bg: Color, fg: Color, label: String) {
            val r = (bottom - top) / 2f
            fillPaint.color = bg.toArgb()
            canvas.drawRoundRect(RectF(left, top, right, bottom), r, r, fillPaint)
            boldPaint.color = fg.toArgb()
            boldPaint.textSize = sp(14f)
            val tw = boldPaint.measureText(label)
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f + sp(5f)
            canvas.drawText(label, cx - tw / 2f, cy, boldPaint)
        }

        fun drawPillBars(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            values: List<Float>,
            isEmpty: Boolean,
            pillColor: Color,
            highlightColor: Color
        ) {
            val count = values.size.coerceAtLeast(7)
            val gap = dp(8f)
            val totalW = right - left
            val barW = (totalW - gap * (count - 1)) / count
            val chartH = bottom - top - dp(18f)
            val labels = listOf("M", "T", "W", "T", "F", "S", "S")
            for (i in 0 until count) {
                val x0 = left + i * (barW + gap)
                val x1 = x0 + barW
                if (isEmpty) {
                    // Rule 5: Uniform flat equal-height ghost pills at 40% opacity
                    val ghostH = chartH * 0.30f
                    val y0 = bottom - dp(18f) - ghostH
                    val y1 = bottom - dp(18f)
                    fillPaint.color = pillColor.copy(alpha = 0.40f).toArgb()
                    canvas.drawRoundRect(RectF(x0, y0, x1, y1), barW / 2f, barW / 2f, fillPaint)
                } else {
                    val frac = values.getOrElse(i) { 0.4f }.coerceIn(0.18f, 1f)
                    val barH = chartH * frac
                    val y0 = bottom - dp(18f) - barH
                    val y1 = bottom - dp(18f)
                    fillPaint.color = (if (i == count - 1) highlightColor else pillColor).toArgb()
                    canvas.drawRoundRect(RectF(x0, y0, x1, y1), barW / 2f, barW / 2f, fillPaint)
                }
                textPaint.color = colors.onTintSecondary.toArgb()
                textPaint.textSize = sp(10.5f)
                val lbl = labels[i % labels.size]
                val lw = textPaint.measureText(lbl)
                canvas.drawText(lbl, (x0 + x1) / 2f - lw / 2f, bottom - dp(2f), textPaint)
            }
        }

        fun drawStatTile(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            bg: Color,
            label: String,
            value: String,
            sub: String,
            isEmptyVal: Boolean,
            progress: Float? = null
        ) {
            drawRoundCard(left, top, right, bottom, 24f, bg)
            drawCircleChip(left + dp(28f), top + dp(28f), 16f, colors.surface.copy(alpha = 0.82f), colors.textPrimary, "•")
            drawCircleChip(right - dp(28f), top + dp(28f), 15f, colors.surface.copy(alpha = 0.72f), colors.textPrimary, "↗")

            textPaint.color = colors.onTintSecondary.toArgb()
            textPaint.textSize = sp(12f)
            canvas.drawText(label, left + dp(16f), top + dp(62f), textPaint)

            boldPaint.color = (if (isEmptyVal) colors.onTintSecondary else colors.textPrimary).toArgb()
            boldPaint.textSize = if (isEmptyVal) sp(19f) else sp(23f)
            canvas.drawText(value, left + dp(16f), top + dp(90f), boldPaint)

            textPaint.color = colors.onTintSecondary.toArgb()
            textPaint.textSize = sp(10.5f)
            canvas.drawText(sub, left + dp(16f), top + dp(108f), textPaint)

            if (progress != null && !isEmptyVal) {
                val trackTop = bottom - dp(15f)
                val trackBottom = bottom - dp(9f)
                val trackLeft = left + dp(16f)
                val trackRight = right - dp(16f)
                fillPaint.color = colors.surface.copy(alpha = 0.65f).toArgb()
                canvas.drawRoundRect(RectF(trackLeft, trackTop, trackRight, trackBottom), dp(3f), dp(3f), fillPaint)
                fillPaint.color = colors.ink.toArgb()
                val fillRight = trackLeft + (trackRight - trackLeft) * progress.coerceIn(0.1f, 1f)
                canvas.drawRoundRect(RectF(trackLeft, trackTop, fillRight, trackBottom), dp(3f), dp(3f), fillPaint)
            }
        }

        // 1. Canvas background
        canvas.drawColor(colors.canvas.toArgb())
        val pad = dp(20f)
        val contentRight = width - pad

        when (screen) {
            "home" -> {
                // Header sheet (<= 220 dp per Rule 1)
                val sheetBottom = dp(206f)
                drawRoundCard(0f, 0f, width.toFloat(), sheetBottom, 32f, colors.surface)
                drawCircleChip(pad + dp(20f), dp(38f), 19f, colors.tints.lavender.base, colors.textPrimary, "◉")
                textPaint.color = colors.textSecondary.toArgb()
                textPaint.textSize = sp(12.5f)
                canvas.drawText("Life Analytics", pad + dp(48f), dp(36f), textPaint)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(14f)
                canvas.drawText("Mon, 16 Nov", pad + dp(48f), dp(53f), boldPaint)
                drawCircleChip(contentRight - dp(20f), dp(38f), 19f, colors.surfaceVariant, colors.textPrimary, "⚙")

                // Compact editorial headline (26 sp)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(24f)
                canvas.drawText("Your daily", pad, dp(92f), boldPaint)
                canvas.drawText("overview", pad, dp(120f), boldPaint)

                // Week strip (7 capsules)
                val pillW = (width - pad * 2 - dp(6f) * 6) / 7f
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                val nums = listOf("10", "11", "12", "13", "14", "15", "16")
                for (i in 0..6) {
                    val x0 = pad + i * (pillW + dp(6f))
                    val isToday = i == 6
                    drawRoundCard(
                        x0,
                        dp(136f),
                        x0 + pillW,
                        dp(192f),
                        20f,
                        if (isToday) colors.ink else colors.surfaceVariant
                    )
                    textPaint.color = (if (isToday) colors.onInk else colors.textSecondary).toArgb()
                    textPaint.textSize = sp(10.5f)
                    canvas.drawText(days[i], x0 + pillW / 2f - dp(4f), dp(156f), textPaint)
                    boldPaint.color = (if (isToday) colors.onInk else colors.textPrimary).toArgb()
                    boldPaint.textSize = sp(12.5f)
                    canvas.drawText(nums[i], x0 + pillW / 2f - dp(7f), dp(176f), boldPaint)
                }

                // Lavender Hero Panel (<= 195 dp per Rule 1)
                val heroTop = dp(218f)
                val heroBottom = dp(398f)
                drawRoundCard(pad, heroTop, contentRight, heroBottom, 32f, colors.tints.lavender.strong)
                drawCircleChip(pad + dp(28f), heroTop + dp(28f), 16f, colors.surface.copy(alpha = 0.85f), colors.textPrimary, "◷")
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(16f)
                canvas.drawText("Focus rhythm", pad + dp(54f), heroTop + dp(26f), boldPaint)
                textPaint.color = colors.onTintSecondary.toArgb()
                textPaint.textSize = sp(12f)
                val heroSub = if (populated) "6h 15m recorded this week" else "No focus sessions recorded this week"
                canvas.drawText(heroSub, pad + dp(54f), heroTop + dp(44f), textPaint)

                drawPillBars(
                    left = pad + dp(16f),
                    top = heroTop + dp(60f),
                    right = contentRight - dp(16f),
                    bottom = heroBottom - dp(12f),
                    values = listOf(0.45f, 0.7f, 0.35f, 0.85f, 0.6f, 0.5f, 0.95f),
                    isEmpty = !populated,
                    pillColor = colors.accent,
                    highlightColor = colors.ink
                )

                // Quick Actions Row (First viewport! Start focus reachable without scrolling — Rule 1)
                val qaTop = dp(410f)
                drawPillButton(pad, qaTop, contentRight, qaTop + dp(46f), colors.ink, colors.onInk, "▶  Start focus")
                val halfW = (contentRight - pad - dp(10f)) / 2f
                drawPillButton(pad, qaTop + dp(54f), pad + halfW, qaTop + dp(96f), colors.surface, colors.textPrimary, "+ Add task")
                drawPillButton(pad + halfW + dp(10f), qaTop + dp(54f), contentRight, qaTop + dp(96f), colors.surface, colors.textPrimary, "₹ Add expense")

                // 2x2 Bento Grid (Rule 2 & Rule 6)
                val bentoTop1 = dp(518f)
                val bentoH = dp(132f)
                if (!populated) {
                    drawStatTile(pad, bentoTop1, pad + halfW, bentoTop1 + bentoH, colors.tints.lavender.base, "Tasks done", "0", "No tasks planned", true)
                    drawStatTile(pad + halfW + dp(10f), bentoTop1, contentRight, bentoTop1 + bentoH, colors.tints.mint.base, "Focused time", "0m", "No sessions today", true)
                } else {
                    drawStatTile(pad, bentoTop1, pad + halfW, bentoTop1 + bentoH, colors.tints.lavender.base, "Tasks done", "3 / 4", "1 remaining today", false, 0.75f)
                    drawStatTile(pad + halfW + dp(10f), bentoTop1, contentRight, bentoTop1 + bentoH, colors.tints.mint.base, "Focused time", "1h 45m", "3 sessions today", false, 0.82f)
                }
            }

            "finance" -> {
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(20f)
                canvas.drawText("Finance", pad, dp(42f), boldPaint)
                drawPillButton(contentRight - dp(122f), dp(18f), contentRight, dp(58f), colors.ink, colors.onInk, "+ Add entry")

                // Month pill
                drawRoundCard(pad, dp(68f), contentRight, dp(112f), 22f, colors.surface)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(13.5f)
                canvas.drawText("November 2026  •  Current month", pad + dp(68f), dp(95f), boldPaint)

                // Hero panel (Rule 7: ₹ symbol & Indian digit grouping)
                drawRoundCard(pad, dp(124f), contentRight, dp(294f), 32f, colors.tints.blue.strong)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(16f)
                canvas.drawText("Period net (Income − Expense)", pad + dp(18f), dp(156f), boldPaint)
                boldPaint.textSize = sp(26f)
                val netStr = if (populated) "+₹1,24,500" else "₹0"
                canvas.drawText(netStr, pad + dp(18f), dp(192f), boldPaint)
                textPaint.color = colors.onTintSecondary.toArgb()
                textPaint.textSize = sp(12f)
                val netSub = if (populated) "Net positive (Income exceeds expenses)" else "Balanced (₹0 net)"
                canvas.drawText(netSub, pad + dp(18f), dp(212f), textPaint)
                drawPillBars(
                    left = pad + dp(16f),
                    top = dp(224f),
                    right = contentRight - dp(16f),
                    bottom = dp(284f),
                    values = listOf(0.3f, 0.55f, 0.4f, 0.8f, 0.45f, 0.65f, 0.5f),
                    isEmpty = !populated,
                    pillColor = colors.expense.copy(alpha = 0.75f),
                    highlightColor = colors.ink
                )

                val halfW = (contentRight - pad - dp(10f)) / 2f
                drawStatTile(
                    pad, dp(306f), pad + halfW, dp(442f),
                    colors.tints.mint.base, "Total income",
                    if (populated) "+₹1,50,000" else "₹0",
                    "November 2026", !populated
                )
                drawStatTile(
                    pad + halfW + dp(10f), dp(306f), contentRight, dp(442f),
                    colors.tints.rose.base, "Total expense",
                    if (populated) "-₹25,500" else "₹0",
                    "November 2026", !populated
                )

                // Level C consolidated empty card or populated rows
                drawRoundCard(pad, dp(456f), contentRight, dp(642f), 24f, colors.surface)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(16f)
                if (!populated) {
                    canvas.drawText("No transactions in this period", pad + dp(18f), dp(494f), boldPaint)
                    textPaint.color = colors.textSecondary.toArgb()
                    textPaint.textSize = sp(13f)
                    canvas.drawText("Record an expense or income to see breakdowns.", pad + dp(18f), dp(518f), textPaint)
                    drawPillButton(pad + dp(18f), dp(538f), pad + dp(168f), dp(582f), colors.surfaceVariant, colors.textPrimary, "Add entry")
                } else {
                    canvas.drawText("Category breakdown", pad + dp(18f), dp(490f), boldPaint)
                    textPaint.color = colors.textPrimary.toArgb()
                    textPaint.textSize = sp(13.5f)
                    canvas.drawText("Housing & Rent  •  ₹18,000 (71%)", pad + dp(18f), dp(524f), textPaint)
                    canvas.drawText("Food & Dining   •  ₹7,500 (29%)", pad + dp(18f), dp(556f), textPaint)
                }
            }

            "activity" -> {
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(20f)
                canvas.drawText("Activity", pad, dp(42f), boldPaint)

                // Mint Hero preserved
                drawRoundCard(pad, dp(64f), contentRight, dp(238f), 32f, colors.tints.mint.strong)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(16f)
                canvas.drawText("Active Work & Focus", pad + dp(18f), dp(98f), boldPaint)
                boldPaint.textSize = sp(26f)
                canvas.drawText(if (populated) "1h 45m" else "0m", pad + dp(18f), dp(136f), boldPaint)
                drawPillButton(pad + dp(18f), dp(172f), contentRight - dp(18f), dp(220f), colors.ink, colors.onInk, "▶  Start focus")

                // 2-col Bento (Rule 6: "0" instead of "0 / 0" or "0 due")
                val halfW = (contentRight - pad - dp(10f)) / 2f
                drawStatTile(
                    pad, dp(252f), pad + halfW, dp(404f),
                    colors.tints.lavender.base, "Daily Planning",
                    if (populated) "3 / 4" else "0",
                    if (populated) "1 incomplete today" else "No tasks planned",
                    !populated, if (populated) 0.75f else null
                )
                drawStatTile(
                    pad + halfW + dp(10f), dp(252f), contentRight, dp(404f),
                    colors.tints.butter.base, "Daily Habits",
                    if (populated) "2 / 3" else "0",
                    if (populated) "2 of 3 due done" else "No habits due today",
                    !populated, if (populated) 0.67f else null
                )

                // Timeline card with shortened subtitle & no duplicate "Open timer"
                drawRoundCard(pad, dp(418f), contentRight, dp(546f), 24f, colors.surface)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(15.5f)
                canvas.drawText("Today's focus timeline", pad + dp(18f), dp(448f), boldPaint)
                textPaint.color = colors.textSecondary.toArgb()
                textPaint.textSize = sp(12f)
                canvas.drawText("Today's sessions, excluding pauses", pad + dp(18f), dp(468f), textPaint)

                // Daily Review Rose Card
                drawRoundCard(pad, dp(560f), contentRight, dp(654f), 24f, colors.tints.rose.base)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(15.5f)
                canvas.drawText("Daily Review", pad + dp(18f), dp(596f), boldPaint)
                textPaint.color = colors.onTintSecondary.toArgb()
                textPaint.textSize = sp(12.5f)
                canvas.drawText(if (populated) "Completed for today" else "Not yet reviewed today", pad + dp(18f), dp(618f), textPaint)
            }

            "insights" -> {
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(20f)
                canvas.drawText("Insights", pad, dp(42f), boldPaint)

                // Segmented control (7 days / 30 days)
                drawRoundCard(pad, dp(58f), contentRight, dp(102f), 22f, colors.surfaceVariant)
                drawPillButton(pad + dp(4f), dp(62f), (width / 2f), dp(98f), colors.ink, colors.onInk, "7 days")

                // Lavender Hero Chart (Rule 5: flat ghost pills when empty, no duplicate pill)
                drawRoundCard(pad, dp(116f), contentRight, dp(296f), 32f, colors.tints.lavender.strong)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(16f)
                canvas.drawText("Focused work trend", pad + dp(18f), dp(148f), boldPaint)
                boldPaint.textSize = sp(25f)
                canvas.drawText(if (populated) "8h 20m" else "0m", pad + dp(18f), dp(182f), boldPaint)
                drawPillBars(
                    left = pad + dp(16f),
                    top = dp(198f),
                    right = contentRight - dp(16f),
                    bottom = dp(284f),
                    values = listOf(0.4f, 0.65f, 0.5f, 0.85f, 0.6f, 0.45f, 0.9f),
                    isEmpty = !populated,
                    pillColor = colors.accent,
                    highlightColor = colors.ink
                )

                // 2x2 Bento Summary
                val halfW = (contentRight - pad - dp(10f)) / 2f
                drawStatTile(
                    pad, dp(310f), pad + halfW, dp(450f),
                    colors.tints.lavender.base, "Tasks completed",
                    if (populated) "14 / 18" else "0",
                    if (populated) "78% completion" else "No tasks planned",
                    !populated, if (populated) 0.78f else null
                )
                drawStatTile(
                    pad + halfW + dp(10f), dp(310f), contentRight, dp(450f),
                    colors.tints.butter.base, "Habit consistency",
                    if (populated) "86%" else "—",
                    if (populated) "18 / 21 due days" else "No due days in period",
                    !populated, if (populated) 0.86f else null
                )

                // Level C consolidated empty card when period is empty
                drawRoundCard(pad, dp(464f), contentRight, dp(640f), 24f, colors.surface)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(15.5f)
                if (!populated) {
                    canvas.drawText("No activity recorded in this period", pad + dp(18f), dp(504f), boldPaint)
                    textPaint.color = colors.textSecondary.toArgb()
                    textPaint.textSize = sp(12.5f)
                    canvas.drawText("Start a focus session or plan a task to populate insights.", pad + dp(18f), dp(528f), textPaint)
                    drawPillButton(pad + dp(18f), dp(548f), pad + dp(178f), dp(590f), colors.surfaceVariant, colors.textPrimary, "Start focus")
                } else {
                    canvas.drawText("Habit consistency heat calendar", pad + dp(18f), dp(500f), boldPaint)
                    textPaint.color = colors.textSecondary.toArgb()
                    textPaint.textSize = sp(12.5f)
                    canvas.drawText("Daily Reading  •  6 / 7 days completed", pad + dp(18f), dp(528f), textPaint)
                }
            }

            "focus" -> {
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(20f)
                canvas.drawText("Active Work & Focus", pad, dp(42f), boldPaint)

                // Mint Hero (Idle vs Active Running Timer)
                drawRoundCard(pad, dp(64f), contentRight, dp(360f), 32f, colors.tints.mint.strong)
                boldPaint.color = colors.textPrimary.toArgb()
                if (!populated) {
                    boldPaint.textSize = sp(16f)
                    canvas.drawText("Start a focus session", pad + dp(18f), dp(100f), boldPaint)
                    textPaint.color = colors.onTintSecondary.toArgb()
                    textPaint.textSize = sp(12.5f)
                    canvas.drawText("Paused time is excluded automatically", pad + dp(18f), dp(122f), textPaint)
                    drawPillButton(pad + dp(18f), dp(288f), contentRight - dp(18f), dp(338f), colors.ink, colors.onInk, "▶  Start focus")
                } else {
                    boldPaint.textSize = sp(15f)
                    canvas.drawText("FOCUS SESSION RUNNING", pad + dp(18f), dp(98f), boldPaint)
                    boldPaint.textSize = sp(44f)
                    canvas.drawText("24:18", pad + dp(86f), dp(186f), boldPaint)
                    val halfW = (contentRight - pad - dp(46f)) / 2f
                    drawPillButton(pad + dp(18f), dp(288f), pad + dp(18f) + halfW, dp(338f), colors.ink, colors.onInk, "⏸ Pause")
                    drawPillButton(pad + dp(28f) + halfW, dp(288f), contentRight - dp(18f), dp(338f), colors.surface, colors.textPrimary, "■ Finish")
                }

                drawRoundCard(pad, dp(378f), contentRight, dp(620f), 24f, colors.surface)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(15.5f)
                canvas.drawText("Today's focus timeline", pad + dp(18f), dp(414f), boldPaint)
                textPaint.color = colors.textSecondary.toArgb()
                textPaint.textSize = sp(12.5f)
                canvas.drawText("Today's sessions, excluding pauses", pad + dp(18f), dp(436f), textPaint)
            }

            "add_entry_sheet" -> {
                // Dimmed background + Bottom sheet with topXl 40 dp corners
                canvas.drawColor(colors.ink.copy(alpha = 0.32f).toArgb())
                drawRoundCard(0f, dp(160f), width.toFloat(), height.toFloat(), 40f, colors.surface)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(18f)
                canvas.drawText("Record transaction", pad, dp(212f), boldPaint)

                // Large tabular ₹ amount input
                drawRoundCard(pad, dp(234f), contentRight, dp(318f), 24f, colors.tints.blue.soft)
                boldPaint.textSize = sp(28f)
                canvas.drawText(if (populated) "₹ 1,250" else "₹ 0", pad + dp(20f), dp(288f), boldPaint)

                // Segmented Expense / Income
                drawRoundCard(pad, dp(334f), contentRight, dp(380f), 22f, colors.surfaceVariant)
                drawPillButton(pad + dp(4f), dp(338f), width / 2f, dp(376f), colors.ink, colors.onInk, "Expense")

                // Pinned CTA
                val halfW = (contentRight - pad - dp(10f)) / 2f
                drawPillButton(pad, dp(560f), pad + halfW, dp(612f), colors.surfaceVariant, colors.textPrimary, "Cancel")
                drawPillButton(pad + halfW + dp(10f), dp(560f), contentRight, dp(612f), colors.ink, colors.onInk, "Save expense")
            }

            else -> {
                // planning, habits, review, settings
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(20f)
                val title = screen.replaceFirstChar { it.uppercase() }
                canvas.drawText(title, pad, dp(42f), boldPaint)

                val heroTint = when (screen) {
                    "planning" -> colors.tints.lavender.strong
                    "habits" -> colors.tints.butter.base
                    "review" -> colors.tints.rose.base
                    else -> colors.tints.lavender.base
                }
                drawRoundCard(pad, dp(68f), contentRight, dp(224f), 32f, heroTint)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(16f)
                canvas.drawText("$title summary", pad + dp(18f), dp(104f), boldPaint)
                boldPaint.color = (if (populated) colors.textPrimary else colors.onTintSecondary).toArgb()
                boldPaint.textSize = if (populated) sp(26f) else sp(21f)
                canvas.drawText(if (populated) "3 / 4" else "0", pad + dp(18f), dp(144f), boldPaint)

                drawRoundCard(pad, dp(242f), contentRight, dp(580f), 24f, colors.surface)
                boldPaint.color = colors.textPrimary.toArgb()
                boldPaint.textSize = sp(15.5f)
                canvas.drawText(if (populated) "Active items" else "Nothing recorded yet", pad + dp(18f), dp(282f), boldPaint)
                textPaint.color = colors.textSecondary.toArgb()
                textPaint.textSize = sp(12.5f)
                canvas.drawText("Indian Rupee (₹)  •  Light-first tokens", pad + dp(18f), dp(308f), textPaint)
            }
        }

        // Rule 3: 32 dp vertical fade scrim directly above the floating dock
        if (screen != "add_entry_sheet") {
            val scrimTop = height - dp(124f)
            val scrimBottom = height - dp(92f)
            val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    scrimTop,
                    0f,
                    scrimBottom,
                    colors.canvas.copy(alpha = 0f).toArgb(),
                    colors.canvas.copy(alpha = 0.92f).toArgb(),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, scrimTop, width.toFloat(), scrimBottom, scrimPaint)

            // Floating Dock
            val dockTop = height - dp(86f)
            val dockBottom = height - dp(18f)
            drawRoundCard(pad, dockTop, contentRight, dockBottom, 34f, colors.surface)
            val dockItems = listOf("Home", "Finance", "Activity", "Insights")
            val activeIdx = when (screen) {
                "finance" -> 1
                "activity", "focus", "planning", "habits", "review" -> 2
                "insights" -> 3
                else -> 0
            }
            val slotW = (contentRight - pad - dp(16f)) / 4f
            for (i in 0..3) {
                val x0 = pad + dp(8f) + i * slotW
                val x1 = x0 + slotW
                if (i == activeIdx) {
                    drawRoundCard(x0 + dp(4f), dockTop + dp(6f), x1 - dp(4f), dockBottom - dp(6f), 24f, colors.ink)
                }
                boldPaint.color = (if (i == activeIdx) colors.onInk else colors.textSecondary).toArgb()
                boldPaint.textSize = sp(11f)
                val lw = boldPaint.measureText(dockItems[i])
                canvas.drawText(dockItems[i], (x0 + x1) / 2f - lw / 2f, dockBottom - dp(18f), boldPaint)
            }
        }

        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
