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
import com.menash.myself.feature.money.data.TransactionRepository
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.Donation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class DonationEditor(val draft: DonationDraft, val isNew: Boolean, val invalid: Boolean = false, val confirmDelete: Boolean = false, val saving: Boolean = false, val saveError: String? = null)

/** Which sheet is open on the maaser screen. */
data class TitheSheets(val donation: DonationEditor? = null, val settings: TitheSettingsDraft? = null)

data class TitheUi(
    val open: Boolean = false,
    /** Null until a start month is saved. */
    val view: TitheView? = null,
    val month: YearMonth? = null,
    val today: LocalDate? = null,
    /** Assets that can be the investment portfolio. */
    val assets: List<Asset> = emptyList(),
    val sheets: TitheSheets = TitheSheets(),
)

/** "מעשרות": the maaser and chomesh balance, month by month, with the donations and the counting settings. */
class TitheViewModel(
    private val money: MoneyRepository,
    transactions: TransactionRepository,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
    private val events: UiEvents? = null,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : BaseViewModel(errorReporter) {

    private val open = MutableStateFlow(false)
    private val sheets = MutableStateFlow(TitheSheets())
    private val month: YearMonth get() = YearMonth.from(today())

    val ui: StateFlow<TitheUi> = combine(
        open, money.observe(), transactions.observeTransactions(), transactions.observeMemory(), sheets,
    ) { isOpen, data, stored, memory, open ->
        TitheUi(isOpen, titheFromImports(data, month, stored, memory), month, today(), data.assets.filter { it.kind == AssetKind.ASSET }, open)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TitheUi())

    fun open() {
        open.value = true
    }

    fun close() {
        if (sheets.value.donation?.saving == true) return
        open.value = false
        sheets.value = TitheSheets()
    }

    fun startDonation() {
        sheets.value = TitheSheets(donation = DonationEditor(DonationDraft(date = today()), isNew = true))
    }

    fun editDonation(donation: Donation) {
        sheets.value = TitheSheets(donation = DonationEditor(donationDraftOf(donation), isNew = false))
    }

    fun updateDonation(transform: (DonationDraft) -> DonationDraft) {
        sheets.update { it.copy(donation = it.donation?.let { editor -> if (editor.saving) editor else editor.copy(draft = transform(editor.draft), invalid = false) }) }
    }

    fun saveDonation() {
        val editor = sheets.value.donation ?: return
        if (editor.saving) return
        val donation = buildDonation(editor.draft, newId)
        if (donation == null) {
            sheets.update { it.copy(donation = editor.copy(invalid = true)) }
            return
        }
        sheets.update { it.copy(donation = editor.copy(draft = editor.draft.copy(id = donation.id), saving = true, saveError = null)) }
        safeLaunch("saveDonation", onError = { error -> sheets.update { it.copy(donation = it.donation?.copy(saving = false, saveError = com.menash.myself.core.ui.ErrorMapper.toFriendlyMessage(error))) } }) {
            money.save(MoneyEntry.DonationEntry(donation))
            sheets.value = TitheSheets()
            events?.send(UiEvent.DONATION_SAVED)
        }
    }

    fun requestDeleteDonation(confirm: Boolean) {
        if (sheets.value.donation?.saving == true) return
        sheets.update { it.copy(donation = it.donation?.copy(confirmDelete = confirm)) }
    }

    fun deleteDonation() {
        if (sheets.value.donation?.saving == true) return
        val id = sheets.value.donation?.draft?.id ?: return
        sheets.value = TitheSheets()
        safeLaunch("deleteDonation") { money.delete(MoneyCollection.DONATIONS, id); events?.send(UiEvent.DELETED) }
    }

    fun startSettings() {
        sheets.value = TitheSheets(settings = settingsDraftOf(ui.value.view?.settings, month))
    }

    fun updateSettings(transform: (TitheSettingsDraft) -> TitheSettingsDraft) {
        sheets.update { it.copy(settings = it.settings?.let(transform)) }
    }

    fun saveSettings() {
        val draft = sheets.value.settings ?: return
        sheets.value = TitheSheets()
        safeLaunch("saveTithe") { money.save(MoneyEntry.TitheEntry(buildSettings(draft))) }
    }

    fun dismissSheets() {
        if (sheets.value.donation?.saving != true) sheets.value = TitheSheets()
    }

    companion object {
        fun factory(
            money: MoneyRepository,
            transactions: TransactionRepository,
            today: () -> LocalDate,
            errorReporter: ErrorReporter,
            events: UiEvents? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TitheViewModel(money, transactions, today, errorReporter, events = events) as T
        }
    }
}
