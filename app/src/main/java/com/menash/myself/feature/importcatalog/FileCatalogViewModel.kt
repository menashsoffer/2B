package com.menash.myself.feature.importcatalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Money

class FileCatalogViewModel(
    private val taskRepository: TaskRepository,
    private val dreamRepository: DreamRepository
) : ViewModel() {

    suspend fun importTasks(
        dataRows: List<List<String>>,
        mapping: Map<String, Int>,
        spaceId: String
    ): Int {
        val (forWhom, isPrivate) = ModelMappers.legacySpaceToForWhom(spaceId)
        val titleIdx = mapping["שם המשימה"] ?: -1
        val nextIdx = mapping["צעד ראשון מיידי (Next Action)"] ?: -1
        val dateIdx = mapping["תאריך יעד"] ?: -1
        var count = 0
        dataRows.forEach { row ->
            val t = row.getOrNull(titleIdx)?.trim() ?: ""
            if (t.isNotBlank()) {
                val parsedDate = ModelMappers.parseLocalDate(row.getOrNull(dateIdx)?.trim())
                taskRepository.createTask(
                    title = t,
                    nextAction = row.getOrNull(nextIdx)?.trim() ?: "",
                    dueDate = parsedDate,
                    forWhom = forWhom,
                    isPrivate = isPrivate
                )
                count++
            }
        }
        return count
    }

    suspend fun importDreams(
        dataRows: List<List<String>>,
        mapping: Map<String, Int>,
        spaceId: String
    ): Int {
        val (forWhom, isPrivate) = ModelMappers.legacySpaceToForWhom(spaceId)
        val titleIdx = mapping["כותרת/שם החלום"] ?: -1
        val listIdx = mapping["רשימה"] ?: -1
        val costIdx = mapping["עלות משוערת"] ?: -1
        var count = 0
        dataRows.forEach { row ->
            val t = row.getOrNull(titleIdx)?.trim() ?: ""
            if (t.isNotBlank()) {
                val cost = row.getOrNull(costIdx)?.toDoubleOrNull() ?: 0.0
                dreamRepository.createDream(
                    Dream(
                        title = t,
                        list = row.getOrNull(listIdx)?.trim() ?: "לבית",
                        estimatedCost = Money.fromShekels(cost),
                        forWhom = forWhom,
                        isPrivate = isPrivate
                    )
                )
                count++
            }
        }
        return count
    }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            dreamRepository: DreamRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FileCatalogViewModel(taskRepository, dreamRepository) as T
            }
        }
    }
}
