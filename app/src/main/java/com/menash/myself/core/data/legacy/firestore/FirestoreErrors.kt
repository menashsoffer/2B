package com.menash.myself.core.data.legacy.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import com.menash.myself.core.crash.CrashlyticsNonFatal
import com.menash.myself.core.ui.ErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow

internal object FirestoreErrors {

    fun report(
        tag: String,
        action: String,
        error: Throwable,
        errorFlow: MutableStateFlow<String?>,
        rememberTechnical: (String) -> Unit
    ) {
        val code = (error as? FirebaseFirestoreException)?.code?.name ?: "ERROR"
        val technical = "$action ($code): ${error.localizedMessage ?: error.message}"
        runCatching { Log.e(tag, technical, error) }
        rememberTechnical(technical)
        CrashlyticsNonFatal.record(error)
        errorFlow.value = ErrorMapper.toFriendlyMessage(error)
    }
}
