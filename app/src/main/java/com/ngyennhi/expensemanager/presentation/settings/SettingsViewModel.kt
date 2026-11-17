package com.ngyennhi.expensemanager.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngyennhi.expensemanager.domain.model.AppLanguage
import com.ngyennhi.expensemanager.domain.model.AppTheme
import com.ngyennhi.expensemanager.domain.repository.SettingsRepository
import com.ngyennhi.expensemanager.presentation.export.CsvExportDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CsvExportStatus {
    IDLE,
    PREPARING,
    AWAITING_DESTINATION,
    WRITING,
    SUCCESS,
    SOURCE_UNAVAILABLE,
    WRITE_FAILED,
    PARTIAL_FILE_MAY_REMAIN,
}

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val isLoading: Boolean = true,
    val isInitialized: Boolean = false,
    val isSaving: Boolean = false,
    val readError: Boolean = false,
    val writeError: Boolean = false,
    val exportStatus: CsvExportStatus = CsvExportStatus.IDLE,
) {
    val exportBusy: Boolean
        get() = exportStatus == CsvExportStatus.PREPARING ||
            exportStatus == CsvExportStatus.AWAITING_DESTINATION ||
            exportStatus == CsvExportStatus.WRITING

}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val uiState = mutableState.asStateFlow()
    private var observation: Job? = null
    private var pendingExport: CsvExportDocument? = null

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

    fun beginCsvExport(): Boolean {
        if (uiState.value.exportBusy) return false
        pendingExport = null
        mutableState.update { it.copy(exportStatus = CsvExportStatus.PREPARING) }
        return true
    }

    fun currentExportDocument(): CsvExportDocument? = pendingExport

    fun exportPrepared(document: CsvExportDocument) {
        if (uiState.value.exportStatus != CsvExportStatus.PREPARING) return
        pendingExport = document
        mutableState.update { it.copy(exportStatus = CsvExportStatus.AWAITING_DESTINATION) }
    }

    fun exportPreparationFailed() {
        pendingExport = null
        mutableState.update { it.copy(exportStatus = CsvExportStatus.SOURCE_UNAVAILABLE) }
    }

    fun exportDestinationSelected() {
        if (pendingExport != null && uiState.value.exportStatus == CsvExportStatus.AWAITING_DESTINATION) {
            mutableState.update { it.copy(exportStatus = CsvExportStatus.WRITING) }
        }
    }

    fun exportCancelled() {
        pendingExport = null
        if (uiState.value.exportStatus == CsvExportStatus.AWAITING_DESTINATION) {
            mutableState.update { it.copy(exportStatus = CsvExportStatus.IDLE) }
        }
    }

    fun exportCompleted() {
        if (uiState.value.exportStatus != CsvExportStatus.WRITING) return
        pendingExport = null
        mutableState.update { it.copy(exportStatus = CsvExportStatus.SUCCESS) }
    }

    fun exportFailed(partialFileMayRemain: Boolean) {
        pendingExport = null
        mutableState.update {
            it.copy(exportStatus = if (partialFileMayRemain) {
                CsvExportStatus.PARTIAL_FILE_MAY_REMAIN
            } else {
                CsvExportStatus.WRITE_FAILED
            })
        }
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
