package com.ngyennhi.expensemanager.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter

internal val LocalDarkAppearance = staticCompositionLocalOf { false }

/** Preserve exported light colors while keeping outline glyphs legible on dark surfaces. */
@Composable
internal fun appearanceIconFilter(darkColor: Color): ColorFilter? =
    if (LocalDarkAppearance.current) ColorFilter.tint(darkColor) else null

/** Shared solid palette: Figma light values and a contrast-oriented dark extension. */
object ExpenseManagerColors {
    val background: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFF15121B) else Color(0xFFFAF9FC)
    val surface: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFF211D28) else Color(0xFFFFFFFF)
    val textPrimary: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFFEAE3F0) else Color(0xFF292635)
    val textSecondary: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFFC2B8CD) else Color(0xFF777280)
    val primary: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFFD0B8EF) else Color(0xFF77609C)
    val primaryText: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFFE0C9FF) else Color(0xFF624A88)
    val primaryContainer: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFF49375F) else Color(0xFFEDE5F7)
    val income: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFFA8D3C0) else Color(0xFF3F7A63)
    val incomeContainer: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFF203E32) else Color(0xFFE0F1E9)
    val expense: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFFF0B5AE) else Color(0xFFA65C56)
    val expenseContainer: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFF502E2C) else Color(0xFFF9E4E1)
    val yellow: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFFE8D58D) else Color(0xFF8B7538)
    val yellowContainer: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFF443B20) else Color(0xFFF8EFCD)
    val mutedSurface: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFF302A39) else Color(0xFFF0EDF4)
    val outline: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFF51495C) else Color(0xFFEAE6EF)
    val chartPurple = Color(0xFFB9A4D7)
    val chartCoral = Color(0xFFEAB4AC)
    val chartGreen = Color(0xFFA8D3C0)
    val chartYellow = Color(0xFFE8D58D)
    val bankContainer: Color
        @Composable get() = if (LocalDarkAppearance.current) Color(0xFF26374D) else Color(0xFFE5EDF7)
}
