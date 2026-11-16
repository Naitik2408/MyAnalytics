package com.lifeanalytics.app.core.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

enum class SupportedCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val fractionDigits: Int = 2
) {
    INR("INR", "₹", "Indian Rupee (₹)", 2),
    USD("USD", "₹", "Indian Rupee (₹)", 2),
    EUR("EUR", "₹", "Indian Rupee (₹)", 2),
    GBP("GBP", "₹", "Indian Rupee (₹)", 2);

    companion object {
        fun fromCode(code: String?): SupportedCurrency {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INR
        }
    }
}

/**
 * Exact integer minor-unit money utilities (docs/08_FINANCE.md Section 6.3, 11_DATA_ARCHITECTURE.md Section 5.5,
 * and 03_UI_UX_GUIDELINES.md v1.3 Rule 7).
 * Never uses binary floating-point (Float/Double) for parsing, storing, or calculating money.
 * Formats with Rupee symbol ₹ and Indian digit grouping (₹12,34,567).
 */
object MoneyUtils {

    /**
     * Parses a user-entered decimal string into exact minor units (paise).
     * Returns null if empty, malformed, negative, zero, or has more decimal places than supported.
     */
    fun parseToMinorUnitsOrNull(
        rawInput: String,
        currency: SupportedCurrency = SupportedCurrency.INR
    ): Long? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null
        // Reject currency symbols or unexpected characters
        if (!trimmed.matches(Regex("^\\d+(\\.\\d+)?$"))) return null

        return try {
            val decimal = BigDecimal(trimmed)
            if (decimal <= BigDecimal.ZERO) return null
            if (decimal.scale() > currency.fractionDigits) return null
            val scaled = decimal.movePointRight(currency.fractionDigits)
            val exactLong = scaled.longValueExact()
            if (exactLong <= 0L) null else exactLong
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Converts minor units back to a plain editable string for edit forms (e.g. "350" or "350.50").
     */
    fun minorUnitsToEditableString(
        amountMinor: Long,
        currency: SupportedCurrency = SupportedCurrency.INR
    ): String {
        val bd = BigDecimal.valueOf(amountMinor).movePointLeft(currency.fractionDigits)
        return bd.stripTrailingZeros().toPlainString()
    }

    /**
     * Formats a non-negative whole number string using Indian digit grouping (e.g. 1234567 -> "12,34,567").
     */
    fun formatIndianGroupedInteger(wholeDigits: String): String {
        val clean = wholeDigits.trimStart('0').ifEmpty { "0" }
        if (clean.length <= 3) return clean
        val lastThree = clean.substring(clean.length - 3)
        val leading = clean.substring(0, clean.length - 3)
        val groups = mutableListOf<String>()
        var idx = leading.length
        while (idx > 0) {
            val start = (idx - 2).coerceAtLeast(0)
            groups.add(0, leading.substring(start, idx))
            idx = start
        }
        return groups.joinToString(",") + "," + lastThree
    }

    /**
     * Formats integer minor units with Rupee symbol ₹ and Indian digit grouping (₹12,34,567).
     */
    fun formatMinorUnits(
        amountMinor: Long,
        @Suppress("UNUSED_PARAMETER") currencyCode: String = SupportedCurrency.INR.code,
        showPositiveSign: Boolean = false
    ): String {
        val isNegative = amountMinor < 0L
        val absMinor = kotlin.math.abs(amountMinor)
        val wholeMajor = absMinor / 100L
        val remainderMinor = absMinor % 100L

        val groupedWhole = formatIndianGroupedInteger(wholeMajor.toString())
        val fractionPart = if (remainderMinor != 0L) {
            String.format(Locale.US, ".%02d", remainderMinor)
        } else {
            ""
        }
        val prefix = when {
            isNegative -> "-"
            showPositiveSign && amountMinor > 0L -> "+"
            else -> ""
        }
        return "$prefix₹$groupedWhole$fractionPart"
    }

    fun formatSignedNetMinorUnits(
        amountMinor: Long,
        currencyCode: String = SupportedCurrency.INR.code
    ): String = formatMinorUnits(
        amountMinor = amountMinor,
        currencyCode = currencyCode,
        showPositiveSign = true
    )
}
