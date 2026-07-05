package com.kinly.famapp.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/** Виды тактильного отклика: лёгкий тик для переключений, увесистый — для подтверждений. */
enum class Haptic { Tick, Confirm }

/**
 * Тактильный отклик как в системных приложениях: галочки, отправка, переключение вкладок.
 * Работает через View-механизм — не требует разрешения VIBRATE.
 */
@Composable
fun rememberHaptic(): (Haptic) -> Unit {
    val view = LocalView.current
    return { kind ->
        val constant = when (kind) {
            Haptic.Tick -> HapticFeedbackConstants.CLOCK_TICK
            Haptic.Confirm ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.KEYBOARD_TAP
        }
        view.performHapticFeedback(constant)
    }
}
