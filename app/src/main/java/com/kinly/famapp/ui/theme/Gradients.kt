package com.kinly.famapp.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Reusable gradients, glows and glass tokens for the 2026 redesign.
// ============================================================

/** Светлый лавандово-розовый акцент (FAB, основные кнопки, активные элементы). */
val AccentGradient = listOf(Primary, Secondary)

/** Тот же светлый градиент для активной вкладки segmented-control. */
val ActivePillGradient = listOf(Primary, Secondary)

/** Indigo → cyan badge behind a list's icon. */
val BadgeGradient = listOf(Color(0xFF4F46E5), Color(0xFF22B7E6))

// Glass surfaces
val GlassPillBg = Color(0x14FFFFFF)       // segmented control / nav container
val GlassPillBorder = Color(0x26FFFFFF)   // 15% white hairline border
val GlassFieldBg = Color(0x0DFFFFFF)      // subtle inner fill

// Glow tints (used as shadow spot/ambient colors)
val GlowViolet = Color(0xFF7C3AED)
val GlowMagenta = Color(0xFFD946EF)
