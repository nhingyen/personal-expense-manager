package com.ngyennhi.expensemanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

@Composable
private fun appColorScheme(dark: Boolean) = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = ExpenseManagerColors.primary,
    onPrimary = if (dark) Color(0xFF30203F) else Color.White,
    primaryContainer = ExpenseManagerColors.primaryContainer,
    onPrimaryContainer = ExpenseManagerColors.primaryText,
    secondary = ExpenseManagerColors.primaryText,
    onSecondary = if (dark) Color(0xFF30203F) else Color.White,
    secondaryContainer = ExpenseManagerColors.primaryContainer,
    onSecondaryContainer = ExpenseManagerColors.primaryText,
    tertiary = ExpenseManagerColors.yellow,
    onTertiary = if (dark) Color(0xFF443B20) else Color.White,
    tertiaryContainer = ExpenseManagerColors.yellowContainer,
    onTertiaryContainer = ExpenseManagerColors.yellow,
    error = ExpenseManagerColors.expense,
    onError = if (dark) Color(0xFF502E2C) else Color.White,
    errorContainer = ExpenseManagerColors.expenseContainer,
    onErrorContainer = ExpenseManagerColors.expense,
    background = ExpenseManagerColors.background,
    onBackground = ExpenseManagerColors.textPrimary,
    surface = ExpenseManagerColors.surface,
    onSurface = ExpenseManagerColors.textPrimary,
    surfaceVariant = ExpenseManagerColors.mutedSurface,
    onSurfaceVariant = ExpenseManagerColors.textSecondary,
    outline = ExpenseManagerColors.outline,
    outlineVariant = ExpenseManagerColors.outline,
    surfaceTint = ExpenseManagerColors.primary,
    inverseSurface = ExpenseManagerColors.textPrimary,
    inverseOnSurface = ExpenseManagerColors.surface,
    inversePrimary = ExpenseManagerColors.primaryContainer,
    scrim = ExpenseManagerColors.textPrimary,
    surfaceDim = ExpenseManagerColors.background,
    surfaceBright = ExpenseManagerColors.surface,
    surfaceContainerLowest = ExpenseManagerColors.surface,
    surfaceContainerLow = ExpenseManagerColors.background,
    surfaceContainer = ExpenseManagerColors.surface,
    surfaceContainerHigh = ExpenseManagerColors.mutedSurface,
    surfaceContainerHighest = ExpenseManagerColors.mutedSurface,
)

@Composable
fun PersonalExpenseManagerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkAppearance provides darkTheme) {
        MaterialTheme(
            colorScheme = appColorScheme(darkTheme),
            typography = Typography,
            content = content,
        )
    }
}
