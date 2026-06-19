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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory2
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
import com.kinly.famapp.data.models.Product
import com.kinly.famapp.features.products.ProductViewModel
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.components.emojiForItem
import com.kinly.famapp.ui.theme.*

@Composable
fun CatalogScreen(viewModel: ProductViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Product?>(null) }

    val sorted = remember(uiState.products) { uiState.products.sortedBy { it.name.lowercase() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 120.dp)
    ) {
        Text(text = "Каталог", style = MaterialTheme.typography.headlineSmall, color = OnSurface)
        Text(
            text = "Все продукты семьи — из них берутся подсказки",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(AccentGradient))
                .clickable { showAddDialog = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF0B1326), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Добавить продукт", color = Color(0xFF0B1326), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(20.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (sorted.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Каталог пуст", color = OnSurfaceVariant)
                }
            }
        } else {
            Text(
                text = "Продуктов: ${sorted.size}",
                color = OnSurfaceVariant, fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    sorted.forEachIndexed { index, product ->
                        val emoji = emojiForItem(product.name)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (emoji != null) {
                                Text(emoji, fontSize = 17.sp)
                                Spacer(Modifier.width(10.dp))
                            } else {
                                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Primary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                if (!product.brand.isNullOrBlank()) {
                                    Text(product.brand, color = OnSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            Box(
                                modifier = Modifier.size(30.dp).clip(CircleShape).clickable { deleting = product },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Удалить", tint = Outline, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (index < sorted.lastIndex) {
                            Divider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CatalogAddDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.createProduct(name = name)
                showAddDialog = false
            }
        )
    }

    deleting?.let { product ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = Color(0xFF1D2538),
            title = { Text("Удалить из каталога?", color = OnSurface) },
            text = { Text("«${product.name}» удалится из каталога и из инвентаря. В уже созданных списках товар останется, но без привязки.", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteProduct(product.id); deleting = null }) {
                    Text("Удалить", color = Secondary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Отмена", color = OnSurfaceVariant) }
            }
        )
    }
}

@Composable
private fun CatalogAddDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1D2538),
        title = { Text("Новый продукт", color = OnSurface) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название", color = Outline) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Outline,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    cursorColor = Primary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("Добавить", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = OnSurfaceVariant) } }
    )
}
