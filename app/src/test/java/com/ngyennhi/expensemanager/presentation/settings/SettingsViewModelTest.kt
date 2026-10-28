package com.ngyennhi.expensemanager.presentation.settings

import androidx.lifecycle.ViewModelStore
import com.ngyennhi.expensemanager.domain.model.AppLanguage
import com.ngyennhi.expensemanager.domain.model.AppTheme
import com.ngyennhi.expensemanager.domain.repository.SettingsRepository
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
    private fun create() = SettingsViewModel(repository).also { store.put("${System.identityHashCode(it)}", it) }

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
