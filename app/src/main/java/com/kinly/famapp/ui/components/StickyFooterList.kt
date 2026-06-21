package com.kinly.famapp.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp

/**
 * Прокручиваемый список с «приклеенным» футером (например, кнопкой добавления).
 *
 * Пока всё содержимое помещается на экране — футер стоит сразу под списком
 * отдельной строкой. Как только список перестаёт влезать — список начинает
 * прокручиваться, а футер остаётся прижатым к низу (над таб-баром).
 *
 * Реализовано через SubcomposeLayout: футер измеряется первым, поэтому список
 * сразу получает правильную доступную высоту и футер не «прыгает» и не пропадает.
 */
@Composable
fun ScrollListWithStickyFooter(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollState: ScrollState = rememberScrollState(),
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    SubcomposeLayout(modifier = modifier.fillMaxSize()) { constraints ->
        val width = constraints.maxWidth
        val maxHeight = constraints.maxHeight

        // 1) Измеряем футер по натуральной высоте.
        val footerPlaceables = subcompose("footer", footer).map {
            it.measure(constraints.copy(minHeight = 0))
        }
        val footerHeight = footerPlaceables.maxOfOrNull { it.height } ?: 0

        // 2) Список получает оставшуюся высоту и при переполнении скроллится.
        val contentMaxHeight = (maxHeight - footerHeight).coerceAtLeast(0)
        val contentPlaceables = subcompose("content") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(contentPadding),
                content = content
            )
        }.map { it.measure(constraints.copy(minHeight = 0, maxHeight = contentMaxHeight)) }
        val contentHeight = contentPlaceables.maxOfOrNull { it.height } ?: 0

        layout(width, maxHeight) {
            contentPlaceables.forEach { it.placeRelative(0, 0) }
            // Футер — сразу под списком (когда он короткий) либо у самого низа (когда длинный).
            footerPlaceables.forEach { it.placeRelative(0, contentHeight) }
        }
    }
}
