package com.ngyennhi.expensemanager.di

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.local.TransactionDao
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.usecase.AddTransactionUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveTransactionsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(TransactionDatabaseModule::class)
@RunWith(AndroidJUnit4::class)
class TransactionHiltTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var database: ExpenseManagerDatabase
    @Inject lateinit var add: AddTransactionUseCase
    @Inject lateinit var observe: ObserveTransactionsUseCase

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun hiltGraphProvidesUseCasesBackedByRoom() = runBlocking {
        val draft = TransactionDraft(TransactionType.INCOME, 10_000L, "Salary", LocalDate.of(2026, 10, 19), null)
        val id = add(draft)

        val record = withTimeout(5_000) { observe().first { records -> records.any { it.id == id } }.single() }
        assertEquals(draft.amountVnd, record.amountVnd)
        assertEquals(draft.type, record.type)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestDatabaseModule {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): ExpenseManagerDatabase =
            Room.inMemoryDatabaseBuilder(context, ExpenseManagerDatabase::class.java).addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET).build()

        @Provides
        fun provideDao(database: ExpenseManagerDatabase): TransactionDao = database.transactionDao()
    }
}
