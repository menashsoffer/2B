package com.menash.myself.data.firestore

import android.content.Context
import com.menash.myself.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

object FirestoreManager {
    @Volatile
    private var instance: FirebaseFirestore? = null

    fun get(context: Context): FirebaseFirestore {
        return instance ?: synchronized(this) {
            val dbId = context.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            
            // Enable persistent offline cache so app functions without internet
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings

            instance = db
            db
        }
    }
}
