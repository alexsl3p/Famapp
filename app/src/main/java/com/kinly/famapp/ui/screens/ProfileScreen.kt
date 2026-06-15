package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kinly.famapp.data.models.Profile
import com.kinly.famapp.features.auth.AuthViewModel
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

@Composable
fun ProfileScreen(
    profile: Profile,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val isSaving by authViewModel.isSavingProfile.collectAsState()
    var name by remember(profile.id) { mutableStateOf(profile.fullName ?: "") }
    val pickPhoto = rememberPhotoPicker { bytes -> authViewModel.updateAvatar(bytes) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад", tint = OnSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text("Профиль", style = MaterialTheme.typography.headlineSmall, color = OnSurface)
        }

        // Avatar
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.3f))
                        .border(2.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { pickPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.avatarUrl != null) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "Аватар",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(profile.initial, color = Primary, fontWeight = FontWeight.Bold, fontSize = 40.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Primary)
                        .clickable { pickPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Сменить фото", tint = Color(0xFF0B1326), modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Имя", color = OnSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = Primary
                    )
                )
                if (profile.email != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Эл. почта", color = OnSurfaceVariant, fontSize = 13.sp)
                    Text(profile.email, color = OnSurface, fontSize = 15.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (name.isNotBlank()) Primary else Outline.copy(alpha = 0.3f))
                .clickable(enabled = name.isNotBlank() && !isSaving) {
                    authViewModel.updateName(name)
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color(0xFF0B1326), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Сохранить", color = Color(0xFF0B1326), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}
