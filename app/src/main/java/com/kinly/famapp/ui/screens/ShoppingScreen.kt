package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kinly.famapp.data.models.Product
import com.kinly.famapp.data.models.ShoppingItem
import com.kinly.famapp.data.models.ShoppingList
import com.kinly.famapp.features.products.BarcodeLookup
import com.kinly.famapp.features.products.ProductViewModel
import com.kinly.famapp.features.shopping.ShoppingViewModel
import com.kinly.famapp.ui.components.GlassCard
import com.kinly.famapp.ui.theme.*

@Composable
fun ShoppingScreen(viewModel: ShoppingViewModel, productViewModel: ProductViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val productUiState by productViewModel.uiState.collectAsState()
    val scanResult by productViewModel.scanResult.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }
    var showCreateList by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 120.dp)
        ) {
            ShoppingListSelector(
                lists = uiState.lists,
                currentList = uiState.currentList,
                expanded = showListMenu,
                onExpandedChange = { showListMenu = it },
                onSelect = { viewModel.selectList(it); showListMenu = false },
                onCreateNew = { showListMenu = false; showCreateList = true }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = uiState.currentList?.title ?: "Shopping List",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurface
                    )
                    Text(
                        text = "Нажмите на товар, чтобы отметить купленным",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).background(Tertiary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.LocalDining, contentDescription = null, tint = Tertiary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = uiState.currentList?.title ?: "Продукты",
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
                            text = "${uiState.items.size} ${if (uiState.items.size == 1) "item" else "items"}",
                            color = OnSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        if (uiState.items.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Список пуст. Добавьте товары!", color = OnSurfaceVariant)
                            }
                        } else {
                            uiState.items.forEachIndexed { index, item ->
                                ShoppingItemRow(
                                    item = item,
                                    onToggle = { viewModel.checkItem(item.id, !item.isChecked) }
                                )
                                if (index < uiState.items.size - 1) {
                                    Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }

                if (uiState.items.any { it.isChecked }) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0DFFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { viewModel.clearChecked() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Outlined.DeleteSweep, contentDescription = null, tint = Secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Очистить купленное", color = Secondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Scan barcode FAB (above the add FAB)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 158.dp)
                .size(56.dp)
                .background(Color(0x33272F43), CircleShape)
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clip(CircleShape)
                .clickable { showScanner = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = "Scan barcode", tint = Primary, modifier = Modifier.size(26.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .size(56.dp)
                .background(Brush.linearGradient(listOf(ShoppingPrimary, Secondary)), CircleShape)
                .clip(CircleShape)
                .clickable { showAddDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Item", tint = Color.White, modifier = Modifier.size(28.dp))
        }

        if (showScanner) {
            ScannerScreen(
                onClose = { showScanner = false },
                onBarcodeDetected = { barcode ->
                    showScanner = false
                    productViewModel.onBarcodeScanned(barcode)
                }
            )
        }
    }

    scanResult?.let { result ->
        ScanResultDialog(
            result = result,
            onDismiss = { productViewModel.clearScanResult() },
            onAddExisting = { product ->
                viewModel.addItem(product.name, null, productId = product.id)
                productViewModel.clearScanResult()
            },
            onCreateAndAdd = { name, barcode, brand, packageSize, imageUrl, source ->
                productViewModel.createScannedProduct(
                    name = name,
                    barcode = barcode,
                    brand = brand,
                    packageSize = packageSize,
                    imageUrl = imageUrl,
                    source = source
                ) { product ->
                    viewModel.addItem(product.name, null, productId = product.id)
                }
                productViewModel.clearScanResult()
            }
        )
    }

    if (showCreateList) {
        CreateListDialog(
            onDismiss = { showCreateList = false },
            onConfirm = { name ->
                viewModel.createList(name)
                showCreateList = false
            }
        )
    }

    if (showAddDialog) {
        AddShoppingItemDialog(
            searchResults = productUiState.searchResults,
            onSearchQueryChange = { productViewModel.setSearchQuery(it) },
            onDismiss = {
                showAddDialog = false
                productViewModel.setSearchQuery("")
            },
            onConfirm = { title, quantity, productId ->
                viewModel.addItem(title, quantity.ifBlank { null }, productId = productId)
                productViewModel.setSearchQuery("")
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ShoppingItemRow(item: ShoppingItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (item.isChecked) ShoppingPrimary.copy(alpha = 0.2f) else Color(0x1AFFFFFF))
                .border(1.dp, if (item.isChecked) ShoppingPrimary else Color(0x33FFFFFF), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (item.isChecked) {
                Icon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = ShoppingPrimary, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = if (item.isChecked) Outline else OnSurface,
                fontSize = 17.sp,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
            )
            if (item.productId != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = Primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "из каталога",
                        color = Primary.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (item.quantity != null) {
            Text(
                text = "${item.quantity}${if (item.unit != null) " ${item.unit}" else ""}",
                color = if (item.isChecked) OutlineVariant else Outline,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
            )
        }
    }
}

@Composable
fun AddShoppingItemDialog(
    searchResults: List<Product>,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (title: String, quantity: String, productId: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showSuggestions by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1D2538),
        title = { Text("Добавить товар", color = OnSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { value ->
                            title = value
                            selectedProduct = null
                            onSearchQueryChange(value)
                            showSuggestions = value.length >= 2
                        },
                        label = { Text("Название", color = Outline) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (selectedProduct != null) Primary else Primary,
                            unfocusedBorderColor = if (selectedProduct != null) Primary.copy(alpha = 0.5f) else Outline,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = Primary
                        ),
                        singleLine = true,
                        trailingIcon = if (selectedProduct != null) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory2,
                                    contentDescription = "Linked to product",
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )

                    if (showSuggestions && searchResults.isNotEmpty() && selectedProduct == null) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                        ) {
                            LazyColumn {
                                items(searchResults.take(4)) { product ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedProduct = product
                                                title = product.name
                                                showSuggestions = false
                                                onSearchQueryChange("")
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Inventory2,
                                            contentDescription = null,
                                            tint = Primary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(product.name, color = OnSurface, fontSize = 14.sp)
                                            if (product.brand != null) {
                                                Text(product.brand, color = OnSurfaceVariant, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Количество (необязательно)", color = Outline) },
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

                if (selectedProduct != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.Inventory2, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Привязан к каталогу",
                            color = Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Убрать",
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { selectedProduct = null }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onConfirm(title.trim(), quantity.trim(), selectedProduct?.id)
            }) {
                Text("Добавить", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
fun ShoppingListSelector(
    lists: List<ShoppingList>,
    currentList: ShoppingList?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1AFFFFFF))
                .clickable { onExpandedChange(true) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentList?.title ?: "Списки",
                color = OnSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.ExpandMore, contentDescription = "Выбрать список", tint = Primary, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            lists.forEach { list ->
                DropdownMenuItem(
                    text = { Text(list.title, color = if (list.id == currentList?.id) Primary else OnSurface) },
                    onClick = { onSelect(list.id) }
                )
            }
            HorizontalDivider(color = Color(0x1AFFFFFF))
            DropdownMenuItem(
                text = { Text("Создать список", color = Primary) },
                leadingIcon = { Icon(Icons.Outlined.PlaylistAdd, null, tint = Primary) },
                onClick = onCreateNew
            )
        }
    }
}

@Composable
fun CreateListDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1D2538),
        title = { Text("Новый список", color = OnSurface) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название списка", color = Outline) },
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
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("Создать", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = OnSurfaceVariant) }
        }
    )
}

@Composable
fun ScanResultDialog(
    result: BarcodeLookup,
    onDismiss: () -> Unit,
    onAddExisting: (Product) -> Unit,
    onCreateAndAdd: (
        name: String,
        barcode: String,
        brand: String?,
        packageSize: String?,
        imageUrl: String?,
        source: String
    ) -> Unit
) {
    // Loading показываем без кнопок — это просто индикатор
    if (result is BarcodeLookup.Loading) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF1D2538),
            title = { Text("Ищем товар…", color = OnSurface) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Проверяем каталог и Open Food Facts", color = OnSurfaceVariant, fontSize = 13.sp)
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Отмена", color = OnSurfaceVariant) }
            }
        )
        return
    }

    // Для остальных кейсов — редактируемое имя + действие
    val initialName = when (result) {
        is BarcodeLookup.ExistingProduct -> result.product.name
        is BarcodeLookup.Suggestion -> result.name
        else -> ""
    }
    var name by remember(result) { mutableStateOf(initialName) }

    val title = when (result) {
        is BarcodeLookup.ExistingProduct -> "Товар найден в каталоге"
        is BarcodeLookup.Suggestion -> "Найдено в Open Food Facts"
        is BarcodeLookup.NotFound -> "Товар не найден"
        else -> ""
    }

    val imageUrl = (result as? BarcodeLookup.Suggestion)?.imageUrl
    val brand = (result as? BarcodeLookup.Suggestion)?.brand
    val packageSize = (result as? BarcodeLookup.Suggestion)?.packageSize

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1D2538),
        title = { Text(title, color = OnSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                if (result is BarcodeLookup.NotFound) {
                    Text(
                        "Введите название, чтобы добавить товар в каталог.",
                        color = OnSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                if (result is BarcodeLookup.ExistingProduct) {
                    Text(
                        if (brand != null) "${result.product.name} ($brand)" else result.product.name,
                        color = OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название", color = Outline) },
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
                if (brand != null || packageSize != null) {
                    Text(
                        listOfNotNull(brand, packageSize).joinToString(" · "),
                        color = OnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            when (result) {
                is BarcodeLookup.ExistingProduct -> {
                    TextButton(onClick = { onAddExisting(result.product) }) {
                        Text("Добавить в список", color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                is BarcodeLookup.Suggestion -> {
                    TextButton(
                        enabled = name.isNotBlank(),
                        onClick = {
                            onCreateAndAdd(name.trim(), result.barcode, result.brand, result.packageSize, result.imageUrl, "openfoodfacts")
                        }
                    ) {
                        Text("В каталог и список", color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                is BarcodeLookup.NotFound -> {
                    TextButton(
                        enabled = name.isNotBlank(),
                        onClick = {
                            onCreateAndAdd(name.trim(), result.barcode, null, null, null, "scan_manual")
                        }
                    ) {
                        Text("Создать и добавить", color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = OnSurfaceVariant) }
        }
    )
}
