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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinly.famapp.data.models.InventoryItem
import com.kinly.famapp.data.models.InventoryStatus
import com.kinly.famapp.data.models.Product
import com.kinly.famapp.features.inventory.InventoryViewModel
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    products: List<Product>
) {
    val uiState by viewModel.uiState.collectAsState()
    val productsMap = remember(products) { products.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 120.dp)
    ) {
        Text(
            text = "Инвентарь",
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurface
        )
        Text(
            text = "Текущие остатки дома",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.items.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Инвентарь пуст",
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Отмечайте покупки в списке\nчтобы товары появились здесь",
                            color = OnSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            val grouped = uiState.items.groupBy { it.status }

            listOf(InventoryStatus.OUT, InventoryStatus.LOW, InventoryStatus.EXPIRING, InventoryStatus.OK).forEach { status ->
                val group = grouped[status]
                if (!group.isNullOrEmpty()) {
                    InventoryStatusSection(
                        status = status,
                        items = group,
                        productsMap = productsMap,
                        onQuantityChange = { itemId, qty -> viewModel.updateQuantity(itemId, qty) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun InventoryStatusSection(
    status: InventoryStatus,
    items: List<InventoryItem>,
    productsMap: Map<String, Product>,
    onQuantityChange: (String, Double) -> Unit
) {
    val accentColor = when (status) {
        InventoryStatus.OUT -> Color(0xFFEF4444)
        InventoryStatus.LOW -> Color(0xFFF59E0B)
        InventoryStatus.EXPIRING -> Color(0xFFF97316)
        InventoryStatus.OK -> Color(0xFF22C55E)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(accentColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = status.label,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${items.size}",
            color = OnSurfaceVariant,
            fontSize = 13.sp
        )
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            items.forEachIndexed { index, item ->
                val product = productsMap[item.productId]
                InventoryItemRow(
                    item = item,
                    productName = product?.name ?: "Продукт",
                    productBrand = product?.brand,
                    statusColor = accentColor,
                    onQuantityChange = onQuantityChange
                )
                if (index < items.size - 1) {
                    Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun InventoryItemRow(
    item: InventoryItem,
    productName: String,
    productBrand: String?,
    statusColor: Color,
    onQuantityChange: (String, Double) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(statusColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(productName, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (productBrand != null) {
                Text(productBrand, color = OnSurfaceVariant, fontSize = 12.sp)
            }
            if (item.minQuantity != null) {
                Text(
                    text = "мин: ${item.minQuantity.toInt()}",
                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable {
                        val newQty = (item.quantity - 1).coerceAtLeast(0.0)
                        onQuantityChange(item.id, newQty)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Outlined.Remove, contentDescription = "Уменьшить", tint = OnSurfaceVariant, modifier = Modifier.size(14.dp))
            }

            Text(
                text = if (item.quantity == item.quantity.toLong().toDouble())
                    item.quantity.toLong().toString()
                else
                    "%.1f".format(item.quantity),
                color = OnSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.widthIn(min = 28.dp),
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.15f))
                    .border(1.dp, Primary.copy(alpha = 0.3f), CircleShape)
                    .clickable { onQuantityChange(item.id, item.quantity + 1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Увеличить", tint = Primary, modifier = Modifier.size(14.dp))
            }
        }
    }
}
