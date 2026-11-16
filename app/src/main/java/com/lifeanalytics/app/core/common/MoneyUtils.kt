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
    USD("USD", "$", "US Dollar ($)", 2),
    EUR("EUR", "€", "Euro (€)", 2),
    GBP("GBP", "£", "British Pound (£)", 2);

    companion object {
        fun fromCode(code: String?): SupportedCurrency {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INR
        }
    }
}

/**
 * Exact integer minor-unit money utilities (docs/08_FINANCE.md Section 6.3 & 11_DATA_ARCHITECTURE.md Section 5.5).
 * Never uses binary floating-point (Float/Double) for parsing, storing, or calculating money.
 */
object MoneyUtils {

    /**
     * Parses a user-entered decimal string into exact minor units (e.g., paise or cents).
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
     * Formats integer minor units with currency symbol and grouping separators.
     */
    fun formatMinorUnits(
        amountMinor: Long,
        currencyCode: String = SupportedCurrency.INR.code,
        showPositiveSign: Boolean = false
    ): String {
        val currency = SupportedCurrency.fromCode(currencyCode)
        val isNegative = amountMinor < 0L
        val absMinor = kotlin.math.abs(amountMinor)
        val bd = BigDecimal.valueOf(absMinor)
            .movePointLeft(currency.fractionDigits)
            .setScale(currency.fractionDigits, RoundingMode.UNNECESSARY)

        val hasFraction = absMinor % 100L != 0L
        val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            isGroupingUsed = true
            minimumFractionDigits = if (hasFraction) currency.fractionDigits else 0
            maximumFractionDigits = currency.fractionDigits
        }
        val formattedNumber = numberFormat.format(bd)
        val prefix = when {
            isNegative -> "-"
            showPositiveSign && amountMinor > 0L -> "+"
            else -> ""
        }
        return "$prefix${currency.symbol}$formattedNumber"
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
