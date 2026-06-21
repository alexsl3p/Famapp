package com.kinly.famapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.kinly.famapp.ui.components.ScrollListWithStickyFooter
import com.kinly.famapp.ui.components.emojiForItem
import com.kinly.famapp.ui.theme.*

/** Русское склонение слова «товар» по числу. */
private fun pluralItems(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "товаров"
        mod10 == 1 -> "товар"
        mod10 in 2..4 -> "товара"
        else -> "товаров"
    }
}

@Composable
fun ShoppingScreen(viewModel: ShoppingViewModel, productViewModel: ProductViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val productState by productViewModel.uiState.collectAsState()
    var showCreateList by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }
    var deletingList by remember { mutableStateOf<ShoppingList?>(null) }

    val currentList = uiState.currentList
    val items = currentList?.let { uiState.itemsByList[it.id] } ?: emptyList()
    val sorted = remember(items) { items.sortedBy { it.isChecked } }
    val activeCount = items.count { !it.isChecked }
    val focusManager = LocalFocusManager.current
    val addSignal by viewModel.addSignal.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Тап в любом пустом месте снимает фокус → черновик товара сохраняется.
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        ScrollListWithStickyFooter(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
            footer = {
                // Строка добавления товара: под списком, пока он влезает; дальше — прижата над навигацией.
                if (currentList != null) {
                    InlineAddRow(
                        products = productState.products,
                        startSignal = addSignal,
                        onAdd = { title, qty, productId ->
                            viewModel.addItem(currentList.id, title, qty, productId = productId)
                        }
                    )
                }
            }
        ) {
        // Выбор списка вынесен в заголовок карточки ниже (тап по названию + стрелка).

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (currentList == null) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Списков нет. Создайте первый список!", color = OnSurfaceVariant)
                }
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    // Заголовок списка — он же переключатель списков (тап → дропдаун)
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showListMenu = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Brush.linearGradient(AccentGradient), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.LocalDining, contentDescription = null, tint = Color(0xFF0B1326), modifier = Modifier.size(17.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        currentList.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurface
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        if (showListMenu) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                        contentDescription = "Сменить список",
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = if (activeCount == 0) "всё куплено" else "$activeCount ${pluralItems(activeCount)} осталось",
                                    color = OnSurfaceVariant,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                        DropdownMenu(expanded = showListMenu, onDismissRequest = { showListMenu = false }) {
                            uiState.lists.forEach { list ->
                                DropdownMenuItem(
                                    text = { Text(list.title, color = if (list.id == currentList.id) Primary else OnSurface) },
                                    onClick = { viewModel.selectList(list.id); showListMenu = false },
                                    trailingIcon = {
                                        IconButton(onClick = { showListMenu = false; deletingList = list }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Удалить список", tint = Outline, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                            }
                            HorizontalDivider(color = Color(0x1AFFFFFF))
                            DropdownMenuItem(
                                text = { Text("Создать список", color = Primary) },
                                leadingIcon = { Icon(Icons.Outlined.PlaylistAdd, null, tint = Primary) },
                                onClick = { showListMenu = false; showCreateList = true }
                            )
                        }
                    }

                    Divider(color = Color(0x1AFFFFFF))

                    if (sorted.isEmpty()) {
                        Text(
                            "Список пуст",
                            color = OnSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    } else {
                        sorted.forEachIndexed { index, item ->
                            ShoppingItemRow(
                                item = item,
                                onToggle = { viewModel.checkItem(item.id, !item.isChecked) },
                                onQuantityChange = { viewModel.updateItemQuantity(item.id, it) },
                                onDelete = { viewModel.deleteItem(item.id) }
                            )
                            if (index < sorted.size - 1) {
                                Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                            }
                        }
                    }

                    if (items.any { it.isChecked }) {
                        Divider(color = Color(0x1AFFFFFF))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.clearChecked(currentList.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = Secondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Очистить купленное", color = Secondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        }
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

    deletingList?.let { list ->
        AlertDialog(
            onDismissRequest = { deletingList = null },
            containerColor = Color(0xFF1D2538),
            title = { Text("Удалить список?", color = OnSurface) },
            text = { Text("«${list.title}» и все его товары будут удалены безвозвратно.", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteList(list.id)
                    deletingList = null
                }) {
                    Text("Удалить", color = Secondary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingList = null }) {
                    Text("Отмена", color = OnSurfaceVariant)
                }
            }
        )
    }
}

/**
 * Добавление товара как в Google Keep: по клику на «Добавить товар» сразу появляется
 * новая строка в том же виде, что и товары (чекбокс + поле ввода названия + счётчик).
 * Подтверждение — Enter/Done: товар добавляется и поле очищается для следующего.
 */
@Composable
private fun InlineAddRow(
    products: List<Product>,
    startSignal: Int = 0,
    onAdd: (String, String?, String?) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableIntStateOf(1) }
    var hasFocused by remember { mutableStateOf(false) }
    var suppressSave by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Внешний сигнал (кнопка + из виджета) открывает строку добавления.
    LaunchedEffect(startSignal) {
        if (startSignal > 0) { hasFocused = false; adding = true }
    }

    // Подсказки из каталога: совпадения по названию (сначала те, что начинаются с введённого).
    val suggestions = remember(name, products) {
        val q = name.trim()
        if (q.isEmpty()) emptyList()
        else products.asSequence()
            .filter { it.name.contains(q, ignoreCase = true) && !it.name.equals(q, ignoreCase = true) }
            .sortedByDescending { it.name.startsWith(q, ignoreCase = true) }
            .distinctBy { it.name.lowercase() }
            .take(6)
            .toList()
    }

    if (!adding) {
        // Компактный «+» по центру
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 4.dp)) {
            Spacer(Modifier.weight(1.3f))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(AccentGradient))
                    .clickable { hasFocused = false; adding = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить товар", tint = Color(0xFF0B1326), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.weight(1.3f))
        }
    } else {
        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Строка-черновик в том же виде, что и товар
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(7.dp))
                )
                Spacer(Modifier.width(12.dp))
                val save: () -> Unit = {
                    if (name.isNotBlank()) {
                        onAdd(name.trim(), qty.toString(), null)
                        name = ""; qty = 1
                        focusRequester.requestFocus()
                    } else {
                        adding = false
                    }
                }
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = TextStyle(color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(ShoppingPrimary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                hasFocused = true
                            } else if (hasFocused) {
                                if (!suppressSave && name.isNotBlank()) {
                                    onAdd(name.trim(), qty.toString(), null)
                                }
                                suppressSave = false
                                name = ""; qty = 1
                                adding = false
                            }
                        },
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (name.isEmpty()) Text("Название товара", color = Outline, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StepButton(Icons.Outlined.Remove, "Меньше", size = 26.dp) { if (qty > 1) qty-- }
                    Text(
                        text = "$qty",
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 18.dp)
                    )
                    StepButton(Icons.Filled.Add, "Больше", size = 26.dp) { qty++ }
                    Spacer(Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { name = ""; qty = 1; adding = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Отменить", tint = Outline, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Подсказки из каталога
            if (suggestions.isNotEmpty()) {
                Divider(color = Color(0x14FFFFFF), modifier = Modifier.padding(horizontal = 14.dp))
                suggestions.forEach { product ->
                    val emoji = emojiForItem(product.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                suppressSave = true
                                onAdd(product.name, qty.toString(), product.id)
                                name = ""; qty = 1
                                adding = false
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Primary.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(10.dp))
                        if (emoji != null) {
                            Text(emoji, fontSize = 15.sp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(product.name, color = OnSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (!product.brand.isNullOrBlank()) {
                            Text(product.brand, color = OnSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0x1AFFFFFF))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = OnSurface, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun shoppingFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ShoppingPrimary,
    unfocusedBorderColor = Outline,
    focusedTextColor = OnSurface,
    unfocusedTextColor = OnSurface,
    cursorColor = ShoppingPrimary
)

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onQuantityChange: (Int) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val qty = item.quantity?.toIntOrNull() ?: 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (item.isChecked) ShoppingPrimary.copy(alpha = 0.25f) else Color(0x14FFFFFF))
                .border(1.dp, if (item.isChecked) ShoppingPrimary else Color(0x40FFFFFF), RoundedCornerShape(7.dp))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (item.isChecked) {
                Icon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = ShoppingPrimary, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        val emoji = remember(item.title) { emojiForItem(item.title) }
        if (emoji != null) {
            Text(text = emoji, fontSize = 17.sp)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onToggle)
        ) {
            Text(
                text = item.title,
                color = if (item.isChecked) Outline else OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
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

        // Количество справа со степпером −/+ (по умолчанию 1)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StepButton(Icons.Outlined.Remove, "Меньше", size = 26.dp) { onQuantityChange(qty - 1) }
            Text(
                text = "$qty",
                color = if (item.isChecked) Outline else OnSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 18.dp)
            )
            StepButton(Icons.Filled.Add, "Больше", size = 26.dp) { onQuantityChange(qty + 1) }
        }

        Spacer(Modifier.width(4.dp))
        // Удалить товар из списка
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = "Удалить",
                tint = Outline,
                modifier = Modifier.size(18.dp)
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
