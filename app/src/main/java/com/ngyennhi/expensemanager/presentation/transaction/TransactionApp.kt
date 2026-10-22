package com.ngyennhi.expensemanager.presentation.transaction

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private const val LIST_ROUTE = "transactions"
private const val ADD_ROUTE = "add"
private const val DETAIL_ROUTE = "detail/{id}"
private const val EDIT_ROUTE = "edit/{id}"

@Composable
fun TransactionApp(viewModel: TransactionViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(viewModel, navController) {
        viewModel.navigationEvents.collect {
            navController.popBackStack(LIST_ROUTE, inclusive = false)
        }
    }

    val returnToList: () -> Unit = {
        navController.popBackStack(LIST_ROUTE, inclusive = false)
        Unit
    }

    NavHost(navController, startDestination = LIST_ROUTE, modifier = Modifier.fillMaxSize()) {
        composable(LIST_ROUTE) {
            TransactionListScreen(
                state = state,
                onQueryChange = viewModel::setQuery,
                onFilterChange = viewModel::setFilter,
                onMonthChange = viewModel::changeMonth,
                onResetFilters = viewModel::resetFilters,
                onAdd = {
                    viewModel.startAdd()
                    navController.navigate(ADD_ROUTE)
                },
                onOpenRecord = { id ->
                    viewModel.selectRecord(id)
                    navController.navigate("detail/$id")
                },
                onRetry = viewModel::retryObservation,
            )
        }
        composable(ADD_ROUTE) {
            TransactionFormScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onReturnToList = returnToList,
                onRetry = viewModel::retryObservation,
                onTypeChange = viewModel::setType,
                onAmountChange = viewModel::setAmount,
                onCategoryChange = viewModel::setCategory,
                onDateChange = viewModel::setDate,
                onNoteChange = viewModel::setNote,
                onSave = viewModel::save,
            )
        }
        composable(DETAIL_ROUTE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = requireNotNull(entry.arguments?.getLong("id"))
            LaunchedEffect(id) { viewModel.selectRecord(id) }
            TransactionDetailScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onReturnToList = returnToList,
                onEdit = {
                    viewModel.startEdit(id)
                    navController.navigate("edit/$id")
                },
                onDelete = viewModel::deleteSelected,
                onRetry = viewModel::retryObservation,
            )
        }
        composable(EDIT_ROUTE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = requireNotNull(entry.arguments?.getLong("id"))
            LaunchedEffect(id) {
                if (viewModel.uiState.value.form.editingId != id) viewModel.startEdit(id)
            }
            TransactionFormScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onReturnToList = returnToList,
                onRetry = viewModel::retryObservation,
                onTypeChange = viewModel::setType,
                onAmountChange = viewModel::setAmount,
                onCategoryChange = viewModel::setCategory,
                onDateChange = viewModel::setDate,
                onNoteChange = viewModel::setNote,
                onSave = viewModel::save,
            )
        }
    }
}
