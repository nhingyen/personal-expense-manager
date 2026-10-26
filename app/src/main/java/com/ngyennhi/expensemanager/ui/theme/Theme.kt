package com.ngyennhi.expensemanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ExpenseManagerColors.primary,
    onPrimary = ExpenseManagerColors.surface,
    primaryContainer = ExpenseManagerColors.primaryContainer,
    onPrimaryContainer = ExpenseManagerColors.primaryText,
    secondary = ExpenseManagerColors.primaryText,
    onSecondary = ExpenseManagerColors.surface,
    secondaryContainer = ExpenseManagerColors.primaryContainer,
    onSecondaryContainer = ExpenseManagerColors.primaryText,
    tertiary = ExpenseManagerColors.yellow,
    onTertiary = ExpenseManagerColors.surface,
    tertiaryContainer = ExpenseManagerColors.yellowContainer,
    onTertiaryContainer = ExpenseManagerColors.yellow,
    error = ExpenseManagerColors.expense,
    onError = ExpenseManagerColors.surface,
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
fun PersonalExpenseManagerTheme(content: @Composable () -> Unit) {
    // Figma only defines a light palette. Keep it fixed until a dark design exists.
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
