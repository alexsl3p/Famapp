package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

private enum class OnboardingMode { SELECT, CREATE, JOIN }

@Composable
fun OnboardingFamilyScreen(
    onCreateFamily: (String) -> Unit,
    onJoinFamily: (String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var mode by remember { mutableStateOf(OnboardingMode.SELECT) }
    var familyName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Добро пожаловать!",
            color = OnSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Создайте семью или присоединитесь по коду приглашения",
            color = OnSurfaceVariant,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        if (error != null) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(
                    text = error,
                    color = Error,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        when (mode) {
            OnboardingMode.SELECT -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(Primary, Secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { mode = OnboardingMode.CREATE },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text("Создать семью", color = Color(0xFF0B1326), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { mode = OnboardingMode.JOIN },
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                    ) {
                        Text("Вступить по коду", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }

            OnboardingMode.CREATE -> {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Название семьи", color = OnSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedTextField(
                            value = familyName,
                            onValueChange = { familyName = it },
                            placeholder = { Text("Например: Семья Ивановых", color = Outline) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Outline,
                                focusedTextColor = OnSurface,
                                unfocusedTextColor = OnSurface,
                                cursorColor = Primary
                            ),
                            singleLine = true
                        )
                    }
                }
                if (isLoading) {
                    CircularProgressIndicator(color = Primary)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (familyName.isNotBlank())
                                    Brush.horizontalGradient(listOf(Primary, Secondary))
                                else
                                    Brush.horizontalGradient(listOf(Outline, Outline))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { if (familyName.isNotBlank()) onCreateFamily(familyName.trim()) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("Создать", color = Color(0xFF0B1326), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { mode = OnboardingMode.SELECT }) {
                    Text("Назад", color = OnSurfaceVariant)
                }
            }

            OnboardingMode.JOIN -> {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Код приглашения", color = OnSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedTextField(
                            value = inviteCode,
                            onValueChange = { if (it.length <= 8) inviteCode = it.uppercase() },
                            placeholder = { Text("A7K9Q2PM", color = Outline) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Outline,
                                focusedTextColor = OnSurface,
                                unfocusedTextColor = OnSurface,
                                cursorColor = Primary
                            ),
                            singleLine = true
                        )
                    }
                }
                if (isLoading) {
                    CircularProgressIndicator(color = Primary)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(Primary, Secondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { if (inviteCode.length == 8) onJoinFamily(inviteCode) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("Вступить", color = Color(0xFF0B1326), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { mode = OnboardingMode.SELECT }) {
                    Text("Назад", color = OnSurfaceVariant)
                }
            }
        }
    }
}
