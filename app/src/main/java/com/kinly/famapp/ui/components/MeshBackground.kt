package com.kinly.famapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Glassmorphism-фон в фирменных лавандово-розовых тонах (как у кнопок):
 * глубокая индиго-база + мягкие пастельные «пятна» (лаванда, розовый, фиолет, маджента),
 * которые красиво просвечивают сквозь стеклянные карточки.
 */
@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // База — глубокий индиго → почти чёрный (чтобы белый текст на карточках читался)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF15123A), Color(0xFF0C0A20), Color(0xFF080612))
            )
        )

        fun glow(color: Color, cx: Float, cy: Float, r: Float) {
            val center = Offset(w * cx, h * cy)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color(0x00000000)),
                    center = center,
                    radius = w * r
                ),
                center = center,
                radius = w * r
            )
        }

        // Верх-слева — лаванда (Primary)
        glow(Color(0x73B79CFF), 0.02f, 0.00f, 0.90f)
        // Верх-справа — розовый (Secondary)
        glow(Color(0x66FFAEDA), 1.00f, 0.04f, 0.85f)
        // Центр — фиолет (подсветка под карточками)
        glow(Color(0x4D8B5CF6), 0.55f, 0.40f, 0.95f)
        // Низ-слева — маджента
        glow(Color(0x59E879F9), 0.00f, 0.92f, 0.80f)
        // Низ-справа — сиренево-синий
        glow(Color(0x4D7C6FF0), 1.00f, 1.00f, 0.78f)
    }
}
