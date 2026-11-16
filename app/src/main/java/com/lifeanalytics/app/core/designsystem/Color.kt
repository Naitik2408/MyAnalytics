package com.lifeanalytics.app.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Theme Tokens (docs/03_UI_UX_GUIDELINES.md Section 4.1)
val LightPrimary = Color(0xFF6558D3)
val LightPrimaryContainer = Color(0xFFEAE7FF)
val LightSecondary = Color(0xFF4776C8)
val LightBackground = Color(0xFFF7F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F1F7)
val LightTextPrimary = Color(0xFF202235)
val LightTextSecondary = Color(0xFF686B7D)
val LightOutline = Color(0xFFDCDDE8)
val LightSuccess = Color(0xFF23865A)
val LightWarning = Color(0xFFB7791F)
val LightError = Color(0xFFC44752)
val LightIncome = Color(0xFF23865A)
val LightExpense = Color(0xFFC44752)
val SuccessGreen = LightSuccess

// Dark Theme Tokens (docs/03_UI_UX_GUIDELINES.md Section 4.2)
val DarkPrimary = Color(0xFFA69BFF)
val DarkPrimaryContainer = Color(0xFF302A59)
val DarkSecondary = Color(0xFF8FB5FF)
val DarkBackground = Color(0xFF12131B)
val DarkSurface = Color(0xFF1C1E29)
val DarkSurfaceVariant = Color(0xFF272A38)
val DarkTextPrimary = Color(0xFFF2F2FA)
val DarkTextSecondary = Color(0xFFB1B3C2)
val DarkOutline = Color(0xFF3B3E4D)
val DarkSuccess = Color(0xFF63C995)
val DarkWarning = Color(0xFFE5B45E)
val DarkError = Color(0xFFF07A84)
val DarkIncome = Color(0xFF63C995)
val DarkExpense = Color(0xFFF07A84)

@Immutable
data class ExtendedColors(
    val success: Color,
    val warning: Color,
    val income: Color,
    val expense: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = LightSuccess,
        warning = LightWarning,
        income = LightIncome,
        expense = LightExpense,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary
    )
}
