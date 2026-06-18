package com.kinly.famapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kinly.famapp.R

/** Manrope — современный гротеск с кириллицей. */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_bold, FontWeight.ExtraBold),
)

/** Unbounded — округлый дисплейный, с кириллицей. */
val Unbounded = FontFamily(
    Font(R.font.unbounded_semibold, FontWeight.Medium),
    Font(R.font.unbounded_semibold, FontWeight.SemiBold),
    Font(R.font.unbounded_bold, FontWeight.Bold),
    Font(R.font.unbounded_extrabold, FontWeight.ExtraBold),
    Font(R.font.unbounded_extrabold, FontWeight.Black),
)

/** Onest — чистый геометрический гротеск, с кириллицей. */
val Onest = FontFamily(
    Font(R.font.onest_regular, FontWeight.Normal),
    Font(R.font.onest_medium, FontWeight.Medium),
    Font(R.font.onest_semibold, FontWeight.SemiBold),
    Font(R.font.onest_bold, FontWeight.Bold),
    Font(R.font.onest_bold, FontWeight.ExtraBold),
)

/** Playfair Display — элегантный антиквенный (serif), с кириллицей. */
val Playfair = FontFamily(
    Font(R.font.playfair_semibold, FontWeight.Normal),
    Font(R.font.playfair_semibold, FontWeight.Medium),
    Font(R.font.playfair_semibold, FontWeight.SemiBold),
    Font(R.font.playfair_bold, FontWeight.Bold),
    Font(R.font.playfair_bold, FontWeight.ExtraBold),
)

/** Inter — нейтральный интерфейсный гротеск, с кириллицей. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_bold, FontWeight.ExtraBold),
)

/** Montserrat — геометрический гротеск (аналог Poppins) с кириллицей. */
val Montserrat = FontFamily(
    Font(R.font.montserrat_medium, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold),
    Font(R.font.montserrat_bold, FontWeight.ExtraBold),
)

/** Comfortaa — округлый геометрический, с кириллицей. */
val Comfortaa = FontFamily(
    Font(R.font.comfortaa_semibold, FontWeight.Normal),
    Font(R.font.comfortaa_semibold, FontWeight.Medium),
    Font(R.font.comfortaa_semibold, FontWeight.SemiBold),
    Font(R.font.comfortaa_bold, FontWeight.Bold),
    Font(R.font.comfortaa_bold, FontWeight.ExtraBold),
)

/** Marck Script — элегантный связный курсив, с кириллицей (акцент на имени). */
val MarckScript = FontFamily(Font(R.font.marckscript_regular, FontWeight.Normal))

/** Caveat — рукописный, с кириллицей (акцент на имени). */
val Caveat = FontFamily(Font(R.font.caveat_bold, FontWeight.Bold))

/**
 * Тема шрифтов:
 *  - accent  — каллиграфический акцент (имя в приветствии),
 *  - display — заголовки/бренд,
 *  - body    — основной текст.
 */
data class FontTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val accent: FontFamily,
    val display: FontFamily,
    val body: FontFamily
)

val FontThemes = listOf(
    FontTheme("signature", "Тема 1 · Элегант", "Marck Script + Montserrat + Inter", MarckScript, Montserrat, Inter),
    FontTheme("romance", "Тема 2 · Романс", "Marck Script + Playfair + Inter", MarckScript, Playfair, Inter),
    FontTheme("cosmo", "Тема 3 · Космо", "Unbounded + Manrope", Unbounded, Unbounded, Manrope),
    FontTheme("clean", "Тема 4 · Чистая", "Manrope", Manrope, Manrope, Manrope),
    FontTheme("geo", "Тема 5 · Гео", "Onest", Onest, Onest, Onest),
    FontTheme("grace", "Тема 6 · Грация", "Caveat + Comfortaa + Inter", Caveat, Comfortaa, Inter),
)

fun fontThemeById(id: String?): FontTheme = FontThemes.firstOrNull { it.id == id } ?: FontThemes.first()

/** Текущая тема шрифтов, доступная во всём дереве композиции. */
val LocalAppFonts = staticCompositionLocalOf { FontThemes.first() }
