package com.kinly.famapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kinly.famapp.features.update.AppUpdateInfo
import com.kinly.famapp.features.update.UpdateState
import com.kinly.famapp.ui.theme.AccentGradient
import com.kinly.famapp.ui.theme.OnSurface
import com.kinly.famapp.ui.theme.OnSurfaceVariant
import com.kinly.famapp.ui.theme.Outline
import com.kinly.famapp.ui.theme.Primary

@Composable
fun UpdateDialog(
    state: UpdateState,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state) {
        is UpdateState.Available -> AvailableContent(state.info, onUpdate, onDismiss)
        is UpdateState.Downloading -> DownloadingContent(state.progress)
        is UpdateState.Error -> ErrorContent(state.message, onDismiss)
        UpdateState.Idle -> Unit
    }
}

@Composable
private fun UpdateShell(dismissable: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if (dismissable) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xF21A1430))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun AvailableContent(info: AppUpdateInfo, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    UpdateShell(dismissable = true, onDismiss = onDismiss) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(AccentGradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.SystemUpdateAlt, contentDescription = null, tint = Color(0xFF0B1326), modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Доступно обновление", color = OnSurface, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        if (info.versionName.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("Версия ${info.versionName}", color = OnSurfaceVariant, fontSize = 13.sp)
        }
        if (!info.notes.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(info.notes, color = OnSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(AccentGradient))
                .clickable { onUpdate() }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Обновить", color = Color(0xFF0B1326), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Позже", color = OnSurfaceVariant, fontSize = 14.sp)
        }
    }
}

@Composable
private fun DownloadingContent(progress: Float) {
    UpdateShell(dismissable = false, onDismiss = {}) {
        Text("Скачивание обновления…", color = OnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        if (progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                color = Primary,
                trackColor = Color(0x1FFFFFFF)
            )
            Spacer(Modifier.height(8.dp))
            Text("${(progress * 100).toInt()}%", color = OnSurfaceVariant, fontSize = 13.sp)
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                color = Primary,
                trackColor = Color(0x1FFFFFFF)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("Не закрывайте приложение", color = Outline, fontSize = 12.sp)
    }
}

@Composable
private fun ErrorContent(message: String, onDismiss: () -> Unit) {
    UpdateShell(dismissable = true, onDismiss = onDismiss) {
        Text("Не получилось", color = OnSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(message, color = OnSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                .clickable { onDismiss() }.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Закрыть", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}
