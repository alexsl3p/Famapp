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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.kinly.famapp.data.models.FamilyMember
import com.kinly.famapp.data.models.Task
import com.kinly.famapp.data.models.TaskComment
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinly.famapp.features.tasks.TaskDraftViewModel
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
    val draftViewModel: TaskDraftViewModel = hiltViewModel()
    val draft by draftViewModel.draft.collectAsState()
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var deletingTask by remember { mutableStateOf<Task?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val membersMap = remember(members) { members.associate { it.userId to it.displayName } }
    val tabs = listOf(
        "Все" to TaskFilter.ALL,
        "Текущие" to TaskFilter.CURRENT,
        "Ежедневные" to TaskFilter.DAILY,
        "Долгосрочные" to TaskFilter.LONGTERM,
        "Готово" to TaskFilter.COMPLETED
    )

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
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(text = "Задачи семьи", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(
                    text = "${filteredTasks.size} задач",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0B0C0)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { (label, filter) ->
                    val isSelected = uiState.filter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0x40630ED4) else Color(0x0DFFFFFF))
                            .border(1.dp, if (isSelected) Color(0x80630ED4) else Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Primary else Color(0xFFB0B0C0),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                if (filteredTasks.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Задач нет", color = OnSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Column {
                            filteredTasks.forEachIndexed { index, task ->
                                CompactTaskRow(
                                    task = task,
                                    membersMap = membersMap,
                                    hasAttachment = task.id in uiState.attachmentTaskIds,
                                    onComplete = { viewModel.completeTask(task.id) },
                                    onUncomplete = { viewModel.uncompleteTask(task.id) },
                                    onTogglePriority = { viewModel.togglePriority(task.id, !task.isPriority) },
                                    onOpenComments = { viewModel.openComments(task.id) },
                                    onEdit = { editingTask = task },
                                    onDelete = { deletingTask = task }
                                )
                                if (index < filteredTasks.lastIndex) {
                                    HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                                }
                            }
                        }
                    }
                }

                // Кнопка добавления — как «Добавить товар» в покупках
                if (uiState.filter != TaskFilter.COMPLETED) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { draftViewModel.open() }.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(Primary, Secondary))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF0B1326), modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("Добавить задачу", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    if (draft.open) {
        AddTaskDialog(
            members = members,
            currentUserId = currentUserId,
            isEdit = false,
            title = draft.title,
            onTitleChange = draftViewModel::setTitle,
            description = draft.description,
            onDescriptionChange = draftViewModel::setDescription,
            dueDate = draft.dueDate,
            onDueDateChange = draftViewModel::setDueDate,
            assignedToId = draft.assignedTo,
            onAssignedChange = draftViewModel::setAssignee,
            taskType = draft.taskType,
            onTaskTypeChange = draftViewModel::setTaskType,
            isPriority = draft.isPriority,
            onPriorityChange = draftViewModel::setPriority,
            onDismiss = { draftViewModel.close() },
            onConfirm = { photoBytes ->
                viewModel.createTask(
                    title = draft.title.trim(),
                    assignedTo = draft.assignedTo,
                    dueDate = draft.dueDate,
                    repeatType = if (draft.taskType == "daily") "daily" else "none",
                    taskType = draft.taskType,
                    description = draft.description.trim().takeIf { it.isNotBlank() },
                    isPriority = draft.isPriority,
                    photoBytes = photoBytes
                )
                draftViewModel.close()
            }
        )
    }

    editingTask?.let { task ->
        var eTitle by rememberSaveable(task.id) { mutableStateOf(task.title) }
        var eDesc by rememberSaveable(task.id) { mutableStateOf(task.description ?: "") }
        var eDue by rememberSaveable(task.id) { mutableStateOf(task.dueDate) }
        var eAssignee by rememberSaveable(task.id) { mutableStateOf(task.assignedTo) }
        var eType by rememberSaveable(task.id) { mutableStateOf(task.taskType) }
        var ePriority by rememberSaveable(task.id) { mutableStateOf(task.isPriority) }
        AddTaskDialog(
            members = members,
            currentUserId = currentUserId,
            isEdit = true,
            title = eTitle,
            onTitleChange = { eTitle = it },
            description = eDesc,
            onDescriptionChange = { eDesc = it },
            dueDate = eDue,
            onDueDateChange = { eDue = it },
            assignedToId = eAssignee,
            onAssignedChange = { eAssignee = it },
            taskType = eType,
            onTaskTypeChange = { eType = it },
            isPriority = ePriority,
            onPriorityChange = { ePriority = it },
            onDismiss = { editingTask = null },
            onConfirm = { _ ->
                viewModel.updateTask(
                    taskId = task.id,
                    title = eTitle.trim(),
                    assignedTo = eAssignee,
                    dueDate = eDue,
                    repeatType = if (eType == "daily") "daily" else "none",
                    taskType = eType,
                    description = eDesc.trim().takeIf { it.isNotBlank() },
                    isPriority = ePriority
                )
                editingTask = null
            }
        )
    }

    deletingTask?.let { task ->
        AlertDialog(
            onDismissRequest = { deletingTask = null },
            containerColor = Color(0xFF1D2538),
            title = { Text("Удалить задачу?", color = OnSurface) },
            text = { Text("«${task.title}» будет удалена безвозвратно.", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task.id)
                    deletingTask = null
                }) {
                    Text("Удалить", color = Secondary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTask = null }) {
                    Text("Отмена", color = OnSurfaceVariant)
                }
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
            var fullscreen by remember { mutableStateOf(false) }
            Spacer(Modifier.height(6.dp))
            AsyncImage(
                model = comment.imageUrl,
                contentDescription = "Фото — нажмите, чтобы открыть",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { fullscreen = true }
            )
            if (fullscreen) {
                Dialog(
                    onDismissRequest = { fullscreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xF2000000))
                            .clickable { fullscreen = false },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = comment.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RealTaskCard(
    task: Task,
    membersMap: Map<String, String> = emptyMap(),
    currentUserId: String = "",
    hasAttachment: Boolean = false,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onTogglePriority: () -> Unit = {},
    onOpenComments: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
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
                    if (hasAttachment) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = "Есть вложение",
                            tint = Tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Редактировать",
                            tint = Outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = Outline,
                            modifier = Modifier.size(18.dp)
                        )
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
    isEdit: Boolean,
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    dueDate: String?,
    onDueDateChange: (String?) -> Unit,
    assignedToId: String?,
    onAssignedChange: (String?) -> Unit,
    taskType: String,
    onTaskTypeChange: (String) -> Unit,
    isPriority: Boolean,
    onPriorityChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (photoBytes: ByteArray?) -> Unit
) {
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAssigneeDropdown by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    val pickPhoto = rememberPhotoPicker { photoBytes = it }

    val datePickerState = rememberDatePickerState()
    val typeOptions = listOf(
        "current" to "Текущая",
        "daily" to "Ежедневная",
        "longterm" to "Долгосрочная"
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDueDateChange(datePickerState.selectedDateMillis?.let { millis ->
                        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    })
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
        title = { Text(if (isEdit) "Редактировать задачу" else "Новая задача", color = OnSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Название задачи", color = Outline) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = taskFieldColors(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Комментарий (необязательно)", color = Outline) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = taskFieldColors(),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                // Приоритет
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPriorityChange(!isPriority) }
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

                // Фото (только при создании; в режиме редактирования фото добавляются через комментарии)
                if (!isEdit) {
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
                        Text("✕", modifier = Modifier.clickable { onDueDateChange(null) })
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
                                onClick = { onAssignedChange(null); showAssigneeDropdown = false }
                            )
                            members.forEach { member ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            member.displayName + if (member.userId == currentUserId) " (Вы)" else "",
                                            color = OnSurface
                                        )
                                    },
                                    onClick = { onAssignedChange(member.userId); showAssigneeDropdown = false }
                                )
                            }
                        }
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { showTypeDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                    ) {
                        Icon(Icons.Outlined.Repeat, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Тип: " + (typeOptions.find { it.first == taskType }?.second ?: "Текущая"), modifier = Modifier.weight(1f))
                    }
                    DropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false }
                    ) {
                        typeOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = if (taskType == value) Primary else OnSurface) },
                                onClick = { onTaskTypeChange(value); showTypeDropdown = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onConfirm(photoBytes)
            }) {
                Text(if (isEdit) "Сохранить" else "Создать", color = Primary, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun CompactTaskRow(
    task: Task,
    membersMap: Map<String, String>,
    hasAttachment: Boolean,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onTogglePriority: () -> Unit,
    onOpenComments: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val meta = buildList {
        task.dueDate?.let { add(it) }
        if (hasAttachment) add("📎")
    }.joinToString("  ·  ")
    val assigneeName = task.assignedTo?.let { membersMap[it] }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Чекбокс выполнения (в стиле списка покупок)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (task.isCompleted) Primary.copy(alpha = 0.25f) else Color(0x14FFFFFF))
                .border(1.dp, if (task.isCompleted) Primary else Color(0x40FFFFFF), RoundedCornerShape(7.dp))
                .clickable { if (task.isCompleted) onUncomplete() else onComplete() },
            contentAlignment = Alignment.Center
        ) {
            if (task.isCompleted) Icon(Icons.Outlined.Check, null, tint = Primary, modifier = Modifier.size(14.dp))
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f).clickable { onOpenComments() }) {
            Text(
                text = task.title,
                color = if (task.isCompleted) Outline else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            )
            if (meta.isNotBlank()) {
                Text(meta, color = OnSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.width(6.dp))

        // Аватар исполнителя (кому назначена)
        if (assigneeName != null) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = assigneeName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(2.dp))
        }

        IconSlot(onTogglePriority) {
            Icon(
                imageVector = if (task.isPriority) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Приоритет",
                tint = if (task.isPriority) Secondary else Outline,
                modifier = Modifier.size(18.dp)
            )
        }
        IconSlot(onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Изменить", tint = Outline, modifier = Modifier.size(17.dp))
        }
        IconSlot(onDelete) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Удалить", tint = Outline, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun IconSlot(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(30.dp).clip(CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}
