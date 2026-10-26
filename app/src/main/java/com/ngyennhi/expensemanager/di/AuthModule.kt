package com.ngyennhi.expensemanager.di

import com.google.firebase.auth.FirebaseAuth
import com.ngyennhi.expensemanager.data.repository.FirebaseAuthClient
import com.ngyennhi.expensemanager.data.repository.FirebaseAuthRepository
import com.ngyennhi.expensemanager.domain.repository.AuthRepository
import com.ngyennhi.expensemanager.domain.usecase.ValidateAuthCredentialsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides @Singleton
    fun repository(): AuthRepository = FirebaseAuthRepository(FirebaseAuthClient(FirebaseAuth.getInstance()))

    @Provides
    fun validation() = ValidateAuthCredentialsUseCase()
}
