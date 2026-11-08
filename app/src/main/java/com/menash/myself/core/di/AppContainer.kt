package com.menash.myself.core.di

import android.content.Context
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext
import com.menash.myself.MyselfApp
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.ExchangeRateRepository
import com.menash.myself.core.data.repository.FitnessRepository
import com.menash.myself.core.data.repository.HebcalRepository
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.core.data.repository.SettingsRepository
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.data.repository.impl.BoiExchangeRateRepository
import com.menash.myself.core.data.repository.impl.FirestoreDreamRepository
import com.menash.myself.core.data.repository.impl.FirestoreFitnessRepository
import com.menash.myself.core.data.repository.impl.FirestoreHouseholdRepository
import com.menash.myself.core.data.repository.impl.FirestoreInboxRepository
import com.menash.myself.core.data.repository.impl.FirestoreTaskRepository
import com.menash.myself.core.data.repository.impl.HebcalRepositoryImpl
import com.menash.myself.core.data.repository.impl.RoomSettingsRepository
import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.data.session.DefaultHouseholdSession
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.core.external.boi.BoiExchangeRateClient
import com.menash.myself.core.external.boi.BoiRateStore
import com.menash.myself.core.external.boi.boiDataStore
import com.menash.myself.core.external.hebcal.HebcalClient
import com.menash.myself.core.external.hebcal.HebcalStore
import com.menash.myself.core.external.hebcal.hebcalDataStore
import com.menash.myself.core.data.legacy.AppDatabase
import com.menash.myself.core.data.legacy.firestore.FirestoreManager
import com.menash.myself.core.data.legacy.firestore.HouseholdRepository
import com.menash.myself.core.data.legacy.firestore.LifeHubRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import com.menash.myself.core.data.repository.impl.FirestoreMoneyRepository
import com.menash.myself.core.data.repository.impl.FirestoreTransactionRepository
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.data.TransactionRepository
import com.menash.myself.feature.money.ImportInbox
import com.menash.myself.feature.money.usdRateOf

/**
 * CompositionLocal reading the singleton AppContainer from MyselfApp Application instance.
 */
val LocalAppContainer = compositionLocalWithComputedDefaultOf<AppContainer> {
    val appContext = LocalContext.currentValue.applicationContext
    (appContext as MyselfApp).appContainer
}

/**
 * Manual Dependency Injection Container owned by MyselfApp.
 * Features and ViewModels depend only on repository interfaces from core/data/repository.
 */
class AppContainer(private val context: Context) {
    val accountDeletionPending = com.menash.myself.core.data.account.AccountDeletionPending(context)

    fun accountDeletionPort() = com.menash.myself.core.data.account.FirebaseAccountDeletionPort(
        context.applicationContext, firestore, householdSession, accountDeletionPending,
    )

    val uiEvents = com.menash.myself.core.ui.UiEvents()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val appDatabase by lazy { AppDatabase.getDatabase(context) }
    private val appDao by lazy { appDatabase.appDao() }

    private val firestore by lazy { FirestoreManager.get(context) }
    private val rawLifeHubRepository by lazy { LifeHubRepository(firestore) }
    private val rawHouseholdRepository by lazy { HouseholdRepository(firestore) { accountDeletionPending.uid.value } }

    // Single source of truth for active household (survives Activity recreation)
    val householdSession: HouseholdSession by lazy { DefaultHouseholdSession() }

    // Shared error reporter for ViewModel action exceptions
    val errorReporter: ErrorReporter by lazy { DefaultErrorReporter() }

    // External data repositories
    val boiRateStore by lazy { BoiRateStore(context.boiDataStore) }
    val exchangeRateRepository: ExchangeRateRepository by lazy {
        BoiExchangeRateRepository(
            boiRateStore,
            BoiExchangeRateClient(),
            java.time.Clock.system(com.menash.myself.core.external.boi.BoiStalenessPolicy.ISRAEL_ZONE),
            applicationScope
        )
    }

    val hebcalStore by lazy { HebcalStore(context.hebcalDataStore) }
    val hebcalRepository: HebcalRepository by lazy {
        HebcalRepositoryImpl(hebcalStore, HebcalClient(), applicationScope)
    }

    // Room repository for local settings (offline data)
    val settingsRepository: SettingsRepository by lazy { RoomSettingsRepository(appDao) }

    // Firestore repositories for shared household items
    val taskRepository: TaskRepository by lazy { FirestoreTaskRepository(rawLifeHubRepository, householdSession) }
    val dreamRepository: DreamRepository by lazy { FirestoreDreamRepository(rawLifeHubRepository, householdSession) }
    val inboxRepository: InboxRepository by lazy { FirestoreInboxRepository(rawLifeHubRepository, householdSession) }
    val fitnessRepository: FitnessRepository by lazy { FirestoreFitnessRepository(rawLifeHubRepository) }
    val householdRepository: HouseholdRepositoryInterface by lazy { FirestoreHouseholdRepository(rawHouseholdRepository, householdSession) }

    // Money pillar: household-shared Firestore documents read by the money engine
    private val firestoreMoneyRepository by lazy { FirestoreMoneyRepository({ firestore }, householdSession) }
    val moneyRepository: MoneyRepository get() = firestoreMoneyRepository

    // Imported bank and card lines (households/{hid}/transactions) and the merchant memory
    private val firestoreTransactionRepository by lazy { FirestoreTransactionRepository({ firestore }, householdSession) }
    val transactionRepository: TransactionRepository get() = firestoreTransactionRepository

    /** Files shared into the app, waiting for the money tab's import. */
    val importInbox = ImportInbox()
    val monthTaskInbox = com.menash.myself.feature.money.MonthTaskInbox()

    /** The stored Bank of Israel USD rate for the money engine; null when none is stored. */
    val usdRateFlow: Flow<BigDecimal?> by lazy { exchangeRateRepository.currentRateFlow.map { usdRateOf(it) } }

    // Combined error flow for global banner
    val errorFlow: Flow<String?> by lazy {
        combine(
            errorReporter.errorState,
            rawLifeHubRepository.errorFlow,
            rawHouseholdRepository.errorFlow,
            firestoreMoneyRepository.errorFlow,
            firestoreTransactionRepository.errorFlow,
        ) { vmErr, lifeErr, hhErr, moneyErr, importErr ->
            vmErr ?: lifeErr ?: hhErr ?: moneyErr ?: importErr
        }
    }

    fun clearErrors() {
        errorReporter.clearError()
        rawLifeHubRepository.clearError()
        rawHouseholdRepository.clearError()
        firestoreMoneyRepository.clearError()
        firestoreTransactionRepository.clearError()
    }
}
