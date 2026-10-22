package com.ngyennhi.expensemanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun figmaTextStyle(size: Int, weight: FontWeight) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * 1.35f).sp,
    letterSpacing = 0.sp,
)

val Typography = Typography(
    displaySmall = figmaTextStyle(38, FontWeight.Medium),
    headlineLarge = figmaTextStyle(36, FontWeight.SemiBold),
    headlineMedium = figmaTextStyle(26, FontWeight.SemiBold),
    headlineSmall = figmaTextStyle(24, FontWeight.SemiBold),
    titleLarge = figmaTextStyle(20, FontWeight.Medium),
    titleMedium = figmaTextStyle(18, FontWeight.SemiBold),
    titleSmall = figmaTextStyle(17, FontWeight.SemiBold),
    bodyLarge = figmaTextStyle(16, FontWeight.SemiBold),
    bodyMedium = figmaTextStyle(14, FontWeight.Normal),
    bodySmall = figmaTextStyle(12, FontWeight.Normal),
    labelLarge = figmaTextStyle(14, FontWeight.Medium),
    labelMedium = figmaTextStyle(13, FontWeight.Medium),
    labelSmall = figmaTextStyle(11, FontWeight.Normal),
)
