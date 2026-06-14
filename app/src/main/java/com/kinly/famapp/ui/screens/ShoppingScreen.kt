package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

data class ShoppingCategory(
    val name: String,
    val icon: ImageVector,
    val iconColor: Color,
    val items: List<ShoppingItem>
)

data class ShoppingItem(
    val name: String,
    val quantity: String,
    val done: Boolean = false
)

@Composable
fun ShoppingScreen() {
    val categories = listOf(
        ShoppingCategory(
            name = "Grocery",
            icon = Icons.Outlined.LocalDining,
            iconColor = Tertiary,
            items = listOf(
                ShoppingItem("Organic Milk", "2 Gal"),
                ShoppingItem("Eggs (Free Range)", "1 Doz", done = true),
                ShoppingItem("Avocados", "4"),
                ShoppingItem("Sourdough Bread", "1 Loaf")
            )
        ),
        ShoppingCategory(
            name = "Home",
            icon = Icons.Outlined.Chair,
            iconColor = Secondary,
            items = listOf(
                ShoppingItem("Paper Towels", "12 Pack"),
                ShoppingItem("Dish Soap", "1 Btl")
            )
        ),
        ShoppingCategory(
            name = "Kids",
            icon = Icons.Outlined.Toys,
            iconColor = Primary,
            items = listOf(
                ShoppingItem("Construction Paper", "1 Pack")
            )
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 120.dp)
        ) {
            // Header
            Text(
                text = "Shopping List",
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Tap items to mark them as done.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            categories.forEach { category ->
                ShoppingCategorySection(category = category)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .size(56.dp)
                .background(
                    Brush.linearGradient(listOf(ShoppingPrimary, Secondary)),
                    CircleShape
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Item",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun ShoppingCategorySection(category: ShoppingCategory) {
    val itemStates = remember { category.items.map { mutableStateOf(it.done) } }

    Column {
        // Category header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(category.iconColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                color = OnSurface,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .background(Color(0x33272F43), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${category.items.size} ${if (category.items.size == 1) "item" else "items"}",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Items container
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                category.items.forEachIndexed { index, item ->
                    val checked = itemStates[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom checkbox
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (checked.value) ShoppingPrimary.copy(alpha = 0.2f)
                                    else Color(0x1AFFFFFF)
                                )
                                .border(
                                    1.dp,
                                    if (checked.value) ShoppingPrimary else Color(0x33FFFFFF),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked.value) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = ShoppingPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = item.name,
                            color = if (checked.value) Outline else OnSurface,
                            fontSize = 17.sp,
                            textDecoration = if (checked.value) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = item.quantity,
                            color = if (checked.value) OutlineVariant else Outline,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            textDecoration = if (checked.value) TextDecoration.LineThrough else null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Toggle on tap
                        Switch(
                            checked = checked.value,
                            onCheckedChange = { checked.value = it },
                            modifier = Modifier.size(0.dp),
                            colors = SwitchDefaults.colors()
                        )
                    }

                    // Make row tappable to toggle
                    LaunchedEffect(Unit) {}

                    if (index < category.items.size - 1) {
                        Divider(
                            color = Color(0x1AFFFFFF),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
