package com.menash.myself.core.data.account

/** Only server-confirmed absence reaches this policy (the listener ignores empty cache snapshots). */
fun shouldRecoverHousehold(householdId: String?, pendingDeletionUid: String?, signedInUid: String?): Boolean =
    signedInUid != null && householdId == null && pendingDeletionUid != signedInUid
