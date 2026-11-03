package com.menash.myself.e2e

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.menash.myself.MainActivity
import com.menash.myself.MyselfApp
import com.menash.myself.e2e.stories.Stories
import com.menash.myself.feature.money.importer.ImportPlan
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * e2e build only (docs/E2E.md). Opened by Maestro with
 * `myself-e2e://signin?email=<test user>&share=<asset under assets/e2e>`.
 *
 * Signs the test user in against the Auth emulator (creating it the first time), then opens
 * the app. With `share`, the bundled sample file is handed to the app through the same
 * ACTION_SEND path a real share from WhatsApp or Files takes.
 */
class E2eActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val link = intent?.data
        val email = link?.getQueryParameter("email")
        val share = link?.getQueryParameter("share")
        lifecycleScope.launch {
            val next = runCatching {
                signIn(email)
                link?.getQueryParameter("today")?.let { com.menash.myself.core.time.TodayProvider.setForE2e(LocalDate.parse(it)) }
                link?.getQueryParameter("seed")?.let { seed ->
                    seedStory(seed, com.menash.myself.core.time.TodayProvider.today())
                }
                Intent(this@E2eActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (share != null) {
                        val file = withContext(Dispatchers.IO) { copyAsset(share) }
                        action = Intent.ACTION_SEND
                        type = mimeOf(share)
                        putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this@E2eActivity, "$packageName.fileprovider", file))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
            }
            next.onSuccess { startActivity(it) }.onFailure {
                Log.e(TAG, "e2e sign-in or share failed", it)
                Toast.makeText(this@E2eActivity, "e2e: ${it.message}", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

    /** No email keeps whoever is signed in (or the default user when nobody is). */
    private suspend fun signIn(email: String?) {
        val auth = FirebaseAuth.getInstance()
        val current = auth.currentUser
        if (current != null && (email == null || current.email == email)) return
        if (current != null) auth.signOut()
        val user = email ?: DEFAULT_EMAIL
        try {
            auth.createUserWithEmailAndPassword(user, PASSWORD).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            auth.signInWithEmailAndPassword(user, PASSWORD).await()
        }
    }

    private suspend fun seedStory(name: String, today: LocalDate) {
        val container = (application as MyselfApp).appContainer
        container.householdRepository.ensureHouseholdExists()
        val existing = container.moneyRepository.observeForScreen().first { it.householdReady }
        if (existing.items.incomes.any { it.id == Stories.INCOME_ID }) return
        val story = Stories.build(name, today)
        val rows = story.rows
        container.transactionRepository.apply(ImportPlan(rows, emptyList(), rows.size, 0, 0, 0, 0, emptyList()))
        // Save the stable income marker last, after the rest of the story is durable.
        container.moneyRepository.saveAll(story.entries)
    }

    private fun copyAsset(name: String): File {
        require(name.matches(Regex("[A-Za-z0-9_.-]+"))) { "bad asset name: $name" }
        val dir = File(cacheDir, "e2e").apply { mkdirs() }
        val target = File(dir, name)
        assets.open("e2e/$name").use { input -> target.outputStream().use { input.copyTo(it) } }
        return target
    }

    private fun mimeOf(name: String) =
        if (name.endsWith(".xls")) "application/vnd.ms-excel"
        else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    private companion object {
        const val TAG = "E2eActivity"
        const val DEFAULT_EMAIL = "e2e@example.test"
        /** Auth emulator only. Never valid against the real project. */
        const val PASSWORD = "e2e-password-123"
    }
}
