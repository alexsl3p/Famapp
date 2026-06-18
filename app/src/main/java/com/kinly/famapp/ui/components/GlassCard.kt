package com.kinly.famapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Стеклянная карточка с «объёмом»: тёмная тень-подъём снизу, лёгкий блик сверху
 * и градиентная рамка (ярче сверху) — имитация glassmorphism.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .shadow(elevation = 10.dp, shape = shape, clip = false, spotColor = Color(0x66000000), ambientColor = Color(0x4D000000))
            .clip(shape)
            // Лёгкий тёмно-фиолетовый «стекло»-тон — фон хорошо просвечивает сквозь карточку
            .background(Color(0x401B1640))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x47FFFFFF), Color(0x0FFFFFFF)))
                ),
                shape
            )
    ) {
        // Верхний блик стекла
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x1FFFFFFF), Color(0x00FFFFFF), Color(0x14000000))
                    )
                )
        )
        content()
    }
}

@Composable
fun GlassButton(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0x12FFFFFF))
            .border(
                BorderStroke(1.dp, Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color(0x0AFFFFFF)))),
                shape
            ),
        content = content
    )
}
