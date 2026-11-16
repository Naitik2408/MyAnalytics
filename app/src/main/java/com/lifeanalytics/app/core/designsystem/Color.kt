package com.lifeanalytics.app.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =============================================================================
// 03_UI_UX_GUIDELINES.md (v1.2) — Section 4: Colour System
// =============================================================================

@Immutable
data class TintSteps(
    val strong: Color,
    val base: Color,
    val soft: Color
)

@Immutable
data class LaTints(
    val lavender: TintSteps,
    val mint: TintSteps,
    val blue: TintSteps,
    val rose: TintSteps,
    val butter: TintSteps
)

enum class ModuleTint {
    TASKS,
    FOCUS,
    HABITS,
    FINANCE,
    REVIEW,
    INSIGHTS
}

@Immutable
data class LaColorScheme(
    val isDark: Boolean,
    val canvas: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val ink: Color,
    val onInk: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val onTintSecondary: Color,
    val accent: Color,
    val outline: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val income: Color,
    val expense: Color,
    val tints: LaTints
) {
    fun tintFor(module: ModuleTint): TintSteps = when (module) {
        ModuleTint.TASKS -> tints.lavender
        ModuleTint.FOCUS -> tints.mint
        ModuleTint.HABITS -> tints.butter
        ModuleTint.FINANCE -> tints.blue
        ModuleTint.REVIEW -> tints.rose
        ModuleTint.INSIGHTS -> tints.lavender
    }
}

// 4.1 & 4.2 Light Theme Tokens
val LightLaTints = LaTints(
    lavender = TintSteps(
        strong = Color(0xFFD3C1F3),
        base = Color(0xFFDDD0F7),
        soft = Color(0xFFEFEAFC)
    ),
    mint = TintSteps(
        strong = Color(0xFFC4E4D2),
        base = Color(0xFFD5EBDF),
        soft = Color(0xFFEAF5EF)
    ),
    blue = TintSteps(
        strong = Color(0xFFC9DEF4),
        base = Color(0xFFD9E9F8),
        soft = Color(0xFFECF3FB)
    ),
    rose = TintSteps(
        strong = Color(0xFFEBCFE0),
        base = Color(0xFFF0DDE8),
        soft = Color(0xFFF8EEF3)
    ),
    butter = TintSteps(
        strong = Color(0xFFFFE6A8),
        base = Color(0xFFFFEFC4),
        soft = Color(0xFFFFF8E3)
    )
)

val LightLaColors = LaColorScheme(
    isDark = false,
    canvas = Color(0xFFF4F3F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFEDF6),
    ink = Color(0xFF111A1C),
    onInk = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF111A1C),
    textSecondary = Color(0xFF5C586B),
    onTintSecondary = Color(0xFF3E3A52),
    accent = Color(0xFF7563D8),
    outline = Color(0xFFE2DEED),
    success = Color(0xFF23865A),
    warning = Color(0xFFB7791F),
    error = Color(0xFFC44752),
    income = Color(0xFF23865A),
    expense = Color(0xFFC44752),
    tints = LightLaTints
)

// 4.3 Dark Theme Tokens
val DarkLaTints = LaTints(
    lavender = TintSteps(
        strong = Color(0xFF4A3F7E),
        base = Color(0xFF3A3260),
        soft = Color(0xFF2B2548)
    ),
    mint = TintSteps(
        strong = Color(0xFF2F5A4A),
        base = Color(0xFF24453A),
        soft = Color(0xFF1B332B)
    ),
    blue = TintSteps(
        strong = Color(0xFF2F4C70),
        base = Color(0xFF243B57),
        soft = Color(0xFF1B2D43)
    ),
    rose = TintSteps(
        strong = Color(0xFF69405A),
        base = Color(0xFF503046),
        soft = Color(0xFF3B2434)
    ),
    butter = TintSteps(
        strong = Color(0xFF6A5726),
        base = Color(0xFF51431E),
        soft = Color(0xFF3B3116)
    )
)

val DarkLaColors = LaColorScheme(
    isDark = true,
    canvas = Color(0xFF12131B),
    surface = Color(0xFF1C1E29),
    surfaceVariant = Color(0xFF272A38),
    ink = Color(0xFFEDE7FF),
    onInk = Color(0xFF1A1630),
    textPrimary = Color(0xFFF2F2FA),
    textSecondary = Color(0xFFB8BAD0),
    onTintSecondary = Color(0xFFDDD9F0),
    accent = Color(0xFFA69BFF),
    outline = Color(0xFF3B3E4D),
    success = Color(0xFF63C995),
    warning = Color(0xFFE5B45E),
    error = Color(0xFFF07A84),
    income = Color(0xFF63C995),
    expense = Color(0xFFF07A84),
    tints = DarkLaTints
)

// Legacy aliases retained so existing imports compile during transition
val LightPrimary = LightLaColors.ink
val LightPrimaryContainer = LightLaTints.lavender.soft
val LightSecondary = LightLaColors.accent
val LightBackground = LightLaColors.canvas
val LightSurface = LightLaColors.surface
val LightSurfaceVariant = LightLaColors.surfaceVariant
val LightTextPrimary = LightLaColors.textPrimary
val LightTextSecondary = LightLaColors.textSecondary
val LightOutline = LightLaColors.outline
val LightSuccess = LightLaColors.success
val LightWarning = LightLaColors.warning
val LightError = LightLaColors.error
val LightIncome = LightLaColors.income
val LightExpense = LightLaColors.expense
val SuccessGreen = LightLaColors.success

val DarkPrimary = DarkLaColors.ink
val DarkPrimaryContainer = DarkLaTints.lavender.soft
val DarkSecondary = DarkLaColors.accent
val DarkBackground = DarkLaColors.canvas
val DarkSurface = DarkLaColors.surface
val DarkSurfaceVariant = DarkLaColors.surfaceVariant
val DarkTextPrimary = DarkLaColors.textPrimary
val DarkTextSecondary = DarkLaColors.textSecondary
val DarkOutline = DarkLaColors.outline
val DarkSuccess = DarkLaColors.success
val DarkWarning = DarkLaColors.warning
val DarkError = DarkLaColors.error
val DarkIncome = DarkLaColors.income
val DarkExpense = DarkLaColors.expense

@Immutable
data class ExtendedColors(
    val success: Color,
    val warning: Color,
    val income: Color,
    val expense: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LocalLaColors = staticCompositionLocalOf { LightLaColors }

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = LightLaColors.success,
        warning = LightLaColors.warning,
        income = LightLaColors.income,
        expense = LightLaColors.expense,
        textPrimary = LightLaColors.textPrimary,
        textSecondary = LightLaColors.textSecondary
    )
}
