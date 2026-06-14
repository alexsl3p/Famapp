package com.kinly.famapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base background
        drawRect(color = Color(0xFF0B1326))

        // Top-left purple glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x26A855F7),
                    Color(0x00A855F7)
                ),
                center = Offset(0f, 0f),
                radius = w * 0.7f
            ),
            center = Offset(0f, 0f),
            radius = w * 0.7f
        )

        // Top-right pink glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x26DB2777),
                    Color(0x00DB2777)
                ),
                center = Offset(w, 0f),
                radius = w * 0.7f
            ),
            center = Offset(w, 0f),
            radius = w * 0.7f
        )

        // Bottom-right blue glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x260891B2),
                    Color(0x000891B2)
                ),
                center = Offset(w, h),
                radius = w * 0.7f
            ),
            center = Offset(w, h),
            radius = w * 0.7f
        )

        // Bottom-left purple glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x26A855F7),
                    Color(0x00A855F7)
                ),
                center = Offset(0f, h),
                radius = w * 0.7f
            ),
            center = Offset(0f, h),
            radius = w * 0.7f
        )
    }
}
