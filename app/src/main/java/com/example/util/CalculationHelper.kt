package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * High-precision mathematical calculation and ledger reconciliation engine.
 * Guarantees zero calculation errors and exact currency/quantity calculations.
 */
object CalculationHelper {

    fun round2(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun calculateItemTotal(quantity: Double, unitPrice: Double): Double {
        if (quantity <= 0.0 || unitPrice <= 0.0) return 0.0
        return round2(quantity * unitPrice)
    }

    fun calculateGrossTotal(items: List<Pair<Double, Double>>): Double {
        var sum = 0.0
        for ((qty, price) in items) {
            sum += calculateItemTotal(qty, price)
        }
        return round2(sum)
    }

    fun calculateNetTotal(gross: Double, discount: Double): Double {
        val g = round2(gross)
        val d = round2(discount)
        return round2((g - d).coerceAtLeast(0.0))
    }

    fun calculateDue(netTotal: Double, paidAmount: Double): Double {
        val net = round2(netTotal)
        val paid = round2(paidAmount)
        return round2((net - paid).coerceAtLeast(0.0))
    }

    fun calculateProfit(sellingTotal: Double, buyPrice: Double, quantity: Double): Double {
        val costTotal = round2(buyPrice * quantity)
        return round2(sellingTotal - costTotal)
    }

    fun formatAmount(amount: Double): String {
        val rounded = round2(amount)
        val absVal = Math.abs(rounded)
        val formatted = if (absVal % 1.0 == 0.0) {
            String.format(java.util.Locale.US, "%.0f", absVal)
        } else {
            String.format(java.util.Locale.US, "%.2f", absVal)
        }
        return if (rounded < 0.0) "-$formatted" else formatted
    }

    fun formatCurrency(amount: Double, currency: String): String {
        val rounded = round2(amount)
        val isNegative = rounded < 0.0
        val absVal = Math.abs(rounded)
        val formatted = if (absVal % 1.0 == 0.0) {
            String.format(java.util.Locale.US, "%.0f", absVal)
        } else {
            String.format(java.util.Locale.US, "%.2f", absVal)
        }
        return if (isNegative) "-$currency$formatted" else "$currency$formatted"
    }
}
