package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

enum class MemberStatus { Active, Busy, Offline }

data class FamilyMember(
    val name: String,
    val status: MemberStatus,
    val location: String,
    val lastSeen: String,
    val locationIcon: ImageVector,
    val initial: String,
    val avatarColor: Color
)

@Composable
fun FamilyScreen() {
    val members = listOf(
        FamilyMember(
            name = "Mom",
            status = MemberStatus.Active,
            location = "At Work",
            lastSeen = "Last seen 10m ago",
            locationIcon = Icons.Outlined.Work,
            initial = "M",
            avatarColor = Tertiary
        ),
        FamilyMember(
            name = "Dad",
            status = MemberStatus.Busy,
            location = "Commuting",
            lastSeen = "Last seen 5m ago",
            locationIcon = Icons.Outlined.DirectionsCar,
            initial = "D",
            avatarColor = Secondary
        ),
        FamilyMember(
            name = "Leo",
            status = MemberStatus.Active,
            location = "At School",
            lastSeen = "Last seen 1h ago",
            locationIcon = Icons.Outlined.School,
            initial = "L",
            avatarColor = Primary
        ),
        FamilyMember(
            name = "Sarah",
            status = MemberStatus.Offline,
            location = "At Home",
            lastSeen = "Last seen 2h ago",
            locationIcon = Icons.Outlined.Home,
            initial = "S",
            avatarColor = Outline
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 100.dp)
    ) {
        Text(
            text = "Family Hub",
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Check in on everyone's latest updates.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        members.forEach { member ->
            FamilyMemberCard(member = member)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Invite Member card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x0DFFFFFF))
                .border(
                    width = 2.dp,
                    color = Primary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
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
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Invite Member",
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun FamilyMemberCard(member: FamilyMember) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(member.avatarColor.copy(alpha = 0.3f), CircleShape)
                        .border(2.dp, Color(0x33FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.initial,
                        color = member.avatarColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }

                // Status badge
                val (statusColor, statusText) = when (member.status) {
                    MemberStatus.Active -> Tertiary to "Active"
                    MemberStatus.Busy -> Secondary to "Busy"
                    MemberStatus.Offline -> Outline to "Offline"
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Name
            Text(
                text = member.name,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                color = OnSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Location + last seen
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = member.locationIcon,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${member.location} • ${member.lastSeen}",
                    color = OnSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Divider(
                color = Color(0x1AFFFFFF),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Message button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x0DFFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Chat,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Message",
                            color = Primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
