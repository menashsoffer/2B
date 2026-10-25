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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(
    private val inboxRepository: InboxRepository,
    private val taskRepository: TaskRepository,
    private val dreamRepository: DreamRepository,
    private val errorReporter: ErrorReporter? = null
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val inboxItems: StateFlow<List<InboxItem>> = inboxRepository.observeInbox()
        .catch { e ->
            handleException("observeInbox", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<UiState<List<InboxItem>>> = combine(inboxItems, _errorState) { list, error ->
        when {
            error != null && list.isEmpty() -> UiState.Error(error)
            list.isEmpty() -> UiState.Empty
            else -> UiState.Content(list)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun clearError() {
        _errorState.value = null
        errorReporter?.clearError()
    }

    private fun handleException(action: String, e: Throwable) {
        if (e is CancellationException) throw e
        val msg = e.localizedMessage ?: e.message ?: "שגיאה ב-$action"
        _errorState.value = msg
        errorReporter?.reportError(action, e)
    }

    private fun launchCatching(action: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException(action, e)
            }
        }
    }

    fun addInboxItem(text: String, forWhom: ForWhom = ForWhom.BOTH, isPrivate: Boolean = false) {
        launchCatching("addInboxItem") {
            inboxRepository.addInboxItem(text, forWhom, isPrivate)
        }
    }

    fun triageAsTask(item: InboxItem, title: String, nextAction: String, isTop3: Boolean) {
        launchCatching("triageAsTask") {
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
        launchCatching("triageAsDream") {
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
        launchCatching("triageAsNote") {
            inboxRepository.markInboxTriaged(item.id, "note", item.isPrivate)
        }
    }

    fun deleteInboxItem(item: InboxItem) {
        launchCatching("deleteInboxItem") {
            inboxRepository.deleteInboxItem(item.id, item.isPrivate)
        }
    }

    companion object {
        fun factory(
            inboxRepository: InboxRepository,
            taskRepository: TaskRepository,
            dreamRepository: DreamRepository,
            errorReporter: ErrorReporter? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InboxViewModel(inboxRepository, taskRepository, dreamRepository, errorReporter) as T
            }
        }
    }
}
