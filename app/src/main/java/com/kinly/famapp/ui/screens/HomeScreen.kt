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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinly.famapp.data.models.Task
import com.kinly.famapp.features.family.FamilyViewModel
import com.kinly.famapp.features.shopping.ShoppingViewModel
import com.kinly.famapp.features.tasks.TasksViewModel
import com.kinly.famapp.ui.components.GlassButton
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

@Composable
fun HomeScreen(
    tasksViewModel: TasksViewModel,
    shoppingViewModel: ShoppingViewModel,
    familyViewModel: FamilyViewModel,
    currentUserId: String,
    userName: String = "",
    onOpenShopping: () -> Unit = {},
    onOpenTasks: () -> Unit = {}
) {
    val tasksState by tasksViewModel.uiState.collectAsState()
    val shoppingState by shoppingViewModel.uiState.collectAsState()
    val familyState by familyViewModel.uiState.collectAsState()

    // Показываем только задачи с проставленной звёздочкой (приоритет).
    // Если ни одна задача не отмечена — приоритетных нет, фолбэка на обычные задачи нет.
    val priorityTask: Task? = tasksState.tasks.firstOrNull { it.isPriority && !it.isCompleted }
    val activeTasks = tasksState.tasks.filter { !it.isCompleted }
    val previewTasks = activeTasks.filter { it.id != priorityTask?.id }.take(4)
    val previewItems = shoppingState.items.filter { !it.isChecked }.take(3)
    val members = familyState.members
    val tasksLeft = tasksState.tasks.count { !it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 100.dp)
    ) {
        // Greeting — приветствие зависит от времени суток + имя пользователя
        val greeting = remember {
            when (java.time.LocalTime.now().hour) {
                in 5..11 -> "Доброе утро"
                in 12..17 -> "Добрый день"
                in 18..22 -> "Добрый вечер"
                else -> "Доброй ночи"
            }
        }
        Text(
            text = if (userName.isNotBlank()) "$greeting, $userName!" else "$greeting!",
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Вот что происходит в вашей семье сегодня.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Priority Task Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenTasks() }
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.linearGradient(colors = listOf(Primary.copy(alpha = 0.2f), Color.Transparent))
                        )
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                                text = "ПРИОРИТЕТ",
                                color = Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        }
                        if (priorityTask?.dueDate != null) {
                            Box(
                                modifier = Modifier
                                    .background(Secondary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                    .border(1.dp, Secondary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = priorityTask.dueDate,
                                    color = Secondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Text(
                        text = priorityTask?.title ?: "Нет приоритетных задач",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                        color = OnSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (priorityTask?.description != null) {
                        Text(
                            text = priorityTask.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else if (priorityTask == null) {
                        Text(
                            text = "Отметьте задачу звёздочкой, чтобы она появилась здесь.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    if (priorityTask != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(Primary, Secondary)),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { tasksViewModel.completeTask(priorityTask.id) }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Выполнить",
                                color = Color(0xFF0B1326),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Stats Card — сводка по семье (без погоды)
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$tasksLeft",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                    Text(
                        text = "Осталось задач",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${shoppingState.items.count { !it.isChecked }}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Secondary
                    )
                    Text(
                        text = "Купить",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${members.size}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Tertiary
                    )
                    Text(
                        text = "Семья",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
        }

        // Shopping List Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenShopping() }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.ShoppingBasket, contentDescription = null, tint = Secondary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = shoppingState.currentList?.title ?: "Список покупок",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                            color = OnSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add",
                        tint = Primary,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { onOpenShopping() }
                    )
                }

                if (previewItems.isEmpty()) {
                    Text("Список пуст", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                } else {
                    previewItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(18.dp).border(1.dp, Outline, RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = item.title, style = MaterialTheme.typography.bodyMedium, color = OnSurface, modifier = Modifier.weight(1f))
                            if (item.quantity != null) {
                                Text(text = item.quantity, color = Outline, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                GlassButton(modifier = Modifier.fillMaxWidth().clickable { onOpenShopping() }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Открыть список", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Tasks Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenTasks() }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Checklist, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Задачи",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                            color = OnSurface
                        )
                    }
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Добавить", tint = Primary, modifier = Modifier.size(24.dp))
                }

                if (previewTasks.isEmpty()) {
                    Text(
                        if (activeTasks.isEmpty()) "Нет активных задач" else "Все задачи в приоритете",
                        color = OnSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    previewTasks.forEach { task ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(18.dp).border(1.dp, Outline, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (task.isPriority) {
                                    Icon(Icons.Filled.Star, null, tint = Secondary, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = task.title, style = MaterialTheme.typography.bodyMedium, color = OnSurface, modifier = Modifier.weight(1f))
                            if (task.dueDate != null) {
                                Text(text = task.dueDate, color = Outline, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                GlassButton(modifier = Modifier.fillMaxWidth().clickable { onOpenTasks() }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Все задачи", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Family Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Outlined.Group, contentDescription = null, tint = Tertiary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Семья", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp), color = OnSurface)
                }

                if (members.isEmpty()) {
                    Text("Участники загружаются...", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        members.take(2).forEach { member ->
                            val memberColor = when (member.color) {
                                "purple" -> Primary
                                "pink" -> Secondary
                                "blue" -> Tertiary
                                else -> Primary
                            }
                            GlassButton(modifier = Modifier.weight(1f).padding(4.dp)) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(memberColor.copy(0.3f), CircleShape)
                                            .border(2.dp, Color(0x33FFFFFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(member.initial, color = memberColor, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = member.displayName, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
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
                        .border(width = 2.dp, color = Primary.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.PersonAdd, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Пригласить участника", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
