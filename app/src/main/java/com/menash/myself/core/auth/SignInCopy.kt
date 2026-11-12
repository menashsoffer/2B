package com.menash.myself.core.auth

/**
 * Every visible string on the sign-in screen.
 * The route and the return path do not read these. A later copy change replaces the strings only.
 */
data class SignInCopy(
    val wordmark: String,
    val body: String,
    val spacesTitle: String,
    val spacesSubtitle: String,
    val syncTitle: String,
    val syncSubtitle: String,
    val privacyTitle: String,
    val privacySubtitle: String,
    val button: String,
    val close: String,
)
