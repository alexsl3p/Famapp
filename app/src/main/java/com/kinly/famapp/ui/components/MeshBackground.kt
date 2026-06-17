package com.kinly.famapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.kinly.famapp.R

/**
 * Фон приложения зависит от времени суток:
 *  • утро/день (6:00–18:00) — розовые облака с планетой;
 *  • вечер/ночь — лунно-фиолетовый пляж.
 * Поверх — тёмный скрим, чтобы стеклянные карточки и текст читались.
 */
@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    val isDay = remember { java.time.LocalTime.now().hour in 6..17 }
    val bg = if (isDay) R.drawable.bg_day else R.drawable.bg_night
    // Днём фон ярче, поэтому скрим чуть плотнее для читаемости.
    val scrim = if (isDay) {
        listOf(Color(0x8C0A0820), Color(0xA6090718), Color(0xC2070512))
    } else {
        listOf(Color(0x730A0820), Color(0x99090718), Color(0xB3070512))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(scrim))
        )
    }
}
