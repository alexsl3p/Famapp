package com.kinly.famapp.widget

import android.content.Context

/** Снимок данных для виджета — общий между приложением и виджетом (SharedPreferences). */
data class WidgetSnapshot(
    val mode: String = "shopping", // "shopping" | "tasks"
    val shopping: List<String> = emptyList(),
    val tasks: List<String> = emptyList()
)

object WidgetData {
    private const val PREFS = "famapp_widget"
    private const val K_MODE = "mode"
    private const val K_SHOPPING = "shopping"
    private const val K_TASKS = "tasks"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(context: Context): WidgetSnapshot {
        val p = prefs(context)
        return WidgetSnapshot(
            mode = p.getString(K_MODE, "shopping") ?: "shopping",
            shopping = p.getString(K_SHOPPING, "").orEmpty().lines().filter { it.isNotBlank() },
            tasks = p.getString(K_TASKS, "").orEmpty().lines().filter { it.isNotBlank() }
        )
    }

    fun writeShopping(context: Context, items: List<String>) {
        prefs(context).edit().putString(K_SHOPPING, items.joinToString("\n")).apply()
    }

    fun writeTasks(context: Context, items: List<String>) {
        prefs(context).edit().putString(K_TASKS, items.joinToString("\n")).apply()
    }

    fun toggleMode(context: Context) {
        val p = prefs(context)
        val next = if ((p.getString(K_MODE, "shopping") ?: "shopping") == "shopping") "tasks" else "shopping"
        p.edit().putString(K_MODE, next).apply()
    }
}
