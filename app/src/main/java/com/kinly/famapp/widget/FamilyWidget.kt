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
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kinly.famapp.MainActivity

class FamilyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetData.read(context)
        provideContent { WidgetContent(snapshot) }
    }
}

private val Lavender = Color(0xFFD2BBFF)
private val Pink = Color(0xFFFFAFD3)
private val White = Color(0xFFFFFFFF)
private val Muted = Color(0xFF9DA0B5)
private val DarkInk = Color(0xFF0B1326)

@Composable
private fun WidgetContent(data: WidgetSnapshot) {
    val isTasks = data.mode == "tasks"
    val items = if (isTasks) data.tasks else data.shopping
    val openTab = if (isTasks) "tasks" else "shopping"

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(Color(0xF21A1430))
            .cornerRadius(22.dp)
            .padding(14.dp)
    ) {
        // Переключатель Задачи / Покупки
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            ModeTab(label = "Задачи", selected = isTasks, mode = "tasks")
            Spacer(GlanceModifier.width(8.dp))
            ModeTab(label = "Покупки", selected = !isTasks, mode = "shopping")
        }

        Spacer(GlanceModifier.height(12.dp))

        // Список — занимает всё доступное место
        Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            if (items.isEmpty()) {
                Text(
                    text = if (isTasks) "Нет активных задач" else "Список пуст",
                    style = TextStyle(color = ColorProvider(Muted), fontSize = 15.sp)
                )
            } else {
                val kind = if (isTasks) "task" else "shopping"
                LazyColumn {
                    items(items, itemId = { it.id.hashCode().toLong() }) { item ->
                        CheckBox(
                            checked = false,
                            onCheckedChange = actionRunCallback<ToggleItemAction>(
                                actionParametersOf(ItemIdKey to item.id, ItemKindKey to kind)
                            ),
                            text = item.title,
                            style = TextStyle(color = ColorProvider(White), fontSize = 16.sp),
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(GlanceModifier.height(10.dp))

        // Нижняя панель: розовые кнопки + и микрофон (как в приложении)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(GlanceModifier.defaultWeight())
            RoundButton(
                glyph = "+",
                glyphSize = 26.sp,
                action = actionStartActivity<MainActivity>(
                    actionParametersOf(OpenTabKey to openTab, StartAddKey to true)
                )
            )
            Spacer(GlanceModifier.width(16.dp))
            RoundButton(
                glyph = "🎤",
                glyphSize = 20.sp,
                action = actionStartActivity<MainActivity>(
                    actionParametersOf(StartVoiceKey to true)
                )
            )
            Spacer(GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, mode: String) {
    Box(
        modifier = GlanceModifier
            .background(if (selected) Pink else Color(0x1AFFFFFF))
            .cornerRadius(14.dp)
            .clickable(actionRunCallback<SetModeAction>(actionParametersOf(ModeKey to mode)))
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(if (selected) DarkInk else White),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun RoundButton(glyph: String, glyphSize: androidx.compose.ui.unit.TextUnit, action: androidx.glance.action.Action) {
    Box(
        modifier = GlanceModifier
            .width(54.dp)
            .height(54.dp)
            .background(Pink)
            .cornerRadius(27.dp)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            style = TextStyle(color = ColorProvider(DarkInk), fontSize = glyphSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        )
    }
}

/** Ключ параметра «какой раздел открыть» — приходит в Activity как intent extra "open_tab". */
val OpenTabKey = ActionParameters.Key<String>("open_tab")
/** Открыть приложение сразу с голосовым вводом. */
val StartVoiceKey = ActionParameters.Key<Boolean>("start_voice")
/** Открыть приложение сразу с формой добавления в текущем разделе. */
val StartAddKey = ActionParameters.Key<Boolean>("start_add")
/** Параметры отметки позиции из виджета. */
val ItemIdKey = ActionParameters.Key<String>("item_id")
val ItemKindKey = ActionParameters.Key<String>("item_kind")
/** Какой режим выставить в виджете ("tasks"/"shopping"). */
val ModeKey = ActionParameters.Key<String>("mode")

/** Явный выбор режима (Задачи/Покупки) прямо в виджете. */
class SetModeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val mode = parameters[ModeKey] ?: return
        WidgetData.setMode(context, mode)
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
