package com.menash.myself.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.InboxItem
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class InboxViewModel(
    private val inboxRepository: InboxRepository,
    private val taskRepository: TaskRepository,
    private val dreamRepository: DreamRepository,
    errorReporter: ErrorReporter
) : BaseViewModel(errorReporter) {

    val inboxItems: StateFlow<List<InboxItem>?> = inboxRepository.observeInbox()
        .map<List<InboxItem>, List<InboxItem>?> { it }
        .catch { e ->
            if (e is Exception) handleException("observeInbox", e) else throw e
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<UiState<List<InboxItem>>> = combine(inboxItems, _errorState) { list, error ->
        when {
            error != null && list.isNullOrEmpty() -> UiState.Error(error)
            list == null -> UiState.Loading
            list.isEmpty() -> UiState.Empty
            else -> UiState.Content(list)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun addInboxItem(text: String, forWhom: ForWhom = ForWhom.BOTH, isPrivate: Boolean = false) {
        safeLaunch("addInboxItem") {
            inboxRepository.addInboxItem(text, forWhom, isPrivate)
        }
    }

    fun triageAsTask(item: InboxItem, title: String, nextAction: String, isTop3: Boolean) {
        safeLaunch("triageAsTask") {
            taskRepository.createTask(
                title = title,
                nextAction = nextAction,
                isTop3 = isTop3,
                forWhom = item.forWhom,
                isPrivate = item.isPrivate
            )
            inboxRepository.markInboxTriaged(item.id, "task", item.isPrivate)
        }
    }

    fun triageAsDream(item: InboxItem, title: String, list: String, status: String = "חלום") {
        safeLaunch("triageAsDream") {
            dreamRepository.createDream(
                Dream(
                    title = title,
                    list = list,
                    status = status,
                    forWhom = item.forWhom,
                    isPrivate = item.isPrivate
                )
            )
            inboxRepository.markInboxTriaged(item.id, "dream", item.isPrivate)
        }
    }

    fun triageAsNote(item: InboxItem) {
        safeLaunch("triageAsNote") {
            inboxRepository.markInboxTriaged(item.id, "note", item.isPrivate)
        }
    }

    fun deleteInboxItem(item: InboxItem) {
        safeLaunch("deleteInboxItem") {
            inboxRepository.deleteInboxItem(item.id, item.isPrivate)
        }
    }

    companion object {
        fun factory(
            inboxRepository: InboxRepository,
            taskRepository: TaskRepository,
            dreamRepository: DreamRepository,
            errorReporter: ErrorReporter
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InboxViewModel(inboxRepository, taskRepository, dreamRepository, errorReporter) as T
            }
        }
    }
}
