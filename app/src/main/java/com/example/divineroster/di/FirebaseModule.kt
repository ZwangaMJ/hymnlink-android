package com.example.divineroster.di

import com.example.divineroster.data.repository.AnnouncementRepository
import com.example.divineroster.data.repository.AnnouncementRepositoryImpl
import com.example.divineroster.data.repository.AppointmentRepository
import com.example.divineroster.data.repository.AppointmentRepositoryImpl
import com.example.divineroster.data.repository.MusicRepository
import com.example.divineroster.data.repository.MusicRepositoryImpl
import com.example.divineroster.data.repository.UserRepository
import com.example.divineroster.data.repository.UserRepositoryImpl
import com.example.divineroster.data.repository.WordOfTheDayRepository
import com.example.divineroster.data.repository.WordOfTheDayRepositoryImpl
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        val options = FirebaseApp.getInstance().options
        // google-services.json only carries a storage bucket once Cloud Storage has been enabled for the project;
        // fall back to the default bucket name so the app still works with an older config file.
        return if (options.storageBucket.isNullOrBlank()) {
            FirebaseStorage.getInstance("gs://${options.projectId}.firebasestorage.app")
        } else {
            FirebaseStorage.getInstance()
        }
    }

    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore): UserRepository =
        UserRepositoryImpl(firestore)

    @Provides
    @Singleton
    fun provideAppointmentRepository(
        firestore: FirebaseFirestore,
        userRepository: UserRepository
    ): AppointmentRepository = AppointmentRepositoryImpl(firestore, userRepository)

    @Provides
    @Singleton
    fun provideAnnouncementRepository(firestore: FirebaseFirestore): AnnouncementRepository =
        AnnouncementRepositoryImpl(firestore)

    @Provides
    @Singleton
    fun provideWordOfTheDayRepository(firestore: FirebaseFirestore): WordOfTheDayRepository =
        WordOfTheDayRepositoryImpl(firestore)

    @Provides
    @Singleton
    fun provideMusicRepository(firestore: FirebaseFirestore, storage: FirebaseStorage): MusicRepository =
        MusicRepositoryImpl(firestore, storage)
}
