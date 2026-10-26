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
    import com.ngyennhi.expensemanager.presentation.navigation.navigateToTab
    import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

    import com.ngyennhi.expensemanager.presentation.wallet.*
    import androidx.compose.ui.res.stringResource
    import com.ngyennhi.expensemanager.R

    @Composable
    fun ExpenseManagerApp(viewModel: TransactionViewModel, homeViewModel: HomeViewModel, statisticsViewModel: StatisticsViewModel, walletViewModel: WalletViewModel) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val walletState by walletViewModel.uiState.collectAsStateWithLifecycle()
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

        LaunchedEffect(walletViewModel, navController) {
            walletViewModel.navigationEvents.collect { id ->
                if (id == null) navController.popBackStack(AppRoutes.WALLETS, false)
                else navController.navigate("wallet/$id") {
                    popUpTo(AppRoutes.WALLETS)
                    launchSingleTop = true
                }
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
                composable(AppRoutes.SETTINGS) {
                    SettingsWalletScreen(onWallets = { navController.navigate(AppRoutes.WALLETS) })
                }
                composable(AppRoutes.WALLETS) {
                    WalletListScreen(walletState, onBack = { navController.popBackStack() },
                        onAdd = { walletViewModel.startAdd(); navController.navigate(AppRoutes.WALLET_FORM) },
                        onOpen = { navController.navigate("wallet/$it") }, onRecord = openRecord, onRetry = walletViewModel::retry)
                }
                composable(AppRoutes.WALLET_DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = requireNotNull(entry.arguments?.getLong("id"))
                    WalletDetailScreen(walletState, id, onBack = { navController.popBackStack() },
                        onEdit = { walletViewModel.startEdit(id); navController.navigate(AppRoutes.WALLET_FORM) },
                        onDelete = { walletViewModel.delete(id) },
                        onAddTransaction = { viewModel.startAdd(id); navController.navigate(AppRoutes.ADD) },
                        onRecord = openRecord, onRetry = walletViewModel::retry)
                }
                composable(AppRoutes.WALLET_FORM) {
                    WalletFormScreen(walletState, onBack = { navController.popBackStack() },
                        onName = walletViewModel::setName, onType = walletViewModel::setType,
                        onOpening = walletViewModel::setOpening, onSave = walletViewModel::save, onRetry = walletViewModel::retry)
                }
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
                        walletState = walletState,
                        onWalletChange = viewModel::setWallet,
                        onRetryWallets = walletViewModel::retry,
                    )
                }
                composable(AppRoutes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = requireNotNull(entry.arguments?.getLong("id"))
                    LaunchedEffect(id) { viewModel.selectRecord(id) }
                    val selectedWallet = state.selectedRecord?.walletId?.let { walletId -> walletState.wallets.firstOrNull { it.id == walletId } }
                    TransactionDetailScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        onReturnToList = returnToList,
                        onEdit = {
                            viewModel.startEdit(id)
                            navController.navigate("edit/$id")
                        },
                        onDelete = viewModel::deleteSelected,
                        walletLoadFailed = walletState.status == com.ngyennhi.expensemanager.presentation.transaction.LoadStatus.ERROR,
                        onRetryWallets = walletViewModel::retry,
                        walletLabel = selectedWallet?.let { walletName(it) } ?: stringResource(
                            if (walletState.status == com.ngyennhi.expensemanager.presentation.transaction.LoadStatus.ERROR) R.string.wallet_read_error
                            else R.string.wallet_loading),
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
                        walletState = walletState,
                        onWalletChange = viewModel::setWallet,
                        onRetryWallets = walletViewModel::retry,
                    )
                }
            }

        }
    }
