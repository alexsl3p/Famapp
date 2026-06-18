package com.kinly.famapp.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.kinly.famapp.ui.theme.AccentGradient
import com.kinly.famapp.ui.theme.OnSurface
import com.kinly.famapp.ui.theme.OnSurfaceVariant
import com.kinly.famapp.ui.theme.Outline
import com.kinly.famapp.ui.theme.Primary
import com.kinly.famapp.ui.theme.Secondary

/**
 * Стильное окно диктовки в стиле приложения (вместо системного окна Google).
 * Использует SpeechRecognizer напрямую: пульсирующий микрофон, живая «дорожка»
 * громкости и распознанный текст в реальном времени.
 */
@Composable
fun VoiceDictationDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    var partial by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rmsLevel by remember { mutableFloatStateOf(0f) }

    val recognizer = remember { if (available) SpeechRecognizer.createSpeechRecognizer(context) else null }

    fun startListening() {
        if (recognizer == null) return
        error = null
        partial = ""
        listening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { recognizer.startListening(intent) }
            .onFailure { error = "Не удалось запустить распознавание" }
    }

    DisposableEffect(recognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() { listening = true }
            override fun onRmsChanged(rmsdB: Float) {
                rmsLevel = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(e: Int) {
                listening = false
                error = when (e) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Не расслышал. Попробуйте ещё раз."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Нет сети для распознавания."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет доступа к микрофону."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознаватель занят, повторите."
                    else -> "Ошибка распознавания. Повторите."
                }
            }
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                listening = false
                if (!text.isNullOrEmpty()) {
                    onResult(text)
                    onDismiss()
                } else {
                    error = "Не расслышал. Попробуйте ещё раз."
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.let { if (it.isNotBlank()) partial = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        recognizer?.setRecognitionListener(listener)
        onDispose {
            runCatching { recognizer?.destroy() }
        }
    }

    // Старт: запрос разрешения или сразу слушаем.
    LaunchedEffect(hasPermission, available) {
        when {
            !available -> error = "Голосовой ввод недоступен на устройстве"
            hasPermission -> startListening()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Dialog(
        onDismissRequest = {
            runCatching { recognizer?.cancel() }
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF21A1430))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Кнопка закрытия
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
                    .clickable {
                        runCatching { recognizer?.cancel() }
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Закрыть", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (error != null) "Упс" else if (listening) "Слушаю…" else "Готовлюсь…",
                    color = OnSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(28.dp))

                PulsingMic(active = listening && error == null, level = rmsLevel)

                Spacer(Modifier.height(28.dp))

                if (error != null) {
                    Text(error!!, color = Secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(AccentGradient))
                            .clickable { startListening() }
                            .padding(horizontal = 28.dp, vertical = 12.dp)
                    ) {
                        Text("Повторить", color = Color(0xFF0B1326), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                } else {
                    Text(
                        text = partial.ifBlank { "Например: «добавь молоко, хлеб, яйца»\nили «задача вынести мусор»" },
                        color = if (partial.isBlank()) Outline else OnSurface,
                        fontSize = if (partial.isBlank()) 13.sp else 17.sp,
                        fontWeight = if (partial.isBlank()) FontWeight.Normal else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Нажмите «Готово», когда закончите",
                        color = Outline,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                            .clickable { runCatching { recognizer?.stopListening() } }
                            .padding(horizontal = 28.dp, vertical = 12.dp)
                    ) {
                        Text("Готово", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

/** Микрофон с пульсирующими кольцами; интенсивность колец зависит от громкости голоса. */
@Composable
private fun PulsingMic(active: Boolean, level: Float) {
    val infinite = rememberInfiniteTransition(label = "mic")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "pulse"
    )
    // Масштаб ядра реагирует на громкость.
    val coreScale by animateFloatAsState(
        targetValue = if (active) 1f + level * 0.25f else 1f,
        label = "coreScale"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
        if (active) {
            // Два расходящихся кольца
            Ring(progress = pulse, baseLevel = level)
            Ring(progress = (pulse + 0.5f) % 1f, baseLevel = level)
        }
        Box(
            modifier = Modifier
                .size(92.dp)
                .scale(coreScale)
                .clip(CircleShape)
                .background(Brush.linearGradient(AccentGradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = null,
                tint = Color(0xFF0B1326),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun Ring(progress: Float, baseLevel: Float) {
    val scale = 1f + progress * (0.7f + baseLevel * 0.6f)
    val fade = (1f - progress) * (0.35f + baseLevel * 0.35f)
    Box(
        modifier = Modifier
            .size(92.dp)
            .scale(scale)
            .alpha(fade)
            .clip(CircleShape)
            .background(Primary.copy(alpha = 0.5f))
    )
}
