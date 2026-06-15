package com.kinly.famapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kinly.famapp.data.models.FamilyMember
import com.kinly.famapp.data.models.Task
import com.kinly.famapp.data.models.TaskComment
import com.kinly.famapp.features.tasks.TaskFilter
import com.kinly.famapp.features.tasks.TasksViewModel
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset

@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    familyId: String,
    currentUserId: String,
    members: List<FamilyMember> = emptyList()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredTasks = viewModel.getFilteredTasks()
    var showAddDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val membersMap = remember(members) { members.associate { it.userId to it.displayName } }
    val tabs = listOf("All" to TaskFilter.ALL, "Mine" to TaskFilter.MINE, "Done" to TaskFilter.COMPLETED)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 100.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(text = "Family Tasks", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(
                        text = "${filteredTasks.size} задач",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB0B0C0)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(listOf(Primary, Secondary)), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showAddDialog = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Task", tint = Color(0xFF0B1326), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tabs.forEach { (label, filter) ->
                    val isSelected = uiState.filter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0x40630ED4) else Color(0x0DFFFFFF))
                            .border(1.dp, if (isSelected) Color(0x80630ED4) else Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .wrapContentWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Primary else Color(0xFFB0B0C0),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (filteredTasks.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Задач нет", color = OnSurfaceVariant)
                    }
                }
            } else {
                filteredTasks.forEach { task ->
                    RealTaskCard(
                        task = task,
                        membersMap = membersMap,
                        currentUserId = currentUserId,
                        onComplete = { viewModel.completeTask(task.id) },
                        onUncomplete = { viewModel.uncompleteTask(task.id) },
                        onTogglePriority = { viewModel.togglePriority(task.id, !task.isPriority) },
                        onOpenComments = { viewModel.openComments(task.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            members = members,
            currentUserId = currentUserId,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, dueDate, assignedTo, repeatType, description, isPriority, photoBytes ->
                viewModel.createTask(
                    title = title,
                    assignedTo = assignedTo,
                    dueDate = dueDate,
                    repeatType = repeatType,
                    description = description,
                    isPriority = isPriority,
                    photoBytes = photoBytes
                )
                showAddDialog = false
            }
        )
    }

    val commentsState by viewModel.commentsState.collectAsState()
    if (commentsState.taskId != null) {
        CommentsSheet(
            comments = commentsState.comments,
            isLoading = commentsState.isLoading,
            isSending = commentsState.isSending,
            membersMap = membersMap,
            onSend = { body, photo -> viewModel.addComment(body, photo) },
            onDismiss = { viewModel.closeComments() }
        )
    }
}

/** Кнопка выбора изображения через системный Photo Picker; отдаёт байты. */
@Composable
fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                }
                if (bytes != null) onPicked(bytes)
            }
        }
    }
    return {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    comments: List<TaskComment>,
    isLoading: Boolean,
    isSending: Boolean,
    membersMap: Map<String, String>,
    onSend: (body: String?, photo: ByteArray?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var input by remember { mutableStateOf("") }
    var pendingPhoto by remember { mutableStateOf<ByteArray?>(null) }
    val pickPhoto = rememberPhotoPicker { pendingPhoto = it }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1D2538)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .heightIn(min = 200.dp, max = 520.dp)
        ) {
            Text("Комментарии", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                comments.isEmpty() -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Пока нет комментариев", color = OnSurfaceVariant)
                }
                else -> LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(comments) { comment ->
                        CommentRow(comment, membersMap[comment.authorId])
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            pendingPhoto?.let {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Фото прикреплено", color = Primary, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("✕", color = OnSurfaceVariant, modifier = Modifier.clickable { pendingPhoto = null })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = pickPhoto) {
                    Icon(Icons.Outlined.AddAPhoto, contentDescription = "Фото", tint = Primary)
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Комментарий…", color = Outline) },
                    modifier = Modifier.weight(1f),
                    colors = taskFieldColors(),
                    maxLines = 3
                )
                IconButton(
                    enabled = !isSending && (input.isNotBlank() || pendingPhoto != null),
                    onClick = {
                        onSend(input.takeIf { it.isNotBlank() }, pendingPhoto)
                        input = ""
                        pendingPhoto = null
                    }
                ) {
                    if (isSending) CircularProgressIndicator(color = Primary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.Send, contentDescription = "Отправить", tint = Primary)
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: TaskComment, authorName: String?) {
    Column {
        if (authorName != null) {
            Text(authorName, color = Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
        }
        if (!comment.body.isNullOrBlank()) {
            Text(comment.body, color = OnSurface, fontSize = 14.sp)
        }
        if (comment.imageUrl != null) {
            Spacer(Modifier.height(6.dp))
            AsyncImage(
                model = comment.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
fun RealTaskCard(
    task: Task,
    membersMap: Map<String, String> = emptyMap(),
    currentUserId: String = "",
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onTogglePriority: () -> Unit = {},
    onOpenComments: () -> Unit = {}
) {
    val accentColor = when {
        task.isCompleted -> Outline
        task.isPriority -> Secondary
        else -> Primary
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
            Column(
                modifier = Modifier
                    .clickable { onOpenComments() }
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { if (it) onComplete() else onUncomplete() },
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
                            color = if (task.isCompleted) OnSurfaceVariant else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                        )
                        if (!task.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = task.description,
                                color = OnSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                        if (task.dueDate != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.CalendarToday, contentDescription = null, tint = Color(0xFFB0B0C0), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = task.dueDate, color = Color(0xFFB0B0C0), fontSize = 12.sp)
                            }
                        }
                        // Attribution
                        val creatorName = membersMap[task.createdBy]
                        val completorName = if (task.isCompleted && task.completedBy != null) membersMap[task.completedBy] else null
                        if (creatorName != null || completorName != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    completorName != null -> "Выполнил: $completorName"
                                    creatorName != null -> "Создал: $creatorName"
                                    else -> ""
                                },
                                color = Color(0xFF7070A0),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onTogglePriority, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (task.isPriority) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Приоритет",
                            tint = if (task.isPriority) Secondary else Outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0x1AFFFFFF))
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Primary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (task.repeatType) {
                                "daily" -> "Ежедневно"
                                "weekly" -> "Еженедельно"
                                "monthly" -> "Ежемесячно"
                                else -> "Разовая"
                            },
                            color = Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (task.assignedTo != null) {
                        val assigneeName = membersMap[task.assignedTo] ?: task.assignedTo.take(4)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .border(2.dp, Color(0xFF131B2E), CircleShape)
                                .background(SurfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = assigneeName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    members: List<FamilyMember> = emptyList(),
    currentUserId: String = "",
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        dueDate: String?,
        assignedTo: String?,
        repeatType: String,
        description: String?,
        isPriority: Boolean,
        photoBytes: ByteArray?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<String?>(null) }
    var assignedToId by remember { mutableStateOf<String?>(null) }
    var repeatType by remember { mutableStateOf("none") }
    var isPriority by remember { mutableStateOf(false) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAssigneeDropdown by remember { mutableStateOf(false) }
    var showRepeatDropdown by remember { mutableStateOf(false) }
    val pickPhoto = rememberPhotoPicker { photoBytes = it }

    val datePickerState = rememberDatePickerState()
    val repeatOptions = listOf(
        "none" to "Не повторяется",
        "daily" to "Ежедневно",
        "weekly" to "Еженедельно",
        "monthly" to "Ежемесячно"
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis?.let { millis ->
                        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("OK", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена", color = OnSurfaceVariant) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1D2538),
        title = { Text("Новая задача", color = OnSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название задачи", color = Outline) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = taskFieldColors(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Комментарий (необязательно)", color = Outline) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = taskFieldColors(),
                    maxLines = 3
                )

                // Приоритет
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isPriority = !isPriority }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPriority) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (isPriority) Secondary else Outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Приоритетная задача", color = if (isPriority) Secondary else OnSurfaceVariant, fontSize = 14.sp)
                }

                // Фото
                OutlinedButton(
                    onClick = pickPhoto,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, if (photoBytes != null) Primary else Outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (photoBytes != null) Primary else Outline)
                ) {
                    Icon(Icons.Outlined.AddAPhoto, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (photoBytes != null) "Фото прикреплено" else "Добавить фото (необязательно)", modifier = Modifier.weight(1f))
                    if (photoBytes != null) {
                        Text("✕", modifier = Modifier.clickable { photoBytes = null })
                    }
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, if (dueDate != null) Primary else Outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (dueDate != null) Primary else Outline)
                ) {
                    Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(dueDate ?: "Срок (необязательно)", modifier = Modifier.weight(1f))
                    if (dueDate != null) {
                        Text("✕", modifier = Modifier.clickable { dueDate = null })
                    }
                }

                if (members.isNotEmpty()) {
                    val assigneeName = members.find { it.userId == assignedToId }?.displayName ?: "Кому (необязательно)"
                    Box {
                        OutlinedButton(
                            onClick = { showAssigneeDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, if (assignedToId != null) Primary else Outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (assignedToId != null) Primary else Outline)
                        ) {
                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(assigneeName, modifier = Modifier.weight(1f))
                        }
                        DropdownMenu(
                            expanded = showAssigneeDropdown,
                            onDismissRequest = { showAssigneeDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Не назначена", color = OnSurfaceVariant) },
                                onClick = { assignedToId = null; showAssigneeDropdown = false }
                            )
                            members.forEach { member ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            member.displayName + if (member.userId == currentUserId) " (Вы)" else "",
                                            color = OnSurface
                                        )
                                    },
                                    onClick = { assignedToId = member.userId; showAssigneeDropdown = false }
                                )
                            }
                        }
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { showRepeatDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, if (repeatType != "none") Primary else Outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (repeatType != "none") Primary else Outline)
                    ) {
                        Icon(Icons.Outlined.Repeat, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(repeatOptions.find { it.first == repeatType }?.second ?: "Не повторяется")
                    }
                    DropdownMenu(
                        expanded = showRepeatDropdown,
                        onDismissRequest = { showRepeatDropdown = false }
                    ) {
                        repeatOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = if (repeatType == value) Primary else OnSurface) },
                                onClick = { repeatType = value; showRepeatDropdown = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    onConfirm(
                        title.trim(),
                        dueDate,
                        assignedToId,
                        repeatType,
                        description.trim().takeIf { it.isNotBlank() },
                        isPriority,
                        photoBytes
                    )
                }
            }) {
                Text("Создать", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = OnSurfaceVariant) }
        }
    )
}

@Composable
private fun taskFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary,
    unfocusedBorderColor = Outline,
    focusedTextColor = OnSurface,
    unfocusedTextColor = OnSurface,
    cursorColor = Primary
)
