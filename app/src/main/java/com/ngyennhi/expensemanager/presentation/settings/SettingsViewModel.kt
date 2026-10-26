package com.ngyennhi.expensemanager.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngyennhi.expensemanager.domain.model.AppLanguage
import com.ngyennhi.expensemanager.domain.model.AppTheme
import com.ngyennhi.expensemanager.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val isLoading: Boolean = true,
    val isInitialized: Boolean = false,
    val isSaving: Boolean = false,
    val readError: Boolean = false,
    val writeError: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val uiState = mutableState.asStateFlow()
    private var observation: Job? = null

    init { retry() }

    fun retry() {
        observation?.cancel()
        mutableState.update { it.copy(isLoading = true, readError = false) }
        observation = viewModelScope.launch {
            try {
                refreshLanguage()
                repository.observeTheme().collect { theme ->
                    mutableState.update { it.copy(theme = theme, isLoading = false, isInitialized = true, readError = false) }
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(isLoading = false, isInitialized = true, readError = true) }
            }
        }
    }

    // Called after creation/resume/configuration changes, including external Android Settings.
    fun refreshLanguage() {
        mutableState.update { it.copy(language = repository.getLanguage()) }
    }

    fun setTheme(theme: AppTheme) {
        if (theme == uiState.value.theme && !uiState.value.writeError) return
        save { repository.setTheme(theme) }
    }

    fun setLanguage(language: AppLanguage) {
        if (language == uiState.value.language && !uiState.value.writeError) return
        save { repository.setLanguage(language); refreshLanguage() }
    }

    private fun save(write: suspend () -> Unit) {
        if (uiState.value.isLoading || uiState.value.readError || uiState.value.isSaving) return
        mutableState.update { it.copy(isSaving = true, writeError = false) }
        viewModelScope.launch {
            try { write()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(writeError = true) }
            } finally {
                mutableState.update { it.copy(isSaving = false) }
            }
        }
    }
}
