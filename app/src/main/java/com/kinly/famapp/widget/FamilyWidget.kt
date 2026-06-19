package com.kinly.famapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kinly.famapp.MainActivity

class FamilyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetData.read(context)
        provideContent { WidgetContent(snapshot) }
    }
}

@Composable
private fun WidgetContent(data: WidgetSnapshot) {
    val accent = Color(0xFFD2BBFF)
    val white = Color(0xFFFFFFFF)
    val muted = Color(0xFF9DA0B5)
    val isTasks = data.mode == "tasks"
    val items = if (isTasks) data.tasks else data.shopping
    val openTab = if (isTasks) "tasks" else "shopping"

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(Color(0xF21A1430))
            .cornerRadius(20.dp)
            .padding(14.dp)
            // Тап по виджету открывает соответствующий раздел приложения.
            .clickable(actionStartActivity<MainActivity>(actionParametersOf(OpenTabKey to openTab)))
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isTasks) "Задачи" else "Покупки",
                style = TextStyle(color = ColorProvider(accent), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = if (isTasks) "→ Покупки" else "→ Задачи",
                style = TextStyle(color = ColorProvider(white), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier
                    .background(Color(0x1AFFFFFF))
                    .cornerRadius(10.dp)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .clickable(actionRunCallback<ToggleModeAction>())
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        if (items.isEmpty()) {
            Text(
                text = if (isTasks) "Нет активных задач" else "Список пуст",
                style = TextStyle(color = ColorProvider(muted), fontSize = 13.sp)
            )
        } else {
            LazyColumn {
                items(items) { line ->
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            text = "•  $line",
                            style = TextStyle(color = ColorProvider(white), fontSize = 14.sp)
                        )
                    }
                }
            }
        }
    }
}

/** Ключ параметра «какой раздел открыть» — приходит в Activity как intent extra "open_tab". */
val OpenTabKey = ActionParameters.Key<String>("open_tab")

/** Переключение Покупки ↔ Задачи прямо в виджете. */
class ToggleModeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        WidgetData.toggleMode(context)
        FamilyWidget().updateAll(context)
    }
}

class FamilyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FamilyWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetWork.schedule(context)
        WidgetWork.refreshNow(context)
    }
}
