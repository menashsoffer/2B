package com.menash.myself.core.auth

/**
 * Every visible string on the sign-in screen. No feature promises (Menash 12/11): the name,
 * the Google button and «סגירה». The route and the return path do not read these.
 */
data class SignInCopy(
    val wordmark: String,
    val button: String,
    val close: String,
)
