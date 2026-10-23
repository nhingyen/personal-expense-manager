package com.ngyennhi.expensemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ngyennhi.expensemanager.presentation.ExpenseManagerApp
import com.ngyennhi.expensemanager.presentation.dashboard.DashboardViewModel
import com.ngyennhi.expensemanager.presentation.transaction.TransactionViewModel
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val transactionViewModel: TransactionViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalExpenseManagerTheme {
                ExpenseManagerApp(transactionViewModel, dashboardViewModel)
            }
        }
    }
}
