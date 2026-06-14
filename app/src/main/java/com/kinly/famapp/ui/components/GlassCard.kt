package com.kinly.famapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x8C0B1326))
            .border(
                width = 1.dp,
                color = Color(0x1AFFFFFF),
                shape = RoundedCornerShape(16.dp)
            ),
        content = content
    )
}

@Composable
fun GlassButton(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF))
            .border(
                width = 1.dp,
                color = Color(0x1AFFFFFF),
                shape = RoundedCornerShape(12.dp)
            ),
        content = content
    )
}
