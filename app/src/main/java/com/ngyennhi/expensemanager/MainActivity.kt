package com.ngyennhi.expensemanager

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ngyennhi.expensemanager.presentation.ExpenseManagerApp
import com.ngyennhi.expensemanager.presentation.home.HomeViewModel
import com.ngyennhi.expensemanager.presentation.settings.SettingsViewModel
import com.ngyennhi.expensemanager.presentation.statistics.StatisticsViewModel
import com.ngyennhi.expensemanager.presentation.transaction.TransactionViewModel
import com.ngyennhi.expensemanager.presentation.category.CategoryViewModel
import com.ngyennhi.expensemanager.presentation.export.writeCsvDocument
import com.ngyennhi.expensemanager.presentation.export.TransactionCsvEncoder
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val transactionViewModel: TransactionViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val statisticsViewModel: StatisticsViewModel by viewModels()
    private val walletViewModel: com.ngyennhi.expensemanager.presentation.wallet.WalletViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val categoryViewModel: CategoryViewModel by viewModels()
    private val createCsvDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val document = settingsViewModel.currentExportDocument()
        if (uri == null) {
            settingsViewModel.exportCancelled()
        } else if (document == null) {
            lifecycleScope.launch {
                withContext(NonCancellable) {
                    val cleaned = withContext(Dispatchers.IO) { cleanupFailedDocument(uri) }
                    settingsViewModel.exportFailed(partialFileMayRemain = !cleaned)
                }
            }
        } else {
            settingsViewModel.exportDestinationSelected()
            lifecycleScope.launch {
                withContext(NonCancellable) {
                    val result = withContext(Dispatchers.IO) {
                        writeCsvDocument(
                            bytes = document.bytes,
                            openDestination = { contentResolver.openOutputStream(uri, "wt") },
                            cleanupDestination = { cleanupFailedDocument(uri) },
                        )
                    }
                    if (result.success) settingsViewModel.exportCompleted()
                    else settingsViewModel.exportFailed(result.partialFileMayRemain)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        enableEdgeToEdge()
        setContent {
            val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val dark = settings.theme.isDark(isSystemInDarkTheme())
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT) { dark },
                )
            }
            // Wait only for the initial read; later retries must retain the navigation host.
            if (settings.isInitialized) {
                PersonalExpenseManagerTheme(darkTheme = dark) {
                    ExpenseManagerApp(transactionViewModel, homeViewModel, statisticsViewModel, walletViewModel, categoryViewModel,
                        settings, settingsViewModel::setLanguage, settingsViewModel::setTheme,
                        settingsViewModel::retry, ::requestCsvExport)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.refreshLanguage()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        settingsViewModel.refreshLanguage()
    }

    private fun cleanupFailedDocument(uri: Uri): Boolean {
        return runCatching {
            contentResolver.openOutputStream(uri, "wt")?.use { } != null
        }.getOrDefault(false)
    }

    private fun requestCsvExport() {
        if (!settingsViewModel.beginCsvExport()) return
        val snapshot = walletViewModel.uiState.value
        lifecycleScope.launch {
            try {
                val document = withContext(Dispatchers.Default) {
                    TransactionCsvEncoder.encode(snapshot)
                }
                settingsViewModel.exportPrepared(document)
                createCsvDocument.launch("personal-expense-transactions-${LocalDate.now()}.csv")
            } catch (cancelled: CancellationException) {
                settingsViewModel.exportPreparationFailed()
                throw cancelled
            } catch (_: Exception) {
                settingsViewModel.exportPreparationFailed()
            }
        }
    }
}
