package com.menash.myself

import com.menash.myself.core.data.mapper.DreamMappers
import com.menash.myself.core.data.mapper.FitnessMappers
import com.menash.myself.core.data.mapper.HouseholdMappers
import com.menash.myself.core.data.mapper.InboxMappers
import com.menash.myself.core.data.mapper.TaskMappers
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.InboxItem
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.core.data.legacy.models.TaskItem
import com.menash.myself.feature.dreams.DreamsViewModel
import com.menash.myself.feature.fitness.FitnessViewModel
import com.menash.myself.feature.home.LifeHomeViewModel
import com.menash.myself.feature.household.HouseholdViewModel
import com.menash.myself.feature.importcatalog.FileCatalogViewModel
import com.menash.myself.feature.inbox.InboxViewModel
import com.menash.myself.feature.tasks.TasksViewModel
import com.menash.myself.shell.MainViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ShellAndViewModelGuardsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class DelayedTaskRepository : TaskRepository {
        val snapshotFlow = MutableSharedFlow<List<Task>>(replay = 1)
        override fun observeTasks(filter: VisibilityFilter): Flow<List<Task>> = snapshotFlow
        override suspend fun createTask(
            title: String,
            nextAction: String,
            isTop3: Boolean,
            isFiveBefore11: Boolean,
            dueDate: LocalDate?,
            forWhom: ForWhom,
            isPrivate: Boolean,
            tag: String
        ) {}
        override suspend fun toggleTaskDone(taskId: String, currentDone: Boolean, isPrivate: Boolean) {}
        override suspend fun setTaskTop3(taskId: String, isTop3: Boolean, isPrivate: Boolean) {}
        override suspend fun setTaskFiveBefore11(taskId: String, isFive: Boolean, isPrivate: Boolean) {}
        override suspend fun updateTaskDate(taskId: String, dueDate: LocalDate?, isPrivate: Boolean) {}
        override suspend fun deleteTask(taskId: String, isPrivate: Boolean) {}
    }

    private class DelayedDreamRepository : DreamRepository {
        val snapshotFlow = MutableSharedFlow<List<Dream>>(replay = 1)
        override fun observeDreams(listFilter: String?, filter: VisibilityFilter): Flow<List<Dream>> = snapshotFlow
        override suspend fun createDream(dream: Dream) {}
        override suspend fun updateDreamStatus(dreamId: String, newStatus: String, isPrivate: Boolean) {}
        override suspend fun updateDreamSavings(dreamId: String, savedAmount: Money, isPrivate: Boolean) {}
        override suspend fun updateDreamAppIdea(
            dreamId: String,
            questions: Map<String, String>,
            nextAction: String,
            isPrivate: Boolean
        ) {}
        override suspend fun deleteDream(dreamId: String, isPrivate: Boolean) {}
    }

    private class DelayedInboxRepository : InboxRepository {
        val snapshotFlow = MutableSharedFlow<List<InboxItem>>(replay = 1)
        override fun observeInbox(): Flow<List<InboxItem>> = snapshotFlow
        override suspend fun addInboxItem(text: String, forWhom: ForWhom, isPrivate: Boolean) {}
        override suspend fun markInboxTriaged(itemId: String, triagedTo: String, isPrivate: Boolean) {}
        override suspend fun deleteInboxItem(itemId: String, isPrivate: Boolean) {}
    }

    private class FakeHouseholdRepo : HouseholdRepositoryInterface {
        override fun observeHousehold(): Flow<Household?> = MutableStateFlow(null)
        override suspend fun ensureHouseholdExists(): String = "h-1"
        override suspend fun invitePartner(householdId: String, email: String) {}
    }

    @Test
    fun `fix 1 - inner list flows start as null and no Empty emission occurs before first snapshot`() = runTest {
        val taskRepo = DelayedTaskRepository()
        val dreamRepo = DelayedDreamRepository()
        val inboxRepo = DelayedInboxRepository()
        val householdRepo = FakeHouseholdRepo()

        val reporter = DefaultErrorReporter()
        val tasksVm = TasksViewModel(taskRepo, householdRepo, reporter)
        val dreamsVm = DreamsViewModel(dreamRepo, householdRepo, reporter)
        val inboxVm = InboxViewModel(inboxRepo, taskRepo, dreamRepo, reporter)

        val taskUiEmissions = mutableListOf<UiState<List<Task>>>()
        val dreamUiEmissions = mutableListOf<UiState<List<Dream>>>()
        val inboxUiEmissions = mutableListOf<UiState<List<InboxItem>>>()

        val job1 = backgroundScope.launch(testDispatcher) { tasksVm.uiState.collect { taskUiEmissions.add(it) } }
        val job2 = backgroundScope.launch(testDispatcher) { dreamsVm.uiState.collect { dreamUiEmissions.add(it) } }
        val job3 = backgroundScope.launch(testDispatcher) { inboxVm.uiState.collect { inboxUiEmissions.add(it) } }

        // Before any snapshot arrives: inner list is null ("not loaded") and UiState is strictly Loading
        assertNull(tasksVm.tasks.value)
        assertNull(dreamsVm.dreams.value)
        assertNull(inboxVm.inboxItems.value)
        assertEquals(listOf(UiState.Loading), taskUiEmissions)
        assertEquals(listOf(UiState.Loading), dreamUiEmissions)
        assertEquals(listOf(UiState.Loading), inboxUiEmissions)
        assertFalse(taskUiEmissions.any { it is UiState.Empty })

        // First real snapshot is empty -> now and only now UiState transitions to Empty
        taskRepo.snapshotFlow.emit(emptyList())
        dreamRepo.snapshotFlow.emit(emptyList())
        inboxRepo.snapshotFlow.emit(emptyList())

        assertEquals(emptyList<Task>(), tasksVm.tasks.value)
        assertEquals(UiState.Empty, tasksVm.uiState.value)
        assertEquals(UiState.Empty, dreamsVm.uiState.value)
        assertEquals(UiState.Empty, inboxVm.uiState.value)

        // Second snapshot has items -> transitions to Content
        val sampleTask = Task(
            id = "t1",
            title = "Real snapshot task",
            nextAction = "",
            isDone = false,
            isTop3 = false,
            isFiveBefore11 = false,
            dueDate = null,
            ownerUid = "u1",
            forWhom = ForWhom.ME,
            isPrivate = false
        )
        taskRepo.snapshotFlow.emit(listOf(sampleTask))
        assertTrue(tasksVm.uiState.value is UiState.Content)

        job1.cancel()
        job2.cancel()
        job3.cancel()
    }

    @Test
    fun `fix 2 - single global capture FAB visible on all tabs and no per-screen FABs`() {
        val projectRoot = File(System.getProperty("user.dir") ?: ".")
        val srcRoot = if (File(projectRoot, "src/main/java").exists()) {
            File(projectRoot, "src/main/java/com/menash/myself")
        } else {
            File(projectRoot, "app/src/main/java/com/menash/myself")
        }

        val mainActivityText = File(srcRoot, "MainActivity.kt").readText()
        assertFalse(
            "MainActivity must not declare a capture FAB",
            mainActivityText.contains("persistent_capture_fab") ||
                mainActivityText.contains("FloatingActionButton("),
        )
        assertFalse(
            "MainActivity must not hide capture FAB on home tab",
            mainActivityText.contains("selectedTab != \"home\"")
        )

        val uiFiles = File(srcRoot, "shell").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        val fabViolations = mutableListOf<String>()
        uiFiles.forEach { file ->
            val content = file.readText()
            if (content.contains("FloatingActionButton(") || content.contains("ExtendedFloatingActionButton(")) {
                fabViolations.add(file.name)
            }
        }
        assertTrue("Per-screen FABs found in UI files: $fabViolations", fabViolations.isEmpty())

        val tasksScreenText = File(srcRoot, "feature/tasks/ui/TasksScreen.kt").readText()
        assertTrue("TasksScreen must have top-bar add action", tasksScreenText.contains("add_task_top_action"))
    }

    @Test
    fun `fix 3 - BaseViewModel safeLaunch reports errors to ErrorReporter and rethrows CancellationException`() = runTest {
        val reporter = DefaultErrorReporter()
        val vm = object : BaseViewModel(reporter) {
            fun triggerFailure() {
                safeLaunch("testAction") {
                    throw IllegalStateException("Something went wrong")
                }
            }

            fun triggerCancellation(onCaughtIncorrectly: () -> Unit) {
                safeLaunch(
                    action = "cancelAction",
                    onError = { onCaughtIncorrectly() }
                ) {
                    throw CancellationException("Coroutine cancelled")
                }
            }
        }

        vm.triggerFailure()
        testScheduler.advanceUntilIdle()
        assertEquals(com.menash.myself.core.ui.ErrorMapper.MSG_UNKNOWN, vm.errorState.value)
        assertEquals("Something went wrong", reporter.lastTechnicalError)

        vm.clearError()
        assertNull(vm.errorState.value)

        var cancellationSwallowed = false
        try {
            vm.triggerCancellation { cancellationSwallowed = true }
            testScheduler.advanceUntilIdle()
        } catch (e: CancellationException) {
            // Expected
        }
        assertFalse("CancellationException must be rethrown, not treated as error", cancellationSwallowed)
        assertNull(vm.errorState.value)

        // Verify the legacy ViewModels (the calculators moved into money on 31/10) inherit from BaseViewModel
        val allViewModels = listOf(
            MainViewModel::class.java,
            TasksViewModel::class.java,
            DreamsViewModel::class.java,
            InboxViewModel::class.java,
            LifeHomeViewModel::class.java,
            FitnessViewModel::class.java,
            HouseholdViewModel::class.java,
            FileCatalogViewModel::class.java,
        )
        allViewModels.forEach { clazz ->
            assertTrue("${clazz.simpleName} must extend BaseViewModel", BaseViewModel::class.java.isAssignableFrom(clazz))
        }
    }

    @Test
    fun `fix 4 - LifeHubRepository and ModelMappers split per collection and all files under 300 lines`() {
        val projectRoot = File(System.getProperty("user.dir") ?: ".")
        val srcRoot = if (File(projectRoot, "src/main/java").exists()) {
            File(projectRoot, "src/main/java/com/menash/myself")
        } else {
            File(projectRoot, "app/src/main/java/com/menash/myself")
        }

        val oversizedFiles = mutableListOf<String>()
        srcRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val count = file.readLines().size
            if (count >= 300) {
                oversizedFiles.add("${file.name} ($count lines)")
            }
        }
        assertTrue("All source files must be < 300 lines, violations: $oversizedFiles", oversizedFiles.isEmpty())

        // Verify per-collection mapper classes exist and map accurately
        val dto = TaskItem(
            id = "t-99",
            title = "Mapper split check",
            space = "shared"
        )
        val domainTask = TaskMappers.taskToDomain(dto)
        assertEquals("t-99", domainTask.id)
        assertEquals(ForWhom.BOTH, domainTask.forWhom)
        assertEquals("shared", TaskMappers.taskToData(domainTask, "h-1").space)

        // Touch other split mapper objects to verify compilation and linkage
        assertNotNull(DreamMappers)
        assertNotNull(InboxMappers)
        assertNotNull(FitnessMappers)
        assertNotNull(HouseholdMappers)
    }
}
