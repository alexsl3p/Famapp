package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinly.famapp.features.appearance.AppearanceViewModel
import com.kinly.famapp.features.notifications.NotificationViewModel
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.FontThemes
import com.kinly.famapp.ui.theme.OnSurface
import com.kinly.famapp.ui.theme.OnSurfaceVariant
import com.kinly.famapp.ui.theme.Outline
import com.kinly.famapp.ui.theme.Primary

@Composable
fun SettingsScreen(
    notificationViewModel: NotificationViewModel,
    appearanceViewModel: AppearanceViewModel,
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    val notifState by notificationViewModel.uiState.collectAsState()
    val fontThemeId by appearanceViewModel.fontThemeId.collectAsState()
    val s = notifState.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад", tint = OnSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text("Настройки", style = MaterialTheme.typography.headlineSmall, color = OnSurface)
        }

        // ───────── Уведомления ─────────
        SectionLabel("Уведомления")
        GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                // Открыть ленту уведомлений
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenNotifications() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Все уведомления", color = OnSurface, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (notifState.unreadCount > 0) {
                        Box(
                            modifier = Modifier.clip(CircleShape).background(Color(0xFFFF4D6D)).padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(notifState.unreadCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Outline)
                }
                HorizontalDivider(color = Color(0x14FFFFFF))
                Text(
                    "Какие уведомления получать",
                    color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                ToggleRow("Новые товары в списке", s.shopping, notificationViewModel::setShopping)
                ToggleRow("Назначенные мне задачи", s.assigned, notificationViewModel::setAssigned)
                ToggleRow("Новые задачи в семье", s.created, notificationViewModel::setCreated)
                ToggleRow("Выполненные задачи", s.completed, notificationViewModel::setCompleted)
            }
        }

        // ───────── Оформление ─────────
        SectionLabel("Оформление")
        Text(
            "Шрифты",
            color = OnSurfaceVariant, fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                FontThemes.forEachIndexed { index, theme ->
                    val selected = theme.id == fontThemeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { appearanceViewModel.setFontTheme(theme.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Превью названия — самим шрифтом темы
                            Text(
                                theme.title,
                                color = if (selected) Primary else OnSurface,
                                fontFamily = theme.display,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Съешь ещё этих булочек · ${theme.subtitle}",
                                color = OnSurfaceVariant,
                                fontFamily = theme.body,
                                fontSize = 13.sp
                            )
                        }
                        RadioButton(
                            selected = selected,
                            onClick = { appearanceViewModel.setFontTheme(theme.id) },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary, unselectedColor = Outline)
                        )
                    }
                    if (index < FontThemes.lastIndex) HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = OnSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = OnSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0B1326),
                checkedTrackColor = Primary,
                uncheckedThumbColor = Outline,
                uncheckedTrackColor = Color(0x14FFFFFF)
            )
        )
    }
}
