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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

data class Task(
    val title: String,
    val date: String,
    val category: String,
    val categoryColor: Color,
    val assigneeInitial: String,
    val accentColor: Color
)

@Composable
fun TasksScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Mine", "Completed")

    val tasks = listOf(
        Task("Pick up groceries", "Today, 5:00 PM", "Errand", Color(0xFFFFD8E7), "D", Color(0xFFFFD8E7)),
        Task("Walk the dog", "Today, 7:00 PM", "Pets", Color(0xFF7BD0FF), "L", Color(0xFF7BD0FF)),
        Task("Pay utility bill", "Tomorrow", "Finance", Color(0xFF4A4455), "M", Color(0xFF4A4455)),
        Task("Clean garage", "Saturday", "Home", Color(0xFFD2BBFF), "+2", Color(0xFFD2BBFF)),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 100.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Family Tasks",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = "4 remaining today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0B0C0)
                )
            }

            // Add button
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(listOf(Primary, Secondary)),
                        RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Task",
                    tint = Color(0xFF0B1326),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selectedTab == index)
                                Color(0x40630ED4)
                            else
                                Color(0x0DFFFFFF)
                        )
                        .border(
                            1.dp,
                            if (selectedTab == index) Color(0x80630ED4) else Color(0x1AFFFFFF),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .then(Modifier.wrapContentWidth()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (selectedTab == index) Primary else Color(0xFFB0B0C0),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Task cards
        tasks.forEach { task ->
            TaskCard(task = task)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun TaskCard(task: Task) {
    var checked by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(task.accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(0xFF0B1326),
                            checkedColor = Primary,
                            uncheckedColor = Outline
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            color = if (checked) OnSurfaceVariant else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            textDecoration = if (checked) TextDecoration.LineThrough else null
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFFB0B0C0),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.date,
                                color = Color(0xFFB0B0C0),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0x1AFFFFFF))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category tag
                    Box(
                        modifier = Modifier
                            .background(
                                task.categoryColor.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = task.category,
                            color = task.categoryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Assignee avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(2.dp, Color(0xFF131B2E), CircleShape)
                            .background(SurfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = task.assigneeInitial,
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
