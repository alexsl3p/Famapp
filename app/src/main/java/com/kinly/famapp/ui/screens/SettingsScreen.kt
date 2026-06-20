package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
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
import com.kinly.famapp.features.appearance.AppearanceViewModel
import com.kinly.famapp.features.notifications.NotificationViewModel
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.AccentGradient
import com.kinly.famapp.ui.theme.FontThemes
import com.kinly.famapp.ui.theme.OnSurface
import com.kinly.famapp.ui.theme.OnSurfaceVariant
import com.kinly.famapp.ui.theme.Outline
import com.kinly.famapp.ui.theme.Primary

@Composable
private fun ScreenScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
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
            Text(title, style = MaterialTheme.typography.headlineSmall, color = OnSurface)
        }
        content()
    }
}

/** Главный экран настроек — две кнопки. */
@Composable
fun SettingsScreen(
    unreadCount: Int,
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAppearance: () -> Unit
) {
    ScreenScaffold("Настройки", onBack) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                MenuRow(Icons.Outlined.NotificationsNone, "Уведомления", "Что присылать и история", badge = unreadCount, onClick = onOpenNotifications)
                HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                MenuRow(Icons.Outlined.Palette, "Внешний вид", "Шрифты и оформление", onClick = onOpenAppearance)
            }
        }
    }
}

/** Настройки уведомлений: типы + переход в историю. */
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val s = state.settings

    ScreenScaffold("Уведомления", onBack) {
        GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
            Column {
                MenuRow(
                    Icons.Outlined.History, "История уведомлений", "Кто что добавил и назначил",
                    badge = state.unreadCount, onClick = onOpenHistory
                )
            }
        }

        SectionLabel("Что присылать")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                ToggleRow("Новые товары в списке", s.shopping, viewModel::setShopping)
                ToggleRow("Назначенные мне задачи", s.assigned, viewModel::setAssigned)
                ToggleRow("Новые задачи в семье", s.created, viewModel::setCreated)
                ToggleRow("Выполненные задачи", s.completed, viewModel::setCompleted)
            }
        }
    }
}

/** Внешний вид: выбор темы шрифтов. */
@Composable
fun AppearanceScreen(
    appearanceViewModel: AppearanceViewModel,
    onBack: () -> Unit
) {
    val fontThemeId by appearanceViewModel.fontThemeId.collectAsState()

    ScreenScaffold("Внешний вид", onBack) {
        SectionLabel("Шрифты")
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
private fun MenuRow(icon: ImageVector, title: String, subtitle: String, badge: Int = 0, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(Brush.linearGradient(AccentGradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0B1326), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = OnSurfaceVariant, fontSize = 12.sp)
        }
        if (badge > 0) {
            Box(
                modifier = Modifier.clip(CircleShape).background(Color(0xFFFF4D6D)).padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(if (badge > 9) "9+" else badge.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Outline)
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
