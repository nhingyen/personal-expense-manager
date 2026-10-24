package com.ngyennhi.expensemanager.presentation

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
import com.ngyennhi.expensemanager.presentation.home.HomeScreen
import com.ngyennhi.expensemanager.presentation.home.HomeViewModel
import com.ngyennhi.expensemanager.presentation.home.HomeActions
import com.ngyennhi.expensemanager.presentation.transaction.TransactionViewModel
import com.ngyennhi.expensemanager.presentation.transaction.TransactionListScreen
import com.ngyennhi.expensemanager.presentation.transaction.TransactionFormScreen
import com.ngyennhi.expensemanager.presentation.transaction.TransactionDetailScreen

private const val HOME_ROUTE = "home"
private const val LIST_ROUTE = "transactions"
private const val ADD_ROUTE = "add"
private const val DETAIL_ROUTE = "detail/{id}"
private const val EDIT_ROUTE = "edit/{id}"

@Composable
fun ExpenseManagerApp(viewModel: TransactionViewModel, homeViewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // Home is always the root. Replace secondary routes and any existing list entry,
    // including when a write was started directly from Home.
    val returnToList: () -> Unit = {
        navController.navigate(LIST_ROUTE) {
            popUpTo(HOME_ROUTE)
            launchSingleTop = true
        }
    }
    val openAdd: () -> Unit = {
        viewModel.startAdd()
        navController.navigate(ADD_ROUTE) { launchSingleTop = true }
    }
    val openRecord: (Long) -> Unit = { id ->
        viewModel.selectRecord(id)
        navController.navigate("detail/$id")
    }
    LaunchedEffect(viewModel, navController) {
        viewModel.navigationEvents.collect {
            returnToList()
        }
    }

    NavHost(navController, startDestination = HOME_ROUTE, modifier = Modifier.fillMaxSize()) {
        composable(HOME_ROUTE) {
            LaunchedEffect(Unit) { homeViewModel.refreshGreeting() }
            HomeScreen(
                state = homeState,
                onPreviousMonth = { homeViewModel.changeMonth(-1) },
                onNextMonth = { homeViewModel.changeMonth(1) },
                onAdd = openAdd,
                onViewAll = {
                    viewModel.showAllForMonth(homeState.selectedMonth)
                    returnToList()
                },
                onOpenRecord = openRecord,
                onRetry = homeViewModel::retryObservation,
                homeActions = HomeActions(
                    onEditBudget = homeViewModel::startBudgetEdit,
                    onBudgetInput = homeViewModel::setBudgetInput,
                    onSaveBudget = homeViewModel::saveMonthlyBudget,
                    onClearBudget = homeViewModel::clearMonthlyBudget,
                    onRetryBudgets = homeViewModel::retryBudgets,
                    onChooseCategory = { category, type ->
                        viewModel.startAdd()
                        viewModel.setCategory(category)
                        viewModel.setType(type)
                        navController.navigate(ADD_ROUTE) { launchSingleTop = true }
                    },
                ),
            )
        }
        composable(LIST_ROUTE) {
            TransactionListScreen(
                state = state,
                onQueryChange = viewModel::setQuery,
                onFilterChange = viewModel::setFilter,
                onMonthChange = viewModel::changeMonth,
                onResetFilters = viewModel::resetFilters,
                onAdd = openAdd,
                onOpenRecord = openRecord,
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
