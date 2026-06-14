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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinly.famapp.ui.components.GlassButton
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 100.dp)
    ) {
        // Greeting
        Text(
            text = "Good Morning, Sarah",
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Here's what's happening with the family today.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Priority Task Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Primary.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Primary.copy(alpha = 0.2f), CircleShape)
                                    .padding(2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PRIORITY",
                                color = Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Secondary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                .border(1.dp, Secondary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Due 2:00 PM",
                                color = Secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = "Pick up Leo from Soccer",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                        color = OnSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Westside Sports Complex. Don't forget his water bottle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Primary, Secondary)
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Complete",
                            color = Color(0xFF0B1326),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Weather + Stats Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.WbCloudy,
                    contentDescription = null,
                    tint = Tertiary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "72°",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Text(
                    text = "Partly Cloudy, Seattle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Divider(color = Color(0x1AFFFFFF))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "3",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                        Text(
                            text = "Tasks Left",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "1",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Secondary
                        )
                        Text(
                            text = "Event",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }

        // Shopping List Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingBasket,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Shopping List",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                            color = OnSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add",
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Items
                val items = listOf(
                    Pair("Oat Milk", "M"),
                    Pair("Whole Wheat Bread", "S"),
                    Pair("Apples", null)
                )
                items.forEach { (name, initials) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(1.dp, Outline, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (initials != null) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        if (initials == "M") TertiaryFixed else SecondaryContainer,
                                        CircleShape
                                    )
                                    .border(1.dp, Color(0xFF0B1326), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = if (initials == "M") OnTertiary else OnSecondaryContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                GlassButton(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "View All Items",
                            color = Primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Family Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = null,
                        tint = Tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Family",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                        color = OnSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mark
                    GlassButton(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Primary.copy(0.3f), CircleShape)
                                    .border(2.dp, Color(0x33FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("M", color = Primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Mark",
                                    color = OnSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "At Work",
                                    color = Tertiary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Leo
                    GlassButton(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Secondary.copy(0.3f), CircleShape)
                                    .border(2.dp, Color(0x33FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("L", color = Secondary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Leo",
                                    color = OnSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "At School",
                                    color = Secondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0DFFFFFF))
                        .border(
                            width = 2.dp,
                            color = Primary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
    }
}
