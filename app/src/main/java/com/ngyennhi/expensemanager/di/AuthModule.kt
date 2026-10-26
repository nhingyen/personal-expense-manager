package com.ngyennhi.expensemanager.di

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.ngyennhi.expensemanager.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.ngyennhi.expensemanager.data.repository.FirebaseAuthClient
import com.ngyennhi.expensemanager.data.repository.FirebaseAuthRepository
import com.ngyennhi.expensemanager.domain.repository.AuthRepository
import com.ngyennhi.expensemanager.domain.usecase.ValidateAuthCredentialsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides @Singleton
    fun repository(@ApplicationContext context: Context): AuthRepository = FirebaseAuthRepository(
        FirebaseAuthClient(FirebaseAuth.getInstance()),
        separateGoogleAccountsConfirmed = BuildConfig.GOOGLE_SEPARATE_ACCOUNTS_CONFIRMED,
        clearCredentialState = { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) },
    )

    @Provides
    fun validation() = ValidateAuthCredentialsUseCase()
}
