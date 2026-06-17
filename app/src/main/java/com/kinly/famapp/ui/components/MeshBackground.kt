package com.kinly.famapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.kinly.famapp.R

/**
 * Фон приложения — фирменное лунно-фиолетовое фото + тёмный скрим,
 * чтобы стеклянные карточки и белый текст оставались читаемыми (glassmorphism).
 */
@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_night),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Затемняющий градиент для читаемости интерфейса
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x730A0820),
                            Color(0x99090718),
                            Color(0xB3070512)
                        )
                    )
                )
        )
    }
}
