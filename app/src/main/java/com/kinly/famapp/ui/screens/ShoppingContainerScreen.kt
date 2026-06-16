package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kinly.famapp.features.inventory.InventoryViewModel
import com.kinly.famapp.features.products.ProductViewModel
import com.kinly.famapp.features.shopping.ShoppingViewModel
import com.kinly.famapp.features.stats.StatsViewModel
import com.kinly.famapp.ui.theme.ActivePillGradient
import com.kinly.famapp.ui.theme.GlassPillBg
import com.kinly.famapp.ui.theme.GlassPillBorder
import com.kinly.famapp.ui.theme.GlowViolet
import com.kinly.famapp.ui.theme.OnSurfaceVariant

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

    // При открытии вкладок инвентаря/статистики подтягиваем свежие данные,
    // чтобы товары, отмеченные купленными, сразу появлялись (не ждём realtime).
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            productViewModel.reload()
            inventoryViewModel.reload()
        } else if (selectedTab == 2) {
            statsViewModel.refresh()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SegmentedTabs(
            tabs = tabs,
            selectedTab = selectedTab,
            onSelect = { selectedTab = it }
        )

        when (selectedTab) {
            0 -> ShoppingScreen(viewModel = shoppingViewModel, productViewModel = productViewModel)
            1 -> InventoryScreen(viewModel = inventoryViewModel, products = productUiState.products)
            2 -> StatsScreen(viewModel = statsViewModel)
        }
    }
}

/** Стеклянный segmented-control с иконками; активная вкладка — фиолетовая «пилюля» с свечением. */
@Composable
private fun SegmentedTabs(
    tabs: List<Pair<ImageVector, String>>,
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GlassPillBg)
            .border(1.dp, GlassPillBorder, RoundedCornerShape(24.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, (icon, label) ->
            val selected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .then(
                        if (selected) Modifier.shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(19.dp),
                            spotColor = GlowViolet,
                            ambientColor = GlowViolet
                        ) else Modifier
                    )
                    .clip(RoundedCornerShape(19.dp))
                    .then(
                        if (selected) Modifier
                            .background(Brush.horizontalGradient(ActivePillGradient))
                            .background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x00FFFFFF))))
                        else Modifier
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) Color.White else OnSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
