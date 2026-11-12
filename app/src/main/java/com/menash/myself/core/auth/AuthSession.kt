package com.menash.myself.core.auth

import com.google.firebase.auth.FirebaseUser

/** Who is using the app. Null session means a guest. Tests pass this instead of a Firebase user. */
data class AuthSession(
    val uid: String,
    val displayName: String? = null,
    val email: String? = null,
)

fun FirebaseUser.toAuthSession(): AuthSession = AuthSession(
    uid = uid,
    displayName = displayName,
    email = email,
)
