package com.kinly.famapp.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Прокручиваемый список с «приклеенным» футером (например, кнопкой добавления).
 *
 * Пока всё содержимое помещается на экране — футер стоит сразу под списком
 * отдельной строкой. Как только список перестаёт влезать — список начинает
 * прокручиваться, а футер остаётся прижатым к низу (над таб-баром).
 */
@Composable
fun ScrollListWithStickyFooter(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollState: ScrollState = rememberScrollState(),
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var footerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxH = maxHeight
        val footerDp = with(density) { footerHeightPx.toDp() }
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (maxH - footerDp).coerceAtLeast(0.dp))
                    .verticalScroll(scrollState)
                    .padding(contentPadding),
                content = content
            )
            Box(modifier = Modifier.onSizeChanged { footerHeightPx = it.height }) {
                footer()
            }
        }
    }
}
