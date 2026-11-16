package com.lifeanalytics.app.core.designsystem

import android.app.Activity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.lifeanalytics.app.R

// =============================================================================
// 03_UI_UX_GUIDELINES.md (v1.2) — Sections 5 & 6: Typography, Spacing, Radii
// =============================================================================

val UrbanistFontFamily: FontFamily = try {
    FontFamily(
        Font(R.font.urbanist_regular, FontWeight.Normal),
        Font(R.font.urbanist_medium, FontWeight.Medium),
        Font(R.font.urbanist_semibold, FontWeight.SemiBold),
        Font(R.font.urbanist_bold, FontWeight.Bold)
    )
} catch (_: Exception) {
    FontFamily.SansSerif
}

@Immutable
data class LaTypeScale(
    // Editorial two-line headline on Home only (34-38 sp / 1.08, Bold 700, tracking -1%)
    val editorialHeadline: TextStyle,
    // Title on sub-screens & primary headers (21 sp, Semibold 600)
    val screenTitle: TextStyle,
    // Big number in a stat tile or hero (30 sp, Bold 700, tabular figures)
    val metricValue: TextStyle,
    // Balanced text-state empty value in a stat tile (16 sp, Medium 500)
    val metricEmptyValue: TextStyle,
    // Active focus timer (48 sp, Semibold 600, tabular figures)
    val timerValue: TextStyle,
    // Section headings ("Today's Tasks", "This week") (17 sp, Semibold 600)
    val sectionTitle: TextStyle,
    // Card / sheet item title (16 sp, Semibold 600)
    val cardTitle: TextStyle = sectionTitle,
    // Main content and descriptions (15 sp, Regular 400)
    val body: TextStyle,
    // Emphasised body / button labels (15 sp, SemiBold 600)
    val buttonLabel: TextStyle,
    // Tile labels, row metadata (13.5 sp, Medium 500)
    val label: TextStyle,
    // Chart day labels and direct value labels (11.5 sp, Medium 500, tabular figures)
    val chartMicroLabel: TextStyle
)

val DefaultLaTypeScale = LaTypeScale(
    editorialHeadline = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.26).sp
    ),
    screenTitle = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp
    ),
    metricValue = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontFeatureSettings = "tnum"
    ),
    metricEmptyValue = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontFeatureSettings = "tnum"
    ),
    timerValue = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 52.sp,
        lineHeight = 58.sp,
        fontFeatureSettings = "tnum"
    ),
    sectionTitle = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    cardTitle = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    body = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    buttonLabel = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    label = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    chartMicroLabel = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        fontFeatureSettings = "tnum"
    )
)

// Section 6.1 Spacing Tokens (4 dp base)
@Immutable
data class LaSpacing(
    val space1: Dp = 4.dp,
    val space2: Dp = 8.dp,
    val space3: Dp = 12.dp,
    val space4: Dp = 16.dp,
    val space5: Dp = 20.dp,
    val space6: Dp = 24.dp,
    val space7: Dp = 28.dp,
    val space8: Dp = 32.dp,
    val space10: Dp = 40.dp,
    val screenInset: Dp = 20.dp,
    val tileGutter: Dp = 12.dp,
    val tilePadding: Dp = 16.dp,
    val heroPadding: Dp = 20.dp,
    val cardPaddingLarge: Dp = 20.dp,
    val sectionGap: Dp = 24.dp,
    val dockHeight: Dp = 68.dp,
    val dockBottomMargin: Dp = 14.dp,
    // Total bottom content padding so scrollable content clears the floating dock and its 32 dp scrim cleanly
    val dockBottomClearance: Dp = 128.dp
)

// Section 6.2 Corner Radius Tokens
@Immutable
data class LaRadii(
    val sm: Dp = 16.dp,
    val md: Dp = 24.dp,
    val lg: Dp = 32.dp,
    val xl: Dp = 40.dp,
    val heatCell: Dp = 11.dp
) {
    val shapeSm: RoundedCornerShape get() = RoundedCornerShape(sm)
    val shapeMd: RoundedCornerShape get() = RoundedCornerShape(md)
    val shapeLg: RoundedCornerShape get() = RoundedCornerShape(lg)
    val shapeXlTop: RoundedCornerShape
        get() = RoundedCornerShape(topStart = xl, topEnd = xl)
    val shapeHeaderSheetBottom: RoundedCornerShape
        get() = RoundedCornerShape(bottomStart = lg, bottomEnd = lg)
    val shapeFull = CircleShape

    /**
     * Concentric radius rule (Section 6.2): inner radius = outer radius - padding.
     */
    fun concentricInner(outer: Dp, padding: Dp): RoundedCornerShape {
        val inner = (outer - padding).coerceAtLeast(8.dp)
        return RoundedCornerShape(inner)
    }
}

val LocalLaTypeScale = staticCompositionLocalOf { DefaultLaTypeScale }
val LocalLaSpacing = staticCompositionLocalOf { LaSpacing() }
val LocalLaRadii = staticCompositionLocalOf { LaRadii() }

object LaTheme {
    val colors: LaColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalLaColors.current

    val typography: LaTypeScale
        @Composable
        @ReadOnlyComposable
        get() = LocalLaTypeScale.current

    val spacing: LaSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalLaSpacing.current

    val radii: LaRadii
        @Composable
        @ReadOnlyComposable
        get() = LocalLaRadii.current
}

private val LightMaterialColorScheme = lightColorScheme(
    primary = LightLaColors.ink,
    onPrimary = LightLaColors.onInk,
    primaryContainer = LightLaTints.lavender.base,
    onPrimaryContainer = LightLaColors.textPrimary,
    secondary = LightLaColors.accent,
    onSecondary = LightLaColors.onInk,
    secondaryContainer = LightLaTints.lavender.soft,
    onSecondaryContainer = LightLaColors.textPrimary,
    background = LightLaColors.canvas,
    onBackground = LightLaColors.textPrimary,
    surface = LightLaColors.surface,
    onSurface = LightLaColors.textPrimary,
    surfaceVariant = LightLaColors.surfaceVariant,
    onSurfaceVariant = LightLaColors.textSecondary,
    outline = LightLaColors.outline,
    error = LightLaColors.error,
    onError = LightLaColors.onInk
)

private val DarkMaterialColorScheme = darkColorScheme(
    primary = DarkLaColors.ink,
    onPrimary = DarkLaColors.onInk,
    primaryContainer = DarkLaTints.lavender.base,
    onPrimaryContainer = DarkLaColors.textPrimary,
    secondary = DarkLaColors.accent,
    onSecondary = DarkLaColors.onInk,
    secondaryContainer = DarkLaTints.lavender.soft,
    onSecondaryContainer = DarkLaColors.textPrimary,
    background = DarkLaColors.canvas,
    onBackground = DarkLaColors.textPrimary,
    surface = DarkLaColors.surface,
    onSurface = DarkLaColors.textPrimary,
    surfaceVariant = DarkLaColors.surfaceVariant,
    onSurfaceVariant = DarkLaColors.textSecondary,
    outline = DarkLaColors.outline,
    error = DarkLaColors.error,
    onError = DarkLaColors.onInk
)

val LifeAnalyticsTypography = Typography(
    displayMedium = DefaultLaTypeScale.timerValue,
    headlineLarge = DefaultLaTypeScale.editorialHeadline,
    headlineSmall = DefaultLaTypeScale.metricValue,
    titleLarge = DefaultLaTypeScale.screenTitle,
    titleMedium = DefaultLaTypeScale.sectionTitle,
    bodyLarge = DefaultLaTypeScale.body,
    bodyMedium = DefaultLaTypeScale.label,
    labelLarge = DefaultLaTypeScale.buttonLabel,
    labelMedium = DefaultLaTypeScale.chartMicroLabel
)

val LifeAnalyticsShapes = Shapes(
    extraSmall = RoundedCornerShape(11.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

@Composable
fun LifeAnalyticsTheme(
    darkTheme: Boolean = false, // Light-first default (guidelines section 3.1 & 4)
    content: @Composable () -> Unit
) {
    val laColors = if (darkTheme) DarkLaColors else LightLaColors
    val materialScheme = if (darkTheme) DarkMaterialColorScheme else LightMaterialColorScheme
    val extendedColors = ExtendedColors(
        success = laColors.success,
        warning = laColors.warning,
        income = laColors.income,
        expense = laColors.expense,
        textPrimary = laColors.textPrimary,
        textSecondary = laColors.textSecondary
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                @Suppress("DEPRECATION")
                window.statusBarColor = laColors.canvas.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = laColors.canvas.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalLaColors provides laColors,
        LocalLaTypeScale provides DefaultLaTypeScale,
        LocalLaSpacing provides LaSpacing(),
        LocalLaRadii provides LaRadii(),
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = LifeAnalyticsTypography,
            shapes = LifeAnalyticsShapes,
            content = content
        )
    }
}
