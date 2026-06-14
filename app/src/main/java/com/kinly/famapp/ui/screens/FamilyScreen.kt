package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinly.famapp.data.models.FamilyMember
import com.kinly.famapp.features.family.FamilyViewModel
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

@Composable
fun FamilyScreen(
    viewModel: FamilyViewModel,
    currentUserId: String
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 100.dp)
    ) {
        Text(
            text = uiState.family?.name ?: "Family Hub",
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Участники семьи и их статусы.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Invite code card
        val inviteCode = uiState.family?.inviteCode
        if (inviteCode != null) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Код приглашения", color = OnSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = inviteCode,
                            color = Primary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x0DFFFFFF))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                .clickable {
                                    clipboard.setText(AnnotatedString(inviteCode))
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Копировать", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.regenerateCode() },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Обновить код", color = OnSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.members.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Участники загружаются...", color = OnSurfaceVariant)
                }
            }
        } else {
            uiState.members.forEach { member ->
                RealFamilyMemberCard(member = member, isCurrentUser = member.userId == currentUserId)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Invite Member card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x0DFFFFFF))
                .border(width = 2.dp, color = Primary.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp))
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Primary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (inviteCode != null)
                        "Поделитесь кодом: $inviteCode"
                    else
                        "Пригласить участника",
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun RealFamilyMemberCard(member: FamilyMember, isCurrentUser: Boolean) {
    val memberColor = when (member.color) {
        "purple" -> Primary
        "pink" -> Secondary
        "blue" -> Tertiary
        else -> Primary
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(memberColor.copy(alpha = 0.3f), CircleShape)
                        .border(2.dp, Color(0x33FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = member.initial, color = memberColor, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }

                Box(
                    modifier = Modifier
                        .background(Tertiary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .border(1.dp, Tertiary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Tertiary, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCurrentUser) "Вы" else "Активен",
                            color = Tertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = member.displayName + if (isCurrentUser) " (Вы)" else "",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                color = OnSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Home, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Участник с ${member.joinedAt?.take(10) ?: "—"}",
                    color = OnSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x0DFFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Chat, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Message", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
