    package com.ngyennhi.expensemanager.presentation

    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.WindowInsets
    import androidx.compose.foundation.layout.consumeWindowInsets
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.Scaffold
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.getValue
    import androidx.compose.ui.Modifier
    import androidx.lifecycle.compose.collectAsStateWithLifecycle
    import androidx.navigation.NavType
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.rememberNavController
    import androidx.navigation.compose.currentBackStackEntryAsState
    import androidx.navigation.navArgument
    import com.ngyennhi.expensemanager.presentation.home.HomeScreen
    import com.ngyennhi.expensemanager.presentation.statistics.StatisticsScreen
    import com.ngyennhi.expensemanager.presentation.statistics.StatisticsViewModel
    import com.ngyennhi.expensemanager.presentation.home.HomeViewModel
    import com.ngyennhi.expensemanager.presentation.home.HomeActions
    import com.ngyennhi.expensemanager.presentation.transaction.TransactionViewModel
    import com.ngyennhi.expensemanager.presentation.transaction.TransactionListScreen
    import com.ngyennhi.expensemanager.presentation.transaction.TransactionFormScreen
    import com.ngyennhi.expensemanager.presentation.transaction.TransactionDetailScreen
    import com.ngyennhi.expensemanager.presentation.navigation.AppRoutes
    import com.ngyennhi.expensemanager.presentation.navigation.AppDestination
    import com.ngyennhi.expensemanager.presentation.common.components.AppBottomBar
    import com.ngyennhi.expensemanager.presentation.navigation.NavigationPlaceholderScreen
    import com.ngyennhi.expensemanager.presentation.navigation.navigateToTab
    import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

    @Composable
    fun ExpenseManagerApp(viewModel: TransactionViewModel, homeViewModel: HomeViewModel, statisticsViewModel: StatisticsViewModel) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val selectedTab = AppDestination.primaryForRoute(backStackEntry?.destination?.route)

        // Successful writes and missing-record returns discard secondary history,
        // restoring the list even when it has not been visited in this session.
        val returnToList: () -> Unit = {
            navController.navigate(AppRoutes.TRANSACTIONS) {
                popUpTo(AppRoutes.HOME)
                launchSingleTop = true
                restoreState = true
            }
        }
        val openAdd: () -> Unit = {
            viewModel.startAdd()
            navController.navigate(AppRoutes.ADD) { launchSingleTop = true }
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

        Scaffold(
            containerColor = ExpenseManagerColors.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                selectedTab?.let { selected ->
                    AppBottomBar(selected, navController::navigateToTab, openAdd)
                }
            },
        ) { padding ->
            NavHost(navController, startDestination = AppRoutes.HOME,
                modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                composable(AppRoutes.HOME) {
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
                                navController.navigate(AppRoutes.ADD) { launchSingleTop = true }
                            },
                        ),
                        showFloatingAdd = false,
                    )
                }
                composable(AppRoutes.TRANSACTIONS) {
                    TransactionListScreen(
                        state = state,
                        onQueryChange = viewModel::setQuery,
                        onFilterChange = viewModel::setFilter,
                        onMonthChange = viewModel::changeMonth,
                        onResetFilters = viewModel::resetFilters,
                        onApplyFilters = viewModel::applyFilters,
                        onAdd = openAdd,
                        onOpenRecord = openRecord,
                        onRetry = viewModel::retryObservation,
                        showFloatingAdd = false,
                    )
                }
                composable(AppRoutes.STATISTICS) {
                    val statisticsState by statisticsViewModel.uiState.collectAsStateWithLifecycle()
                    StatisticsScreen(statisticsState,
                        onPreviousMonth = { statisticsViewModel.changeMonth(-1) },
                        onNextMonth = { statisticsViewModel.changeMonth(1) },
                        onRetry = statisticsViewModel::retryObservation)
                }
                composable(AppRoutes.SETTINGS) { NavigationPlaceholderScreen(AppDestination.SETTINGS) }
                composable(AppRoutes.ADD) {
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
                composable(AppRoutes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
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
                composable(AppRoutes.EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
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
    }
