package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinly.famapp.data.models.Notification
import com.kinly.famapp.features.notifications.NotificationSettings
import com.kinly.famapp.features.notifications.NotificationViewModel
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Открыли экран — помечаем всё прочитанным (бейдж гаснет).
    LaunchedEffect(Unit) { viewModel.markAllRead() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад", tint = OnSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text("Уведомления", style = MaterialTheme.typography.headlineSmall, color = OnSurface)
            Spacer(Modifier.weight(1f))
            if (state.notifications.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAll() }) {
                    Text("Очистить", color = OnSurfaceVariant, fontSize = 13.sp)
                }
            }
        }

        if (state.notifications.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    tint = Outline,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Пока нет уведомлений", color = OnSurfaceVariant, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Здесь появятся новые товары в списке\nи назначенные вам задачи",
                    color = Outline,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.notifications, key = { it.id }) { n ->
                    NotificationRow(n)
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: Notification) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(iconColors(n.type))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconFor(n.type),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(n.title, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (!n.body.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(n.body, color = OnSurfaceVariant, fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(formatNotifTime(n.createdAt), color = Outline, fontSize = 11.sp)
            }
        }
    }
}

private fun iconFor(type: String): ImageVector = when (type) {
    "shopping_added" -> Icons.Outlined.ShoppingCart
    "task_assigned" -> Icons.Outlined.AssignmentInd
    "task_completed" -> Icons.Outlined.CheckCircle
    else -> Icons.Outlined.AddTask
}

private fun iconColors(type: String): List<Color> = when (type) {
    "shopping_added" -> listOf(Color(0xFF4F46E5), Color(0xFF22B7E6))
    "task_assigned" -> listOf(Color(0xFFD946EF), Color(0xFFFF6FA5))
    "task_completed" -> listOf(Color(0xFF16A34A), Color(0xFF4ADE80))
    else -> AccentGradient
}

/** "2026-06-17T20:11:33+00:00" → "17.06 20:11" (без тяжёлых date-библиотек). */
private fun formatNotifTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        val date = iso.substringBefore('T')        // 2026-06-17
        val time = iso.substringAfter('T').take(5)  // 20:11
        val parts = date.split('-')
        if (parts.size == 3) "${parts[2]}.${parts[1]} $time" else iso
    }.getOrElse { "" }
}
