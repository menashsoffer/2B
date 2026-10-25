package com.menash.myself.core.ui

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps technical exceptions to user-friendly Hebrew error messages.
 */
object ErrorMapper {

    const val MSG_OFFLINE = "אירעה שגיאת תקשורת. בדוק את החיבור לרשת ונסה שוב."
    const val MSG_PERMISSION_DENIED = "אין הרשאה לבצע פעולה זו."
    const val MSG_NO_HOUSEHOLD = "טרם הוגדר משק בית. יש ליצור או להצטרף למשק בית."
    const val MSG_UNKNOWN = "אירעה שגיאה בלתי צפויה. נסה שוב מאוחר יותר."

    fun toFriendlyMessage(throwable: Throwable?): String {
        if (throwable == null) return MSG_UNKNOWN

        // Check FirebaseFirestoreException code
        if (throwable is com.google.firebase.firestore.FirebaseFirestoreException) {
            when (throwable.code) {
                com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED -> return MSG_PERMISSION_DENIED
                com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE -> return MSG_OFFLINE
                else -> {}
            }
        }

        // Check network exceptions
        val isNetwork = throwable is UnknownHostException ||
                throwable is SocketTimeoutException ||
                throwable is java.net.ConnectException ||
                (throwable is IOException && !(throwable is java.io.FileNotFoundException))

        if (isNetwork) {
            val msg = (throwable.message ?: "").lowercase()
            if (msg.contains("unavailable") || msg.contains("offline") || msg.contains("network") || msg.contains("unable to resolve host") || throwable is UnknownHostException || throwable is SocketTimeoutException || throwable is java.net.ConnectException) {
                return MSG_OFFLINE
            }
        }

        val msg = (throwable.message ?: "").lowercase()
        val causeMsg = (throwable.cause?.message ?: "").lowercase()

        val isPermission = msg.contains("permission_denied") ||
                msg.contains("permission denied") ||
                causeMsg.contains("permission_denied")

        if (isPermission) return MSG_PERMISSION_DENIED

        val isNoHousehold = (throwable is IllegalStateException &&
                (msg.contains("household") || msg.contains("missing household"))) ||
                causeMsg.contains("household")

        if (isNoHousehold) return MSG_NO_HOUSEHOLD

        return MSG_UNKNOWN
    }
}
