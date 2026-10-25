package com.menash.myself.core.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.menash.myself.core.data.repository.AiSubscriptionRepository
import com.menash.myself.core.data.repository.CarRepository
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.FitnessRepository
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.core.data.repository.PensionFundRepository
import com.menash.myself.core.data.repository.SettingsRepository
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.data.repository.impl.FirestoreDreamRepository
import com.menash.myself.core.data.repository.impl.FirestoreFitnessRepository
import com.menash.myself.core.data.repository.impl.FirestoreHouseholdRepository
import com.menash.myself.core.data.repository.impl.FirestoreInboxRepository
import com.menash.myself.core.data.repository.impl.FirestoreTaskRepository
import com.menash.myself.core.data.repository.impl.RoomAiSubscriptionRepository
import com.menash.myself.core.data.repository.impl.RoomCarRepository
import com.menash.myself.core.data.repository.impl.RoomPensionFundRepository
import com.menash.myself.core.data.repository.impl.RoomSettingsRepository
import com.menash.myself.core.data.session.DefaultHouseholdSession
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.data.AppDatabase
import com.menash.myself.data.firestore.FirestoreManager
import com.menash.myself.data.firestore.HouseholdRepository
import com.menash.myself.data.firestore.LifeHubRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * CompositionLocal providing the singleton AppContainer across the Compose tree.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided")
}

/**
 * Manual Dependency Injection Container.
 * Features and ViewModels depend only on repository interfaces from core/data/repository.
 */
class AppContainer(private val context: Context) {

    private val appDatabase by lazy { AppDatabase.getDatabase(context) }
    private val appDao by lazy { appDatabase.appDao() }

    private val firestore by lazy { FirestoreManager.get(context) }
    private val rawLifeHubRepository by lazy { LifeHubRepository(firestore) }
    private val rawHouseholdRepository by lazy { HouseholdRepository(firestore) }

    // Single source of truth for active household
    val householdSession: HouseholdSession by lazy { DefaultHouseholdSession() }

    // Room repositories for calculators & settings (local offline data)
    val carRepository: CarRepository by lazy { RoomCarRepository(appDao) }
    val aiSubscriptionRepository: AiSubscriptionRepository by lazy { RoomAiSubscriptionRepository(appDao) }
    val pensionFundRepository: PensionFundRepository by lazy { RoomPensionFundRepository(appDao) }
    val settingsRepository: SettingsRepository by lazy { RoomSettingsRepository(appDao) }

    // Firestore repositories for shared household items
    val taskRepository: TaskRepository by lazy { FirestoreTaskRepository(rawLifeHubRepository, householdSession) }
    val dreamRepository: DreamRepository by lazy { FirestoreDreamRepository(rawLifeHubRepository, householdSession) }
    val inboxRepository: InboxRepository by lazy { FirestoreInboxRepository(rawLifeHubRepository, householdSession) }
    val fitnessRepository: FitnessRepository by lazy { FirestoreFitnessRepository(rawLifeHubRepository) }
    val householdRepository: HouseholdRepositoryInterface by lazy { FirestoreHouseholdRepository(rawHouseholdRepository, householdSession) }

    // Combined Firestore error flow for global banner
    val errorFlow: Flow<String?> by lazy {
        combine(rawLifeHubRepository.errorFlow, rawHouseholdRepository.errorFlow) { lifeErr, hhErr ->
            lifeErr ?: hhErr
        }
    }

    fun clearErrors() {
        rawLifeHubRepository.clearError()
        rawHouseholdRepository.clearError()
    }
}
