package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kinly.famapp.features.inventory.InventoryViewModel
import com.kinly.famapp.features.products.ProductViewModel
import com.kinly.famapp.features.shopping.ShoppingViewModel
import com.kinly.famapp.features.stats.StatsViewModel
import com.kinly.famapp.ui.theme.Primary
import com.kinly.famapp.ui.theme.OnSurfaceVariant
import androidx.compose.ui.graphics.Color

@Composable
fun ShoppingContainerScreen(
    shoppingViewModel: ShoppingViewModel,
    productViewModel: ProductViewModel,
    inventoryViewModel: InventoryViewModel,
    statsViewModel: StatsViewModel
) {
    val productUiState by productViewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Icons.Outlined.FormatListBulleted to "Список",
        Icons.Outlined.Inventory2 to "Инвентарь",
        Icons.Outlined.BarChart to "Статистика"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1AFFFFFF)),
        ) {
            tabs.forEachIndexed { index, (icon, label) ->
                val selected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Primary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) Primary else OnSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> ShoppingScreen(viewModel = shoppingViewModel, productViewModel = productViewModel)
            1 -> InventoryScreen(viewModel = inventoryViewModel, products = productUiState.products)
            2 -> StatsScreen(viewModel = statsViewModel)
        }
    }
}
