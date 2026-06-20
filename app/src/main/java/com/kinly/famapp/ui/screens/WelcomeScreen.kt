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
import com.kinly.famapp.ui.theme.LocalAppFonts
import com.kinly.famapp.ui.theme.OnSurface
import com.kinly.famapp.ui.theme.OnSurfaceVariant
import com.kinly.famapp.ui.theme.Primary
import com.kinly.famapp.ui.theme.Secondary

@Composable
fun WelcomeScreen(
    onSignInWithGoogle: () -> Unit,
    onContinueAsGuest: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Family OS",
            color = Primary,
            fontFamily = LocalAppFonts.current.display,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Семейный органайзер",
            color = OnSurfaceVariant,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Задачи, покупки, семья — всё в одном месте",
            color = OnSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 64.dp)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (isLoading) {
            CircularProgressIndicator(color = Primary, modifier = Modifier.size(48.dp))
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Primary, Secondary))),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = onSignInWithGoogle,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Войти через Google",
                        color = Color(0xFF0B1326),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
