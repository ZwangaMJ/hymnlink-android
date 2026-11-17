package com.example.divineroster.data.repository

import com.example.divineroster.data.entity.WordOfTheDay
import com.example.divineroster.data.firestore.documentFlow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface WordOfTheDayRepository {
    /** Emits null until an admin has published a word. */
    fun observeWordOfTheDay(): Flow<WordOfTheDay?>
    suspend fun publish(word: WordOfTheDay)
}

@Singleton
class WordOfTheDayRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : WordOfTheDayRepository {

    private val document get() = firestore.collection("config").document("wordOfTheDay")

    override fun observeWordOfTheDay(): Flow<WordOfTheDay?> =
        document.documentFlow().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(WordOfTheDay::class.java) else null
        }

    override suspend fun publish(word: WordOfTheDay) {
        document.set(word).await()
    }
}
