package com.kinly.famapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Строит Typography под выбранную тему шрифтов: крупное/бренд — display, остальное — body. */
fun appTypography(theme: FontTheme): Typography {
    val display = theme.display
    val body = theme.body
    return Typography(
        displayLarge = TextStyle(
            fontFamily = display, fontWeight = FontWeight.ExtraBold,
            fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = display, fontWeight = FontWeight.Bold,
            fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = body, fontWeight = FontWeight.Bold,
            fontSize = 24.sp, lineHeight = 31.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = body, fontWeight = FontWeight.Bold,
            fontSize = 23.sp, lineHeight = 29.sp
        ),
        titleLarge = TextStyle(
            fontFamily = body, fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp, lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = body, fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp, lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = body, fontWeight = FontWeight.Normal,
            fontSize = 17.sp, lineHeight = 26.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = body, fontWeight = FontWeight.Normal,
            fontSize = 15.sp, lineHeight = 22.sp
        ),
        bodySmall = TextStyle(
            fontFamily = body, fontWeight = FontWeight.Normal,
            fontSize = 13.sp, lineHeight = 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = body, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = body, fontWeight = FontWeight.Medium,
            fontSize = 12.sp, lineHeight = 17.sp
        ),
        labelSmall = TextStyle(
            fontFamily = body, fontWeight = FontWeight.Medium,
            fontSize = 11.sp, lineHeight = 15.sp
        )
    )
}
