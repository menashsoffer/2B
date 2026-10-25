package com.menash.myself.feature.importcatalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.SpaceSelection
import com.menash.myself.core.domain.util.DateInputParser
import com.menash.myself.core.ui.BaseViewModel

class FileCatalogViewModel(
    private val taskRepository: TaskRepository,
    private val dreamRepository: DreamRepository,
    errorReporter: ErrorReporter? = null
) : BaseViewModel(errorReporter) {

    fun importCsvRows(
        selectedTargetType: String,
        importedRows: List<List<String>>,
        mapping: Map<String, Int>,
        onComplete: (Int) -> Unit
    ) {
        var count = 0
        safeLaunch(
            action = "importCsvRows",
            onError = { onComplete(count) }
        ) {
            val dataRows = importedRows.drop(1)
            if (selectedTargetType == "tasks") {
                for (row in dataRows) {
                    val title = row.getOrNull(mapping["כותרת משימה"] ?: -1)?.trim() ?: ""
                    if (title.isNotEmpty()) {
                        val nextAction = row.getOrNull(mapping["צעד הבא"] ?: -1) ?: ""
                        val dueDateStr = row.getOrNull(mapping["תאריך יעד"] ?: -1) ?: ""
                        val spaceRaw = row.getOrNull(mapping["מרחב (שלי/של אשתי/משותף)"] ?: -1) ?: "משותף"
                        val selection = SpaceSelection.parseFromCsv(spaceRaw)
                        val dueDate = DateInputParser.parseOrNull(dueDateStr)
                        taskRepository.createTask(
                            title = title,
                            nextAction = nextAction,
                            dueDate = dueDate,
                            forWhom = selection.forWhom,
                            isPrivate = selection.isPrivate
                        )
                        count++
                    }
                }
            } else {
                for (row in dataRows) {
                    val title = row.getOrNull(mapping["כותרת חלום"] ?: -1)?.trim() ?: ""
                    if (title.isNotEmpty()) {
                        val list = row.getOrNull(mapping["רשימה"] ?: -1) ?: "לבית"
                        val cost = row.getOrNull(mapping["עלות משוערת"] ?: -1)?.toDoubleOrNull() ?: 0.0
                        val saved = row.getOrNull(mapping["סכום שנחסך"] ?: -1)?.toDoubleOrNull() ?: 0.0
                        val nextAction = row.getOrNull(mapping["צעד הבא"] ?: -1) ?: ""
                        val spaceRaw = row.getOrNull(mapping["מרחב"] ?: -1) ?: "משותף"
                        val selection = SpaceSelection.parseFromCsv(spaceRaw)
                        dreamRepository.createDream(
                            Dream(
                                title = title,
                                list = list,
                                estimatedCost = Money.fromShekels(cost),
                                savedAmount = Money.fromShekels(saved),
                                nextAction = nextAction,
                                forWhom = selection.forWhom,
                                isPrivate = selection.isPrivate
                            )
                        )
                        count++
                    }
                }
            }
            onComplete(count)
        }
    }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            dreamRepository: DreamRepository,
            errorReporter: ErrorReporter? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FileCatalogViewModel(taskRepository, dreamRepository, errorReporter) as T
            }
        }
    }
}
