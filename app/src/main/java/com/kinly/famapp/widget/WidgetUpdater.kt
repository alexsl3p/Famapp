package com.kinly.famapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Обновляет снимок данных виджета и перерисовывает виджеты на главном экране. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun setFamily(familyId: String) {
        WidgetData.writeFamilyId(context, familyId)
        WidgetWork.schedule(context)
    }

    fun setUser(userId: String) {
        WidgetData.writeUserId(context, userId)
    }

    fun updateShopping(items: List<String>) {
        WidgetData.writeShopping(context, items)
        refresh()
    }

    fun updateTasks(items: List<String>) {
        WidgetData.writeTasks(context, items)
        refresh()
    }

    private fun refresh() {
        scope.launch { runCatching { FamilyWidget().updateAll(context) } }
    }
}
