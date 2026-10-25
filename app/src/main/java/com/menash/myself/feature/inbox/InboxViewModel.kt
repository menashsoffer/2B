package com.menash.myself.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.InboxItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(
    private val inboxRepository: InboxRepository,
    private val taskRepository: TaskRepository,
    private val dreamRepository: DreamRepository
) : ViewModel() {

    val inboxItems: StateFlow<List<InboxItem>> = inboxRepository.observeInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun triageToTask(item: InboxItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val taskTitle = if (item.text.startsWith("ל") || item.text.startsWith("ה")) {
                item.text
            } else {
                "לטפל ב-${item.text}"
            }
            taskRepository.createTask(
                title = taskTitle,
                nextAction = "צעד ראשון",
                forWhom = item.forWhom,
                isPrivate = item.isPrivate
            )
            inboxRepository.markInboxTriaged(item.id, "task", item.isPrivate)
            onDone()
        }
    }

    fun triageToDream(item: InboxItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val targetList = if (item.forWhom == ForWhom.ME) "פיתוח ואפליקציות" else "לבית"
            dreamRepository.createDream(
                Dream(
                    title = item.text,
                    list = targetList,
                    forWhom = item.forWhom,
                    isPrivate = item.isPrivate
                )
            )
            inboxRepository.markInboxTriaged(item.id, "dream", item.isPrivate)
            onDone()
        }
    }

    fun triageToIdea(item: InboxItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            dreamRepository.createDream(
                Dream(
                    title = item.text,
                    list = "פיתוח ואפליקציות",
                    forWhom = ForWhom.ME,
                    isPrivate = item.isPrivate
                )
            )
            inboxRepository.markInboxTriaged(item.id, "idea", item.isPrivate)
            onDone()
        }
    }

    fun triageToNote(item: InboxItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            inboxRepository.markInboxTriaged(item.id, "note", item.isPrivate)
            onDone()
        }
    }

    fun deleteItem(item: InboxItem) {
        viewModelScope.launch {
            inboxRepository.deleteInboxItem(item.id, item.isPrivate)
        }
    }

    companion object {
        fun factory(
            inboxRepository: InboxRepository,
            taskRepository: TaskRepository,
            dreamRepository: DreamRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InboxViewModel(inboxRepository, taskRepository, dreamRepository) as T
            }
        }
    }
}
