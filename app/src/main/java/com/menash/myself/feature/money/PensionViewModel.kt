package com.menash.myself.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.core.ui.UiEvent
import com.menash.myself.core.ui.UiEvents
import com.menash.myself.feature.money.data.MoneyCollection
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.domain.PensionFund
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID

data class FundEditor(val draft: FundDraft, val isNew: Boolean, val invalid: FundField? = null, val confirmDelete: Boolean = false, val saving: Boolean = false, val saveError: String? = null)

data class PensionUi(
    val open: Boolean = false,
    val view: PensionView = PensionView(emptyList(), null),
    val editor: FundEditor? = null,
)

/** "פנסיה וחיסכון": today's picture of each fund, updated by hand from the quarterly report. */
class PensionViewModel(
    private val money: MoneyRepository,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
    private val events: UiEvents? = null,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : BaseViewModel(errorReporter) {

    private val open = MutableStateFlow(false)
    private val editor = MutableStateFlow<FundEditor?>(null)

    val ui: StateFlow<PensionUi> = combine(open, money.observe(), editor) { isOpen, data, current ->
        PensionUi(isOpen, pensionView(data.pensionFunds, today()), current)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PensionUi())

    fun open() {
        open.value = true
    }

    fun close() {
        if (editor.value?.saving == true) return
        open.value = false
        editor.value = null
    }

    fun startAdding() {
        editor.value = FundEditor(FundDraft(), isNew = true)
    }

    fun edit(fund: PensionFund) {
        editor.value = FundEditor(fundDraftOf(fund), isNew = false)
    }

    fun update(transform: (FundDraft) -> FundDraft) {
        editor.update { if (it == null || it.saving) it else it.copy(draft = transform(it.draft), invalid = null) }
    }

    fun save() {
        val current = editor.value ?: return
        if (current.saving) return
        when (val result = buildFund(current.draft, today(), newId)) {
            is FundResult.Invalid -> editor.value = current.copy(invalid = result.field)
            is FundResult.Ok -> {
                editor.value = current.copy(draft = current.draft.copy(id = result.fund.id), saving = true, saveError = null)
                safeLaunch("savePensionFund", onError = { error -> val message = com.menash.myself.core.ui.ErrorMapper.toFriendlyMessage(error); editor.update { it?.copy(saving = false, saveError = message) }; events?.send(UiEvent.SaveError(message) { if (editor.value?.draft?.id == result.fund.id) save() }) }) {
                    money.save(MoneyEntry.PensionFundEntry(result.fund))
                    editor.value = null
                    events?.send(UiEvent.MONEY_SAVED)
                }
            }
        }
    }

    fun requestDelete(confirm: Boolean) {
        if (editor.value?.saving == true) return
        editor.update { it?.copy(confirmDelete = confirm) }
    }

    fun delete() {
        if (editor.value?.saving == true) return
        val id = editor.value?.draft?.id ?: return
        editor.value = null
        safeLaunch("deletePensionFund") { money.delete(MoneyCollection.PENSION_FUNDS, id); events?.send(UiEvent.DELETED) }
    }

    fun dismiss() {
        if (editor.value?.saving != true) editor.value = null
    }

    companion object {
        fun factory(money: MoneyRepository, today: () -> LocalDate, errorReporter: ErrorReporter, events: UiEvents? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = PensionViewModel(money, today, errorReporter, events) as T
            }
    }
}
