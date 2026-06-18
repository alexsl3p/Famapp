package com.kinly.famapp.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kinly.famapp.R

/** Manrope — современный гротеск с полной кириллицей (текст, кнопки, подписи). */
val BodySans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_bold, FontWeight.ExtraBold),
)

/** Unbounded — трендовый округлый дисплейный шрифт с кириллицей (заголовки, акценты). */
val DisplaySans = FontFamily(
    Font(R.font.unbounded_semibold, FontWeight.Medium),
    Font(R.font.unbounded_semibold, FontWeight.SemiBold),
    Font(R.font.unbounded_bold, FontWeight.Bold),
    Font(R.font.unbounded_extrabold, FontWeight.ExtraBold),
    Font(R.font.unbounded_extrabold, FontWeight.Black),
)
