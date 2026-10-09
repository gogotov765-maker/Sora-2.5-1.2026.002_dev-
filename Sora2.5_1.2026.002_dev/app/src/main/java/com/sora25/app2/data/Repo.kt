package com.sora25.app2.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object Repo {
    private val db get() = FirebaseFirestore.getInstance()

    private fun com.google.firebase.firestore.DocumentSnapshot.toItem() =
        toObject(VideoItem::class.java)?.copy(id = id)

    /** Общая лента: только опубликованные видео (сортировка на клиенте, индексы не нужны). */
    fun feed(): Flow<List<VideoItem>> = callbackFlow {
        val reg = db.collection("videos")
            .whereEqualTo("published", true)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { it.toItem() }
                    ?.filter { it.videoUrl.isNotBlank() }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Мои видео: черновики и опубликованные. */
    fun mine(uid: String): Flow<List<VideoItem>> = callbackFlow {
        val reg = db.collection("videos")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { it.toItem() }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Сохраняет аккаунт на сервере: users/{uid} с почтой и/или телефоном. */
    suspend fun saveAccount(u: FirebaseUser) {
        val ref = db.collection("users").document(u.uid)
        val data = mapOf("email" to (u.email ?: ""), "phone" to (u.phoneNumber ?: ""))
        if (ref.get().await().exists()) ref.update(data).await()
        else ref.set(data + ("createdAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun setPublished(id: String, value: Boolean) {
        db.collection("videos").document(id).update("published", value).await()
    }

    suspend fun delete(id: String) {
        db.collection("videos").document(id).delete().await()
    }
}
