package com.kinly.famapp.widget

import android.content.Context

/** Позиция виджета: id (для отметки) + название. */
data class WidgetItem(val id: String, val title: String)

/** Снимок данных для виджета — общий между приложением и виджетом (SharedPreferences). */
data class WidgetSnapshot(
    val mode: String = "shopping", // "shopping" | "tasks"
    val shopping: List<WidgetItem> = emptyList(),
    val tasks: List<WidgetItem> = emptyList()
)

object WidgetData {
    private const val PREFS = "famapp_widget"
    private const val K_MODE = "mode"
    private const val K_SHOPPING = "shopping"
    private const val K_TASKS = "tasks"
    private const val K_FAMILY = "family_id"
    private const val K_USER = "user_id"
    private const val K_LAST_SEEN = "notif_last_seen"
    private const val SEP = "" // разделитель id/title

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun encode(items: List<WidgetItem>): String =
        items.joinToString("\n") { it.id + SEP + it.title }

    private fun decode(raw: String?): List<WidgetItem> =
        raw.orEmpty().lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val i = line.indexOf(SEP)
            if (i >= 0) WidgetItem(line.substring(0, i), line.substring(i + 1))
            else WidgetItem("", line) // обратная совместимость со старым форматом
        }

    fun read(context: Context): WidgetSnapshot {
        val p = prefs(context)
        return WidgetSnapshot(
            mode = p.getString(K_MODE, "shopping") ?: "shopping",
            shopping = decode(p.getString(K_SHOPPING, "")),
            tasks = decode(p.getString(K_TASKS, ""))
        )
    }

    fun writeFamilyId(context: Context, familyId: String) {
        prefs(context).edit().putString(K_FAMILY, familyId).apply()
    }

    fun readFamilyId(context: Context): String? = prefs(context).getString(K_FAMILY, null)

    fun writeUserId(context: Context, userId: String) {
        prefs(context).edit().putString(K_USER, userId).apply()
    }

    fun readUserId(context: Context): String? = prefs(context).getString(K_USER, null)

    fun readLastSeen(context: Context): String? = prefs(context).getString(K_LAST_SEEN, null)

    fun writeLastSeen(context: Context, iso: String) {
        prefs(context).edit().putString(K_LAST_SEEN, iso).apply()
    }

    fun writeShopping(context: Context, items: List<WidgetItem>) {
        prefs(context).edit().putString(K_SHOPPING, encode(items)).apply()
    }

    fun writeTasks(context: Context, items: List<WidgetItem>) {
        prefs(context).edit().putString(K_TASKS, encode(items)).apply()
    }

    fun toggleMode(context: Context) {
        val p = prefs(context)
        val next = if ((p.getString(K_MODE, "shopping") ?: "shopping") == "shopping") "tasks" else "shopping"
        p.edit().putString(K_MODE, next).apply()
    }
}
