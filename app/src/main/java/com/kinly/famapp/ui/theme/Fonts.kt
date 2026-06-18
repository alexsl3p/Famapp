package com.kinly.famapp.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kinly.famapp.R

/** Satoshi — современный геометрический гротеск (текст, кнопки, подписи). */
val Satoshi = FontFamily(
    Font(R.font.satoshi_regular, FontWeight.Normal),
    Font(R.font.satoshi_medium, FontWeight.Medium),
    Font(R.font.satoshi_medium, FontWeight.SemiBold),
    Font(R.font.satoshi_bold, FontWeight.Bold),
    Font(R.font.satoshi_black, FontWeight.ExtraBold),
    Font(R.font.satoshi_black, FontWeight.Black),
)

/** Clash Display — трендовый дисплейный шрифт для заголовков. */
val ClashDisplay = FontFamily(
    Font(R.font.clashdisplay_regular, FontWeight.Normal),
    Font(R.font.clashdisplay_medium, FontWeight.Medium),
    Font(R.font.clashdisplay_semibold, FontWeight.SemiBold),
    Font(R.font.clashdisplay_bold, FontWeight.Bold),
    Font(R.font.clashdisplay_bold, FontWeight.ExtraBold),
)
