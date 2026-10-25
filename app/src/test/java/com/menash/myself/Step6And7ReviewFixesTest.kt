package com.menash.myself

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.menash.myself.core.data.model.ItemLocation
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.FitnessRepository
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.data.session.DefaultHouseholdSession
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.InboxItem
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.feature.dreams.DreamsViewModel
import com.menash.myself.feature.home.LifeHomeViewModel
import com.menash.myself.feature.tasks.TasksViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = MyselfApp::class)
class Step6And7ReviewFixesTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class NoHouseholdTaskRepository(
        private val session: HouseholdSession
    ) : TaskRepository {
        override fun observeTasks(filter: VisibilityFilter): Flow<List<Task>> = MutableStateFlow(emptyList())

        override suspend fun createTask(
            title: String,
            nextAction: String,
            isTop3: Boolean,
            isFiveBefore11: Boolean,
            dueDate: LocalDate?,
            forWhom: ForWhom,
            isPrivate: Boolean,
            tag: String
        ) {
            if (!isPrivate) {
                session.requireHouseholdId()
            }
        }

        override suspend fun toggleTaskDone(taskId: String, currentDone: Boolean, isPrivate: Boolean) {
            if (!isPrivate) session.requireHouseholdId()
        }

        override suspend fun setTaskTop3(taskId: String, isTop3: Boolean, isPrivate: Boolean) {
            if (!isPrivate) session.requireHouseholdId()
        }

        override suspend fun setTaskFiveBefore11(taskId: String, isFive: Boolean, isPrivate: Boolean) {
            if (!isPrivate) session.requireHouseholdId()
        }

        override suspend fun updateTaskDate(taskId: String, dueDate: LocalDate?, isPrivate: Boolean) {
            if (!isPrivate) session.requireHouseholdId()
        }

        override suspend fun deleteTask(taskId: String, isPrivate: Boolean) {
            if (!isPrivate) session.requireHouseholdId()
        }
    }

    private class FakeHouseholdRepository : HouseholdRepositoryInterface {
        override fun observeHousehold(): Flow<Household?> = MutableStateFlow(null)
        override suspend fun ensureHouseholdExists(): String = "h-1"
        override suspend fun invitePartner(householdId: String, email: String) {}
    }

    private class FakeDreamRepository : DreamRepository {
        override fun observeDreams(listFilter: String?, filter: VisibilityFilter): Flow<List<Dream>> =
            MutableStateFlow(emptyList())
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

    private class FakeInboxRepository : InboxRepository {
        override fun observeInbox(): Flow<List<InboxItem>> = MutableStateFlow(emptyList())
        override suspend fun addInboxItem(text: String, forWhom: ForWhom, isPrivate: Boolean) {}
        override suspend fun markInboxTriaged(itemId: String, triagedTo: String, isPrivate: Boolean) {}
        override suspend fun deleteInboxItem(itemId: String, isPrivate: Boolean) {}
    }

    private class FakeFitnessRepository : FitnessRepository {
        override fun observeTodayFitness(): Flow<FitnessEntry?> = MutableStateFlow(null)
        override fun observeWeeklyFitness(): Flow<List<FitnessEntry>> = MutableStateFlow(emptyList())
        override suspend fun saveTodayFitness(moved: Boolean, minutes: Int, type: String, date: LocalDate) {}
    }

    @Test
    fun `fix 1 - createTask with no household populates error state and does not crash`() = runTest {
        val emptyHouseholdSession = DefaultHouseholdSession()
        val errorReporter = DefaultErrorReporter()
        val viewModel = TasksViewModel(
            taskRepository = NoHouseholdTaskRepository(emptyHouseholdSession),
            householdRepository = FakeHouseholdRepository(),
            errorReporter = errorReporter
        )

        viewModel.createTask(title = "Buy milk", forWhom = ForWhom.BOTH, isPrivate = false)

        val err = viewModel.errorState.value
        assertNotNull("Expected errorState to be populated instead of crashing", err)
        assertTrue(err!!.contains("Active household session required"))
        assertTrue(errorReporter.errorState.value?.contains(err) == true)
    }

    @Test
    fun `fix 2 - MyselfApp Application owns AppContainer and HouseholdSession survives recreation`() {
        val app = ApplicationProvider.getApplicationContext<Application>() as MyselfApp
        val container1 = app.appContainer
        container1.householdSession.setHouseholdId("household-survives-rotation")

        // Simulate Activity recreation reading from Application again
        val appAfterRecreation = ApplicationProvider.getApplicationContext<Application>() as MyselfApp
        val container2 = appAfterRecreation.appContainer

        assertSame(container1, container2)
        assertEquals("household-survives-rotation", container2.householdSession.requireHouseholdId())
    }

    @Test
    fun `fix 3 - default VisibilityFilter is MINE_AND_BOTH for tasks dreams and home`() {
        val emptySession = DefaultHouseholdSession("h-1")
        val taskRepo = NoHouseholdTaskRepository(emptySession)
        val householdRepo = FakeHouseholdRepository()
        val dreamRepo = FakeDreamRepository()
        val inboxRepo = FakeInboxRepository()
        val fitnessRepo = FakeFitnessRepository()

        val tasksVm = TasksViewModel(taskRepo, householdRepo)
        val dreamsVm = DreamsViewModel(dreamRepo, householdRepo)
        val homeVm = LifeHomeViewModel(taskRepo, dreamRepo, inboxRepo, fitnessRepo, householdRepo)

        assertEquals(VisibilityFilter.MINE_AND_BOTH, tasksVm.visibilityFilter.value)
        assertEquals(VisibilityFilter.MINE_AND_BOTH, dreamsVm.visibilityFilter.value)
        assertEquals(VisibilityFilter.MINE_AND_BOTH, homeVm.visibilityFilter.value)
    }

    @Test
    fun `fix 4 and step 7 - zero data-layer or ModelMappers imports in feature and ui and files under 300 lines`() {
        val projectRoot = File(System.getProperty("user.dir") ?: ".")
        val srcRoot = if (File(projectRoot, "src/main/java").exists()) {
            File(projectRoot, "src/main/java/com/menash/myself")
        } else {
            File(projectRoot, "app/src/main/java/com/menash/myself")
        }

        val targetDirs = listOf(File(srcRoot, "feature"), File(srcRoot, "ui"))
        val violations = mutableListOf<String>()

        targetDirs.forEach { dir ->
            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { ktFile ->
                val lines = ktFile.readLines()
                if (lines.size >= 300) {
                    violations.add("${ktFile.name} has ${lines.size} lines (must be < 300)")
                }
                lines.forEachIndexed { idx, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("import com.menash.myself.data.") ||
                        trimmed.contains("ModelMappers")
                    ) {
                        violations.add("${ktFile.name}:${idx + 1} forbidden reference: $trimmed")
                    }
                }
            }
        }

        assertTrue("Architectural violations found:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    @Test
    fun `fix 5 - ItemLocation resolves typed Household vs Private without sentinel string`() {
        val noHouseholdSession = DefaultHouseholdSession(null)
        val privateLoc = ItemLocation.resolve(
            isPrivate = true,
            householdSession = noHouseholdSession,
            uidProvider = { "user-123" }
        )
        assertEquals(ItemLocation.Private("user-123"), privateLoc)

        val activeHouseholdSession = DefaultHouseholdSession("household-456")
        val householdLoc = ItemLocation.resolve(
            isPrivate = false,
            householdSession = activeHouseholdSession,
            uidProvider = { "user-123" }
        )
        assertEquals(ItemLocation.Household("household-456"), householdLoc)
    }
}
