package com.kinly.famapp.features.voice

/** Результат разбора голосовой команды. */
sealed class VoiceCommand {
    data class AddShopping(val title: String) : VoiceCommand()
    data class AddTask(val title: String) : VoiceCommand()
    data class Unknown(val raw: String) : VoiceCommand()
}

/**
 * Простой разбор русских голосовых команд:
 *   «добавь в список покупок яйца»  → AddShopping("Яйца")
 *   «купить молоко»                  → AddShopping("Молоко")
 *   «добавь задачу вынести мусор»    → AddTask("Вынести мусор")
 *   «напомни полить цветы»           → AddTask("Полить цветы")
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

    fun parse(raw: String): VoiceCommand {
        val text = raw.trim()
        if (text.isEmpty()) return VoiceCommand.Unknown(raw)
        val lower = text.lowercase()

        return when {
            taskTriggers.any { lower.contains(it) } ->
                VoiceCommand.AddTask(capitalize(extractAfter(text, lower, taskAnchors)))

            shoppingTriggers.any { lower.contains(it) } ->
                VoiceCommand.AddShopping(capitalize(extractAfter(text, lower, shoppingAnchors)))

            else -> VoiceCommand.Unknown(text)
        }.let { cmd ->
            // Если после якоря ничего не осталось — считаем команду непонятой.
            when (cmd) {
                is VoiceCommand.AddTask -> if (cmd.title.isBlank()) VoiceCommand.Unknown(text) else cmd
                is VoiceCommand.AddShopping -> if (cmd.title.isBlank()) VoiceCommand.Unknown(text) else cmd
                else -> cmd
            }
        }
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
