package com.kinly.famapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.kinly.famapp.data.models.Message
import com.kinly.famapp.features.chat.AudioRecorder
import com.kinly.famapp.features.chat.ChatViewModel
import com.kinly.famapp.ui.theme.*

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    familyId: String,
    meId: String,
    otherId: String?,
    otherName: String,
    onBack: () -> Unit
) {
    val isGroup = otherId == null
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(familyId, otherId) { viewModel.start(familyId, meId, otherId) }
    // Первое открытие — мгновенно к последнему сообщению; новые — с плавной прокруткой.
    var firstScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            if (firstScrollDone) listState.animateScrollToItem(state.messages.size - 1)
            else { listState.scrollToItem(state.messages.size - 1); firstScrollDone = true }
        }
    }
    // Клавиатура открылась — держим последнее сообщение на виду.
    val imeVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    LaunchedEffect(imeVisible) {
        if (imeVisible && state.messages.isNotEmpty()) listState.scrollToItem(state.messages.size - 1)
    }

    var input by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    val haptic = com.kinly.famapp.ui.components.rememberHaptic()
    val recorder = remember { AudioRecorder(context) }
    val pickPhoto = rememberPhotoPicker { bytes -> viewModel.sendImage(bytes) }
    var fullscreen by remember { mutableStateOf<String?>(null) }

    // Разрешение на микрофон: спрашиваем при первом тапе, после выдачи сразу начинаем запись.
    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && recorder.start()) recording = true
    }
    val startRecording: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (recorder.start()) recording = true
        } else {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    // Уходим с экрана во время записи — не бросаем рекордер включённым.
    DisposableEffect(Unit) { onDispose { recorder.cancel() } }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад", tint = OnSurface)
            }
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(otherName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Text(otherName, color = OnSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = Color(0x14FFFFFF))

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(state.messages, key = { it.id }) { m ->
                val mine = viewModel.isMine(m)
                MessageBubble(
                    m = m,
                    mine = mine,
                    senderLabel = if (isGroup && !mine) viewModel.senderName(m) else null,
                    onImageClick = { fullscreen = it }
                )
            }
        }

        // Отправка фото/голосового — тонкий индикатор над полем ввода
        if (state.isSending) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Primary,
                trackColor = Color.Transparent
            )
        }

        // Input
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = pickPhoto, enabled = !state.isSending) {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Фото", tint = Primary)
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(if (recording) "Запись…" else "Сообщение", color = Outline) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary, unfocusedBorderColor = Outline,
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, cursorColor = Primary
                ),
                enabled = !recording
            )
            Spacer(Modifier.width(6.dp))
            if (input.isBlank()) {
                // Микрофон: тап — начать запись, ещё тап — отправить
                val micBg = if (recording) Color(0xFFFF4D6D) else Color(0x1AFFFFFF)
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(micBg)
                        .clickable {
                            if (recording) {
                                haptic(com.kinly.famapp.ui.components.Haptic.Confirm)
                                recording = false
                                val bytes = recorder.stop()
                                if (bytes != null) viewModel.sendAudio(bytes)
                            } else {
                                haptic(com.kinly.famapp.ui.components.Haptic.Tick)
                                startRecording()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (recording) "Отправить голосовое" else "Записать голосовое",
                        tint = if (recording) Color.White else Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(AccentGradient))
                        .clickable {
                            haptic(com.kinly.famapp.ui.components.Haptic.Confirm)
                            viewModel.sendText(input); input = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Отправить", tint = Color(0xFF0B1326), modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    fullscreen?.let { url ->
        Dialog(onDismissRequest = { fullscreen = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xF2000000)).clickable { fullscreen = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MessageBubble(m: Message, mine: Boolean, senderLabel: String? = null, onImageClick: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (mine) Brush.linearGradient(AccentGradient)
                    else Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x22FFFFFF)))
                )
                .padding(if (m.imageUrl != null) 4.dp else 10.dp)
        ) {
            val textColor = if (mine) Color(0xFF0B1326) else OnSurface
            if (senderLabel != null) {
                Text(
                    senderLabel,
                    color = Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = if (m.imageUrl != null) 6.dp else 0.dp, vertical = if (m.imageUrl != null) 2.dp else 0.dp)
                )
            }
            if (m.imageUrl != null) {
                AsyncImage(
                    model = m.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.widthIn(max = 240.dp).heightIn(max = 240.dp).clip(RoundedCornerShape(12.dp)).clickable { onImageClick(m.imageUrl) }
                )
            }
            if (m.audioUrl != null) {
                AudioPlayer(url = m.audioUrl, tint = textColor)
            }
            if (!m.body.isNullOrBlank()) {
                if (m.imageUrl != null) Spacer(Modifier.height(4.dp))
                Text(m.body, color = textColor, fontSize = 15.sp, modifier = Modifier.padding(horizontal = if (m.imageUrl != null) 6.dp else 0.dp, vertical = if (m.imageUrl != null) 4.dp else 0.dp))
            }
        }
    }
}

@Composable
private fun AudioPlayer(url: String, tint: Color) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    val player = remember { MediaPlayer() }
    DisposableEffect(Unit) { onDispose { runCatching { player.release() } } }

    Row(
        modifier = Modifier
            .clickable {
                runCatching {
                    if (playing) {
                        // Кнопка «стоп» — останавливаем и начинаем сначала при следующем тапе.
                        player.stop(); playing = false
                    } else {
                        player.reset()
                        player.setDataSource(context, android.net.Uri.parse(url))
                        player.setOnCompletionListener { playing = false }
                        player.prepare()
                        player.start()
                        playing = true
                    }
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = "Воспроизвести",
            tint = tint,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("Голосовое", color = tint, fontSize = 14.sp)
    }
}
