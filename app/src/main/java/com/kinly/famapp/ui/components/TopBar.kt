package com.kinly.famapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kinly.famapp.ui.theme.ClashDisplay
import com.kinly.famapp.ui.theme.Primary

@Composable
fun KinlyTopBar(
    modifier: Modifier = Modifier,
    showUserAvatar: Boolean = true,
    userInitial: String = "S",
    avatarUrl: String? = null,
    unreadCount: Int = 0,
    onAvatarClick: () -> Unit = {},
    onBellClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x990B1326))
            .border(
                width = 1.dp,
                color = Color(0x1AFFFFFF),
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.3f))
                .border(width = 2.dp, color = Color(0x33FFFFFF), shape = CircleShape)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Профиль",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    text = userInitial,
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Title
        Text(
            text = "Family OS",
            color = Primary,
            fontFamily = ClashDisplay,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

        // Notification bell с бейджем непрочитанных
        IconButton(onClick = onBellClick) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Уведомления",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 5.dp, y = (-3).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF4D6D))
                            .border(1.5.dp, Color(0xFF0B1326), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
