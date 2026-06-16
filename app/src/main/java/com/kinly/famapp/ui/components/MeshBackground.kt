package com.kinly.famapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Премиальный mesh-фон под glassmorphism: глубокий navy + мягкие цветные «пятна»
 * (фиолет, маджента, индиго, циан), которые просвечивают сквозь стеклянные карточки.
 */
@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // База — глубокий тёмный navy с лёгким вертикальным переходом
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0C1124), Color(0xFF080B18))
            )
        )

        fun glow(color: Color, center: Offset, radius: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color(0x00000000)),
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius
            )
        }

        // Верх-слева — фиолетовый
        glow(Color(0x665B3F9E), Offset(w * 0.05f, h * 0.02f), w * 0.85f)
        // Верх-справа — маджента/розовый
        glow(Color(0x59C026D3), Offset(w * 1.0f, h * 0.05f), w * 0.80f)
        // Центр — индиго (подсветка под карточками)
        glow(Color(0x3D4F46E5), Offset(w * 0.5f, h * 0.42f), w * 0.95f)
        // Низ-слева — фиолетово-синий
        glow(Color(0x4D6D28D9), Offset(w * 0.0f, h * 0.95f), w * 0.80f)
        // Низ-справа — циан
        glow(Color(0x3D0891B2), Offset(w * 1.0f, h * 1.0f), w * 0.75f)
    }
}
