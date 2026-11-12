package com.menash.myself.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.data.TransactionRepository
import com.menash.myself.feature.money.domain.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Which sheet is open on the car screen. */
sealed interface CarSheet {
    data class Categories(val selected: Set<String>, val opened: Set<String> = selected) : CarSheet
    data class Compare(val draft: CarCompareDraft, val opened: CarCompareDraft = draft) : CarSheet
}

data class CarUi(
    val open: Boolean = false,
    val current: CarCurrent? = null,
    val plan: CarPlan = CarPlan(),
    /** Null while nothing is typed in the comparison. */
    val compare: CarCompareView? = null,
    /** Expense categories the car can be made of. */
    val categories: List<Category> = emptyList(),
    val sheet: CarSheet? = null,
)

/** "כמה עולה לנו הרכב": today's car from the files, and what a new car costs under each way of paying. */
class CarViewModel(
    private val money: MoneyRepository,
    transactions: TransactionRepository,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
) : BaseViewModel(errorReporter) {

    private val open = MutableStateFlow(false)
    private val sheet = MutableStateFlow<CarSheet?>(null)

    val ui: StateFlow<CarUi> = combine(
        open, money.observe(), transactions.observeTransactions(), transactions.observeMemory(), sheet,
    ) { isOpen, data, stored, memory, current ->
        val plan = data.car ?: CarPlan()
        val car = carCurrent(data, plan, stored, memory, today())
        val compare = carCompare(plan.compare, car).takeUnless { it.isEmpty }
        CarUi(isOpen, car, plan, compare, data.categories.filter { it.parentId == null }, current)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CarUi())

    fun open() {
        open.value = true
    }

    fun close() {
        open.value = false
        sheet.value = null
    }

    fun editCategories() {
        sheet.value = CarSheet.Categories(ui.value.plan.categoryIds)
    }

    fun toggleCategory(id: String) {
        sheet.update { (it as? CarSheet.Categories)?.let { s -> s.copy(selected = if (id in s.selected) s.selected - id else s.selected + id) } ?: it }
    }

    fun saveCategories() {
        val selected = (sheet.value as? CarSheet.Categories)?.selected ?: return
        sheet.value = null
        val plan = ui.value.plan.copy(categoryIds = selected)
        safeLaunch("saveCarCategories") { money.save(MoneyEntry.CarEntry(plan)) }
    }

    fun editCompare() {
        sheet.value = CarSheet.Compare(ui.value.plan.compare)
    }

    fun updateCompare(transform: (CarCompareDraft) -> CarCompareDraft) {
        sheet.update { (it as? CarSheet.Compare)?.let { s -> s.copy(draft = transform(s.draft)) } ?: it }
    }

    fun saveCompare() {
        val draft = (sheet.value as? CarSheet.Compare)?.draft ?: return
        sheet.value = null
        val plan = ui.value.plan.copy(compare = draft)
        safeLaunch("saveCarCompare") { money.save(MoneyEntry.CarEntry(plan)) }
    }

    fun dismiss() {
        sheet.value = null
    }

    companion object {
        fun factory(
            money: MoneyRepository,
            transactions: TransactionRepository,
            today: () -> LocalDate,
            errorReporter: ErrorReporter,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CarViewModel(money, transactions, today, errorReporter) as T
        }
    }
}
