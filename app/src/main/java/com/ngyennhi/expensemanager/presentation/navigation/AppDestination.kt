package com.ngyennhi.expensemanager.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavHostController
import com.ngyennhi.expensemanager.R

internal object AppRoutes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val ADD = "add"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"
    const val EDIT = "edit/{id}"
}

internal enum class AppDestination(
    val route: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    HOME(AppRoutes.HOME, R.string.home_title, R.drawable.ic_nav_home),
    TRANSACTIONS(AppRoutes.TRANSACTIONS, R.string.transactions_title, R.drawable.ic_nav_transactions),
    ADD(AppRoutes.ADD, R.string.nav_add, R.drawable.ic_nav_add),
    STATISTICS(AppRoutes.STATISTICS, R.string.statistics_title, R.drawable.ic_nav_statistics),
    SETTINGS(AppRoutes.SETTINGS, R.string.settings_title, R.drawable.ic_nav_settings);

    companion object {
        fun primaryForRoute(route: String?): AppDestination? = entries.firstOrNull {
            it != ADD && it.route == route
        }
    }
}

/** Primary tabs keep their saveable content, with Home as the single Back root. */
internal fun NavHostController.navigateToTab(destination: AppDestination) {
    require(destination != AppDestination.ADD)
    if (currentDestination?.route == destination.route) return
    navigate(destination.route) {
        popUpTo(AppRoutes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
