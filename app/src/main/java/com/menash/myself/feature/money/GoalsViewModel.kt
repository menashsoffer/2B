package com.menash.myself.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.feature.money.data.MoneyCollection
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.domain.Feasibility
import com.menash.myself.feature.money.domain.Goal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** The open goal sheet; [preview] is the live answer for what is typed, null while it is not a goal yet. */
data class GoalEditor(
    val draft: GoalDraft,
    val isNew: Boolean,
    val error: GoalField? = null,
    val preview: Feasibility? = null,
    val confirmDelete: Boolean = false,
)

data class GoalsUi(
    val open: Boolean = false,
    val view: CanWeView = CanWeView(null, null, emptyList()),
    val month: YearMonth? = null,
    val editor: GoalEditor? = null,
)

/** "האם אפשר?": the household's goals, each with the engine's answer, and a sheet to add or change one. */
class GoalsViewModel(
    private val repository: MoneyRepository,
    usdRates: Flow<BigDecimal?>,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : BaseViewModel(errorReporter) {

    private val open = MutableStateFlow(false)
    private val editor = MutableStateFlow<GoalEditor?>(null)
    private var latest = MoneyData()
    private var rate: BigDecimal? = null
    private val month: YearMonth get() = YearMonth.from(today())

    val ui: StateFlow<GoalsUi> = combine(open, repository.observe(), usdRates, editor) { isOpen, data, usd, sheet ->
        latest = data
        rate = usd
        GoalsUi(isOpen, canWe(data, month, usd, closedFigures(data.closedMonths, data.averagesFrom).monthlySaving), month, sheet?.let { it.copy(preview = preview(it.draft)) })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GoalsUi())

    private fun preview(draft: GoalDraft): Feasibility? =
        (buildGoal(draft) { "preview" } as? GoalResult.Ok)?.let { previewAnswer(it.goal, latest, month, rate, closedFigures(latest.closedMonths, latest.averagesFrom).monthlySaving) }

    fun open() {
        open.value = true
    }

    fun close() {
        open.value = false
        editor.value = null
    }

    fun setAveragesFrom(from: YearMonth?) {
        safeLaunch("saveAverages") { repository.save(MoneyEntry.AveragesSettingsEntry(from)) }
    }

    fun startAdding() {
        editor.value = GoalEditor(newGoalDraft(month), isNew = true)
    }

    fun startEditing(goal: Goal) {
        editor.value = GoalEditor(goalDraftOf(goal), isNew = false)
    }

    fun updateDraft(transform: (GoalDraft) -> GoalDraft) {
        editor.update { it?.copy(draft = transform(it.draft), error = null) }
    }

    fun dismissEditor() {
        editor.value = null
    }

    fun save() {
        val current = editor.value ?: return
        when (val result = buildGoal(current.draft, newId)) {
            is GoalResult.Invalid -> editor.value = current.copy(error = result.field)
            is GoalResult.Ok -> {
                editor.value = null
                safeLaunch("saveGoal") { repository.save(MoneyEntry.GoalEntry(result.goal)) }
            }
        }
    }

    fun requestDelete(confirm: Boolean) {
        editor.update { it?.copy(confirmDelete = confirm) }
    }

    fun delete() {
        val id = editor.value?.draft?.id ?: return
        editor.value = null
        safeLaunch("deleteGoal") { repository.delete(MoneyCollection.GOALS, id) }
    }

    companion object {
        fun factory(
            repository: MoneyRepository,
            usdRates: Flow<BigDecimal?>,
            today: () -> LocalDate,
            errorReporter: ErrorReporter,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GoalsViewModel(repository, usdRates, today, errorReporter) as T
        }
    }
}
