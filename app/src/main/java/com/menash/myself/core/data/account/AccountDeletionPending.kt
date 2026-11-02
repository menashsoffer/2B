package com.menash.myself.core.data.account

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Do not create a replacement household during account deletion, including retries after process death. */
class AccountDeletionPending(context: Context) {
    private val prefs = context.getSharedPreferences("account_deletion", Context.MODE_PRIVATE)
    private val mutableUid = MutableStateFlow(prefs.getString("uid", null))
    val uid = mutableUid.asStateFlow()
    fun set(uid: String, pending: Boolean) {
        check(prefs.edit().putString("uid", if (pending) uid else null).commit())
        mutableUid.value = if (pending) uid else null
    }
}
