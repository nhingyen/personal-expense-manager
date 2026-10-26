package com.ngyennhi.expensemanager.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

@Composable
internal fun NavigationPlaceholderScreen(destination: AppDestination) {
    Scaffold(
        containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(stringResource(destination.label)) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(24.dp).testTag("${destination.route}_placeholder"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.navigation_coming_soon, stringResource(destination.label)),
                style = MaterialTheme.typography.bodyMedium, color = ExpenseManagerColors.textSecondary,
                textAlign = TextAlign.Center)
        }
    }
}
