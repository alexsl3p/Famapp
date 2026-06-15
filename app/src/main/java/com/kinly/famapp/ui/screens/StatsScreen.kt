package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinly.famapp.features.stats.FrequentProduct
import com.kinly.famapp.features.stats.InventoryStatusRow
import com.kinly.famapp.features.stats.StatsViewModel
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 120.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Статистика", style = MaterialTheme.typography.headlineSmall, color = OnSurface)
                Text(
                    "Покупки и остатки",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )
            }
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(imageVector = Icons.Outlined.Refresh, contentDescription = "Обновить", tint = OnSurfaceVariant)
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            // Часто покупаемые
            StatSectionHeader(
                icon = Icons.Outlined.ShoppingBag,
                title = "Часто покупаемые",
                subtitle = "за 90 дней",
                color = Primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.frequentProducts.isEmpty()) {
                EmptyStatCard("Пока нет данных о покупках")
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        uiState.frequentProducts.forEachIndexed { index, product ->
                            FrequentProductRow(product, index + 1)
                            if (index < uiState.frequentProducts.size - 1) {
                                Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Требует внимания
            val needsAttention = uiState.lowStockItems
            if (needsAttention.isNotEmpty()) {
                StatSectionHeader(
                    icon = Icons.Outlined.Warning,
                    title = "Требует внимания",
                    subtitle = "${needsAttention.size} позиций",
                    color = Color(0xFFF59E0B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        needsAttention.forEachIndexed { index, item ->
                            LowStockRow(item)
                            if (index < needsAttention.size - 1) {
                                Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Каденция покупок
            StatSectionHeader(
                icon = Icons.Outlined.BarChart,
                title = "Ритм покупок",
                subtitle = "среднее время между закупками",
                color = Tertiary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.cadence.isEmpty()) {
                EmptyStatCard("Недостаточно данных для анализа")
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        uiState.cadence
                            .filter { (it.avgDaysBetween ?: 0.0) > 0 }
                            .sortedBy { it.avgDaysBetween }
                            .take(6)
                            .forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.productName, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text("${item.totalPurchases} покупок", color = OnSurfaceVariant, fontSize = 12.sp)
                                    }
                                    val days = item.avgDaysBetween?.let { "каждые ${it.toInt()} дн." } ?: "—"
                                    Text(days, color = Tertiary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                if (index < uiState.cadence.size - 1) {
                                    Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatSectionHeader(icon: ImageVector, title: String, subtitle: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = OnSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyStatCard(message: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(message, color = OnSurfaceVariant, fontSize = 14.sp)
        }
    }
}

@Composable
private fun FrequentProductRow(product: FrequentProduct, rank: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(rank.toString(), color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(product.productName, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (product.brand != null) Text(product.brand, color = OnSurfaceVariant, fontSize = 12.sp)
        }
        Text(
            "${product.purchaseCount}×",
            color = Primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun LowStockRow(item: InventoryStatusRow) {
    val statusColor = when (item.status) {
        "out" -> Color(0xFFEF4444)
        "low" -> Color(0xFFF59E0B)
        "expiring" -> Color(0xFFF97316)
        else -> OnSurfaceVariant
    }
    val statusLabel = when (item.status) {
        "out" -> "Закончилось"
        "low" -> "Заканчивается"
        "expiring" -> "Истекает"
        else -> "OK"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.productName, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (item.brand != null) Text(item.brand, color = OnSurfaceVariant, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(statusLabel, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${item.quantity.toInt()} шт.", color = OnSurfaceVariant, fontSize = 11.sp)
        }
    }
}
