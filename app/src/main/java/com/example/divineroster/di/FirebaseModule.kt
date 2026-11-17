package com.example.divineroster.di

import com.example.divineroster.data.repository.AppointmentRepository
import com.example.divineroster.data.repository.AppointmentRepositoryImpl
import com.example.divineroster.data.repository.TaskRepository
import com.example.divineroster.data.repository.TaskRepositoryImpl
import com.example.divineroster.data.repository.UserRepository
import com.example.divineroster.data.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    fun provideTaskRepository(firestore: FirebaseFirestore): TaskRepository =
        TaskRepositoryImpl(firestore)
}
