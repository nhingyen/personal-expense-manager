package com.ngyennhi.expensemanager.presentation.settings

import androidx.lifecycle.ViewModelStore
import com.ngyennhi.expensemanager.domain.model.AppLanguage
import com.ngyennhi.expensemanager.domain.model.AppTheme
import com.ngyennhi.expensemanager.domain.repository.SettingsRepository
import com.ngyennhi.expensemanager.presentation.export.TransactionCsvEncoder
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.wallet.WalletUiState
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeSettings()
    private val store = ViewModelStore()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun create() = SettingsViewModel(repository)
        .also { store.put("${System.identityHashCode(it)}", it) }

    @Test fun defaultsAndRestoredPreferencesAreObserved() = runTest(dispatcher) {
        repository.theme.value = AppTheme.DARK
        repository.selectedLanguage = AppLanguage.ENGLISH
        val vm = create()
        assertTrue(vm.uiState.value.isLoading)
        runCurrent()
        assertTrue(vm.uiState.value.isInitialized)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(AppTheme.DARK, vm.uiState.value.theme)
        assertEquals(AppLanguage.ENGLISH, vm.uiState.value.language)
        val restored = create()
        runCurrent()
        assertEquals(vm.uiState.value, restored.uiState.value)
    }

    @Test fun allLanguageAndThemeChoicesAreSavedAndExternalLanguageIsRefreshed() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        for (language in listOf(AppLanguage.VIETNAMESE, AppLanguage.ENGLISH, AppLanguage.SYSTEM)) {
            vm.setLanguage(language); runCurrent()
            assertEquals(language, repository.selectedLanguage)
            assertEquals(language, vm.uiState.value.language)
        }
        for (theme in listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM)) {
            vm.setTheme(theme); runCurrent()
            assertEquals(theme, repository.theme.value)
            assertEquals(theme, vm.uiState.value.theme)
        }
        repository.selectedLanguage = AppLanguage.ENGLISH
        vm.refreshLanguage()
        assertEquals(AppLanguage.ENGLISH, vm.uiState.value.language)
        val writes = repository.writes
        vm.setTheme(AppTheme.SYSTEM); vm.setLanguage(AppLanguage.ENGLISH); runCurrent()
        assertEquals(writes, repository.writes)
    }

    @Test fun failedWritesRetainSelectionAndAllowRetryWhilePendingWritesBlockDuplicates() = runTest(dispatcher) {
        val vm = create(); runCurrent()
        repository.failWrite = true
        vm.setLanguage(AppLanguage.ENGLISH); runCurrent()
        assertEquals(AppLanguage.SYSTEM, vm.uiState.value.language)
        assertTrue(vm.uiState.value.writeError)
        repository.failWrite = false
        repository.gate = CompletableDeferred()
        vm.setTheme(AppTheme.DARK)
        vm.setLanguage(AppLanguage.ENGLISH)
        runCurrent()
        assertTrue(vm.uiState.value.isSaving)
        assertEquals(AppLanguage.SYSTEM, vm.uiState.value.language)
        repository.gate!!.complete(Unit); runCurrent()
        assertFalse(vm.uiState.value.isSaving)
        assertFalse(vm.uiState.value.writeError)
        assertEquals(AppTheme.DARK, vm.uiState.value.theme)
        vm.setLanguage(AppLanguage.ENGLISH); runCurrent()
        assertEquals(AppLanguage.ENGLISH, vm.uiState.value.language)
    }

    @Test fun readFailureCanRetryAndDoesNotResetInitializedHost() = runTest(dispatcher) {
        repository.failRead = true
        val vm = create(); runCurrent()
        assertTrue(vm.uiState.value.readError)
        assertTrue(vm.uiState.value.isInitialized)
        vm.setTheme(AppTheme.DARK); runCurrent()
        assertEquals(0, repository.writes)
        repository.failRead = false
        vm.retry()
        assertTrue(vm.uiState.value.isInitialized)
        runCurrent()
        assertFalse(vm.uiState.value.readError)
        assertEquals(2, repository.observers)
    }

    @Test fun genuineEmptySnapshotCreatesHeaderOnlyExportAndCancellationReturnsToIdle() = runTest(dispatcher) {
        val vm = create(); runCurrent()
        assertTrue(vm.beginCsvExport())
        val document = TransactionCsvEncoder.encode(WalletUiState(status = LoadStatus.READY))
        assertEquals(0, document.transactionCount)
        vm.exportPrepared(document)
        assertEquals(CsvExportStatus.AWAITING_DESTINATION, vm.uiState.value.exportStatus)
        assertNotNull(vm.currentExportDocument())
        vm.exportCancelled()
        assertEquals(CsvExportStatus.IDLE, vm.uiState.value.exportStatus)
        assertNull(vm.currentExportDocument())
    }

    @Test fun pendingPreparationAndOpenPickerBlockDuplicatesAndPreparationFailureIsRetryable() = runTest(dispatcher) {
        val vm = create(); runCurrent()
        assertTrue(vm.beginCsvExport())
        assertFalse(vm.beginCsvExport())
        assertEquals(CsvExportStatus.PREPARING, vm.uiState.value.exportStatus)
        vm.exportPrepared(TransactionCsvEncoder.encode(WalletUiState(status = LoadStatus.READY)))
        assertFalse(vm.beginCsvExport())
        assertEquals(CsvExportStatus.AWAITING_DESTINATION, vm.uiState.value.exportStatus)
        vm.exportCancelled()

        assertTrue(vm.beginCsvExport())
        vm.exportPreparationFailed()
        assertEquals(CsvExportStatus.SOURCE_UNAVAILABLE, vm.uiState.value.exportStatus)
        assertTrue(vm.beginCsvExport())
    }

    @Test fun destinationAndWriteResultsHaveDistinctStates() = runTest(dispatcher) {
        val vm = create(); runCurrent()
        vm.exportCompleted()
        assertEquals(CsvExportStatus.IDLE, vm.uiState.value.exportStatus)

        vm.beginCsvExport()
        vm.exportPrepared(TransactionCsvEncoder.encode(WalletUiState(status = LoadStatus.READY)))
        vm.exportDestinationSelected()
        assertEquals(CsvExportStatus.WRITING, vm.uiState.value.exportStatus)
        vm.exportFailed(partialFileMayRemain = false)
        assertEquals(CsvExportStatus.WRITE_FAILED, vm.uiState.value.exportStatus)

        vm.beginCsvExport()
        vm.exportPrepared(TransactionCsvEncoder.encode(WalletUiState(status = LoadStatus.READY)))
        vm.exportDestinationSelected()
        vm.exportFailed(partialFileMayRemain = true)
        assertEquals(CsvExportStatus.PARTIAL_FILE_MAY_REMAIN, vm.uiState.value.exportStatus)

        vm.beginCsvExport()
        vm.exportPrepared(TransactionCsvEncoder.encode(WalletUiState(status = LoadStatus.READY)))
        vm.exportDestinationSelected()
        vm.exportCompleted()
        assertEquals(CsvExportStatus.SUCCESS, vm.uiState.value.exportStatus)
    }

    private class FakeSettings : SettingsRepository {
        val theme = MutableStateFlow(AppTheme.SYSTEM)
        var selectedLanguage = AppLanguage.SYSTEM
        var failRead = false
        var failWrite = false
        var writes = 0
        var observers = 0
        var gate: CompletableDeferred<Unit>? = null
        override fun observeTheme() = flow {
            observers++
            if (failRead) throw IOException()
            theme.collect { emit(it) }
        }
        override fun getLanguage() = selectedLanguage
        override suspend fun setLanguage(language: AppLanguage) {
            write(); this.selectedLanguage = language
        }
        override suspend fun setTheme(theme: AppTheme) {
            write(); this.theme.value = theme
        }
        private suspend fun write() {
            writes++
            gate?.await()
            if (failWrite) throw IOException()
        }
    }
}
