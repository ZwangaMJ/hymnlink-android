package com.example.divineroster.data.firestore

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "FirestoreFlow"

fun Query.snapshotFlow(): Flow<com.google.firebase.firestore.QuerySnapshot> = callbackFlow {
    val query = this@snapshotFlow
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.e(TAG, "Query listener error on $query", error)
            close(error)
        } else if (snapshot != null) {
            trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}

fun DocumentReference.documentFlow(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.e(TAG, "Document listener error on ${this@documentFlow.path}", error)
            close(error)
        } else if (snapshot != null) {
            trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}
