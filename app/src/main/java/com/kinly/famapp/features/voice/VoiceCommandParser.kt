package com.kinly.famapp.features.voice

/** Результат разбора голосовой команды. */
sealed class VoiceCommand {
    /** Один или несколько товаров за раз. */
    data class AddShopping(val titles: List<String>) : VoiceCommand()
    /** Одна или несколько задач за раз. */
    data class AddTask(val titles: List<String>) : VoiceCommand()
    data class Unknown(val raw: String) : VoiceCommand()
}

/**
 * Простой разбор русских голосовых команд:
 *   «добавь в список молоко хлеб яйца»       → AddShopping([Молоко, Хлеб, Яйца])
 *   «купить молоко и хлеб»                    → AddShopping([Молоко, Хлеб])
 *   «добавь туалетную бумагу, молоко»         → AddShopping([Туалетную бумагу, Молоко])
 *   «добавь задачу вынести мусор»             → AddTask([Вынести мусор])
 *   «задачи вынести мусор, помыть посуду»     → AddTask([Вынести мусор, Помыть посуду])
 */
object VoiceCommandParser {

    // Якоря для задач (берём всё, что после якоря). Порядок — от более конкретных к общим.
    private val taskAnchors = listOf(
        "задачу", "задача", "задачи", "задание",
        "напомни мне", "напомни", "нужно сделать", "надо сделать", "сделать"
    )

    // Якоря для покупок.
    private val shoppingAnchors = listOf(
        "список покупок", "в список покупок", "в покупки", "список",
        "купить", "купи", "куплю", "в магазине", "в магазин"
    )

    // Слова-триггеры, по которым решаем, что это вообще за команда.
    private val taskTriggers = listOf("задач", "задани", "напомни", "сделать")
    private val shoppingTriggers = listOf("список", "покупк", "покупо", "купить", "купи", "куплю", "магазин")

    // Явные разделители перечисления: запятая, «и», «да», плюс, перенос строки.
    private val strongSeparators = Regex("\\s*[,;+\\n]\\s*|\\s+и\\s+|\\s+да\\s+")

    fun parse(raw: String): VoiceCommand {
        val text = raw.trim()
        if (text.isEmpty()) return VoiceCommand.Unknown(raw)
        val lower = text.lowercase()

        return when {
            taskTriggers.any { lower.contains(it) } -> {
                val items = splitTaskItems(extractAfter(text, lower, taskAnchors))
                if (items.isEmpty()) VoiceCommand.Unknown(text) else VoiceCommand.AddTask(items)
            }
            shoppingTriggers.any { lower.contains(it) } -> {
                val items = splitShoppingItems(extractAfter(text, lower, shoppingAnchors))
                if (items.isEmpty()) VoiceCommand.Unknown(text) else VoiceCommand.AddShopping(items)
            }
            else -> VoiceCommand.Unknown(text)
        }
    }

    /**
     * Покупки: «молоко хлеб яйца» → три товара.
     * Если есть явные разделители (запятая/«и») — режем по ним (сохраняя составные названия
     * вроде «туалетная бумага»). Иначе — по пробелам (диктовка простых товаров через паузу).
     */
    private fun splitShoppingItems(s: String): List<String> {
        val text = s.trim()
        if (text.isEmpty()) return emptyList()
        val parts = if (strongSeparators.containsMatchIn(text)) {
            text.split(strongSeparators)
        } else {
            text.split(Regex("\\s+"))
        }
        return parts.map { capitalize(it) }.filter { it.isNotBlank() }
    }

    /**
     * Задачи: режем только по явным разделителям (запятая/«и»), чтобы не ломать
     * многословные задачи вроде «вынести мусор».
     */
    private fun splitTaskItems(s: String): List<String> {
        val text = s.trim()
        if (text.isEmpty()) return emptyList()
        return text.split(strongSeparators).map { capitalize(it) }.filter { it.isNotBlank() }
    }

    /** Возвращает текст после первого найденного якоря; если якоря нет — убирает вводные слова. */
    private fun extractAfter(text: String, lower: String, anchors: List<String>): String {
        for (anchor in anchors) {
            val idx = lower.indexOf(anchor)
            if (idx >= 0) {
                return text.substring(idx + anchor.length).trim().trimStart(',', ':', '-', ' ')
            }
        }
        // Якорь не найден — срезаем типичные вводные «добавь», «добавить» в начале.
        var t = text.trim()
        for (lead in listOf("добавь", "добавить", "пожалуйста")) {
            if (t.lowercase().startsWith(lead)) t = t.substring(lead.length).trim()
        }
        return t
    }

    private fun capitalize(s: String): String =
        s.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
