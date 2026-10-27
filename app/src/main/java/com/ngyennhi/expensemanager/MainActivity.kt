package com.ngyennhi.expensemanager

import android.content.res.Configuration
import android.os.Bundle
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
import com.ngyennhi.expensemanager.presentation.ExpenseManagerApp
import com.ngyennhi.expensemanager.presentation.home.HomeViewModel
import com.ngyennhi.expensemanager.presentation.settings.SettingsViewModel
import com.ngyennhi.expensemanager.presentation.statistics.StatisticsViewModel
import com.ngyennhi.expensemanager.presentation.transaction.TransactionViewModel
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val transactionViewModel: TransactionViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val statisticsViewModel: StatisticsViewModel by viewModels()
    private val walletViewModel: com.ngyennhi.expensemanager.presentation.wallet.WalletViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
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
                    ExpenseManagerApp(transactionViewModel, homeViewModel, statisticsViewModel, walletViewModel,
                        settings, settingsViewModel::setLanguage, settingsViewModel::setTheme, settingsViewModel::retry)
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
}
