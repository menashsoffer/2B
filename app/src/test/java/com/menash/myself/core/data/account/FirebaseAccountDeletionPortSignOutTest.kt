package com.menash.myself.core.data.account

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.menash.myself.core.data.session.DefaultHouseholdSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseAccountDeletionPortSignOutTest {
    @Test fun signOutSignsOutOfFirebaseFirst() {
        val user: FirebaseUser = mock {
            on { uid } doReturn "u-1"
            on { email } doReturn "a@b.c"
        }
        val auth: FirebaseAuth = mock { on { currentUser } doReturn user }
        val db: FirebaseFirestore = mock()
        val session = DefaultHouseholdSession("h-1")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val port = FirebaseAccountDeletionPort(context, db, session, AccountDeletionPending(context), auth)

        val job = CoroutineScope(Dispatchers.Unconfined).launch { port.signOut() }
        try {
            verify(auth).signOut()
        } finally {
            job.cancel()
        }
    }
}
