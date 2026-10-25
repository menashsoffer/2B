package com.menash.myself.core.domain.model

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Pure domain value object representing money in agorot (1 NIS = 100 agorot).
 * Prevents floating point inaccuracy across financial calculations.
 */
data class Money(val agorot: Long) : Comparable<Money> {

    val shekels: Double
        get() = agorot / 100.0

    operator fun plus(other: Money): Money = Money(agorot + other.agorot)

    operator fun minus(other: Money): Money = Money(agorot - other.agorot)

    operator fun times(factor: Double): Money = Money((agorot * factor).roundToLong())

    operator fun times(factor: Long): Money = Money(agorot * factor)

    operator fun times(factor: Int): Money = Money(agorot * factor.toLong())

    operator fun div(divisor: Long): Money {
        require(divisor != 0L) { "Divisor cannot be zero" }
        return Money(agorot / divisor)
    }

    operator fun div(divisor: Int): Money {
        require(divisor != 0) { "Divisor cannot be zero" }
        return Money(agorot / divisor.toLong())
    }

    operator fun unaryMinus(): Money = Money(-agorot)

    override fun compareTo(other: Money): Int = agorot.compareTo(other.agorot)

    fun isZero(): Boolean = agorot == 0L

    fun isPositive(): Boolean = agorot > 0L

    fun isNegative(): Boolean = agorot < 0L

    /**
     * Formats the monetary amount in Hebrew NIS style (e.g. "₪1,250" or "₪1,250.50").
     */
    fun formatShekels(includeAgorot: Boolean = false): String {
        val absShekels = kotlin.math.abs(shekels)
        val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
            if (includeAgorot) {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            } else {
                minimumFractionDigits = 0
                maximumFractionDigits = 0
            }
        }
        val formattedNumber = fmt.format(absShekels)
        return if (agorot < 0) "-₪$formattedNumber" else "₪$formattedNumber"
    }

    companion object {
        val ZERO = Money(0L)

        fun fromAgorot(agorot: Long): Money = Money(agorot)

        fun fromShekels(shekels: Double): Money = Money((shekels * 100.0).roundToLong())

        fun fromShekels(shekels: Long): Money = Money(shekels * 100L)
    }
}
