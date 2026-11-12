package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** Paamonim expense classes: חודשיות קבועות, שוטפות (monthly budget), תקופתיות (annual budget). */
enum class ExpenseClass { FIXED, CURRENT, PERIODIC }

/** parentId links a sub-item (e.g. עיתון under מנויים) to its category. */
data class Category(val id: String, val name: String, val klass: ExpenseClass, val parentId: String? = null) {
    init { requireId(id) }
    val isDonation: Boolean get() = id == DONATION_CATEGORY_ID || id == STANDING_DONATION_CATEGORY_ID
}

const val DONATION_CATEGORY_ID = "current-donations"
const val STANDING_DONATION_CATEGORY_ID = "fixed-8"

const val MISC_CATEGORY_NAME = "שונות"

const val VEHICLE_ENERGY_CATEGORY_NAME = "דלק / חשמל"

const val MAX_CATEGORY_NAME = 40

/**
 * A category the household adds while sorting lines. Null for a blank or too long name.
 * An existing category with the same name in the same class is reused instead of a duplicate.
 */
fun newCategory(name: String, klass: ExpenseClass, existing: List<Category>, newId: () -> String): Category? {
    val clean = name.trim().replace(Regex("""\s+"""), " ")
    if (clean.isEmpty() || clean.length > MAX_CATEGORY_NAME) return null
    return existing.firstOrNull { it.klass == klass && it.name.equals(clean, ignoreCase = true) }
        ?: Category(newId(), clean, klass)
}

/** Annual budget for a periodic category; the plan reserves annualAmount / 12 every month. */
data class PeriodicBudget(
    val id: String, val categoryId: String, val name: String, val annualAmount: Money, val yearStartMonth: YearMonth,
) {
    init { requireId(id); require(annualAmount >= Money.ZERO) }
}

/** monthlyPayment is paid in each of the remainingPayments months after the month of asOf. */
data class Debt(
    val id: String, val creditor: String, val balance: Money, val asOf: LocalDate,
    val annualInterestPercent: BigDecimal, val remainingPayments: Int, val monthlyPayment: Money,
) {
    init { requireId(id); require(balance >= Money.ZERO && monthlyPayment >= Money.ZERO && remainingPayments >= 0) }
}

/** monthlyDeposit applies from startMonth, when known, through maturity. Null startMonth is legacy/unknown. */
data class Saving(
    val id: String, val name: String, val monthlyDeposit: Money, val accumulated: Money, val maturity: YearMonth? = null,
    val startMonth: YearMonth? = null,
) {
    init { requireId(id); require(monthlyDeposit >= Money.ZERO && accumulated >= Money.ZERO) }
}

private fun cat(id: String, name: String, klass: ExpenseClass, parent: String? = null) = Category(id, name, klass, parent)

/** Default categories from the Paamonim רישומון, with stable ids. */
fun defaultCategories(): List<Category> {
    val fixed = listOf("משכנתא", "ביטוח משכנתא", "שכר דירה", "מיסי ישוב / ועד בית", "ביטוחים (למעט רכב)",
        "הוראות קבע לחיסכון", "מנויים", "תרומות בהוראת קבע")
    val subscriptions = listOf("עיתון", "כבלים/Hot/Yes", "ספריה", "ספק אינטרנט", "הגרלות", "ספורט/חדר כושר", "גוגל", "סינון לטלפון")
    val periodic = listOf("ארנונה / שמירה", "מים וביוב", "חשמל", "גז", "חימום - סולר, נפט", "חינוך", "חוגים, קייטנות ובריכה",
        "ביטוח רכב + טסט", "תיקוני רכב", "ביגוד והנעלה", "בריאות", "עמלות וריביות בנקים", "טיפולי שיניים", "אופטיקה",
        "חופשה / טיול", "יהדות / חגים", "מתנות לאירועים ושמחות", "רכישות ושירותים", "תספורת וקוסמטיקה", "ביטוח לאומי (למי שלא עובד)")
    val current = listOf("מזון", "תחבורה ציבורית", VEHICLE_ENERGY_CATEGORY_NAME, "טלפון נייח", "טלפון נייד", "אחזקת בית", "עוזרת / שמרטף", "סיגריות",
        "אוכל בחוץ", "פארם", "נוחות ומכונות אוטומטיות", "חניה - כביש 6")
    val subscriptionsId = "fixed-" + (fixed.indexOf("מנויים") + 1)
    return fixed.mapIndexed { i, n -> cat("fixed-${i + 1}", n, ExpenseClass.FIXED) } +
        subscriptions.mapIndexed { i, n -> cat("sub-${i + 1}", n, ExpenseClass.FIXED, subscriptionsId) } +
        periodic.mapIndexed { i, n -> cat("periodic-${i + 1}", n, ExpenseClass.PERIODIC) } +
        current.mapIndexed { i, n -> cat("current-${i + 1}", n, ExpenseClass.CURRENT) } +
        cat(DONATION_CATEGORY_ID, "תרומות", ExpenseClass.CURRENT)
}
