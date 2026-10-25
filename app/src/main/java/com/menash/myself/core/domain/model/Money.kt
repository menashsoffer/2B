package com.menash.myself.core.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Pure domain value object representing money in agorot (1 NIS = 100 agorot).
 * Uses consistent HALF_UP rounding policy for multiplication and division.
 */
data class Money(val agorot: Long) : Comparable<Money> {

    val agoras: Long
        get() = agorot

    val shekels: Double
        get() = agorot / 100.0

    operator fun plus(other: Money): Money = Money(agorot + other.agorot)

    operator fun minus(other: Money): Money = Money(agorot - other.agorot)

    operator fun times(factor: Double): Money {
        val bd = BigDecimal.valueOf(agorot).multiply(BigDecimal.valueOf(factor))
        return Money(bd.setScale(0, RoundingMode.HALF_UP).longValueExact())
    }

    operator fun times(factor: Long): Money = Money(agorot * factor)

    operator fun times(factor: Int): Money = Money(agorot * factor.toLong())

    operator fun div(divisor: Long): Money {
        require(divisor != 0L) { "Divisor cannot be zero" }
        val bd = BigDecimal.valueOf(agorot).divide(BigDecimal.valueOf(divisor), 0, RoundingMode.HALF_UP)
        return Money(bd.longValueExact())
    }

    operator fun div(divisor: Int): Money = div(divisor.toLong())

    operator fun div(divisor: Double): Money {
        require(divisor != 0.0) { "Divisor cannot be zero" }
        val bd = BigDecimal.valueOf(agorot).divide(BigDecimal.valueOf(divisor), 0, RoundingMode.HALF_UP)
        return Money(bd.longValueExact())
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

        fun fromShekels(shekels: Double): Money {
            val bd = BigDecimal.valueOf(shekels).multiply(BigDecimal.valueOf(100.0))
            return Money(bd.setScale(0, RoundingMode.HALF_UP).longValueExact())
        }

        fun fromShekels(shekels: Long): Money = Money(shekels * 100L)
    }
}
