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
import com.ngyennhi.expensemanager.presentation.common.components.PrimaryNavigationBar
import com.ngyennhi.expensemanager.presentation.dashboard.DashboardScreen
import com.ngyennhi.expensemanager.presentation.dashboard.DashboardViewModel
import com.ngyennhi.expensemanager.presentation.dashboard.DashboardHomeActions
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
fun ExpenseManagerApp(viewModel: TransactionViewModel, dashboardViewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
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
    val primaryNavigation: @Composable (Boolean) -> Unit = { isHome ->
        PrimaryNavigationBar(
            isHome = isHome,
            onHome = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
            onTransactions = { if (isHome) returnToList() },
            onAdd = openAdd,
        )
    }

    LaunchedEffect(viewModel, navController) {
        viewModel.navigationEvents.collect {
            returnToList()
        }
    }

    NavHost(navController, startDestination = HOME_ROUTE, modifier = Modifier.fillMaxSize()) {
        composable(HOME_ROUTE) {
            LaunchedEffect(Unit) { dashboardViewModel.refreshGreeting() }
            DashboardScreen(
                state = dashboardState,
                onPreviousMonth = { dashboardViewModel.changeMonth(-1) },
                onNextMonth = { dashboardViewModel.changeMonth(1) },
                onAdd = openAdd,
                onViewAll = {
                    viewModel.showAllForMonth(dashboardState.selectedMonth)
                    returnToList()
                },
                onOpenRecord = openRecord,
                onRetry = dashboardViewModel::retryObservation,
                homeActions = DashboardHomeActions(
                    onEditBudget = dashboardViewModel::startBudgetEdit,
                    onBudgetInput = dashboardViewModel::setBudgetInput,
                    onSaveBudget = dashboardViewModel::saveMonthlyBudget,
                    onClearBudget = dashboardViewModel::clearMonthlyBudget,
                    onRetryBudgets = dashboardViewModel::retryBudgets,
                    onChooseCategory = { category, type ->
                        viewModel.startAdd()
                        viewModel.setCategory(category)
                        viewModel.setType(type)
                        navController.navigate(ADD_ROUTE) { launchSingleTop = true }
                    },
                ),
                bottomBar = { primaryNavigation(true) },
                showFloatingAdd = false,
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
                bottomBar = { primaryNavigation(false) },
                showFloatingAdd = false,
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
