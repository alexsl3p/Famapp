package com.kinly.famapp.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Reusable gradients, glows and glass tokens for the 2026 redesign.
// ============================================================

/** Violet → magenta accent (FAB, primary actions, active highlights). */
val AccentGradient = listOf(Color(0xFF7C3AED), Color(0xFFD946EF))

/** Softer violet pill used for the active segmented-control tab. */
val ActivePillGradient = listOf(Color(0xFF5B3F9E), Color(0xFF7C3AED))

/** Indigo → cyan badge behind a list's icon. */
val BadgeGradient = listOf(Color(0xFF4F46E5), Color(0xFF22B7E6))

// Glass surfaces
val GlassPillBg = Color(0x14FFFFFF)       // segmented control / nav container
val GlassPillBorder = Color(0x26FFFFFF)   // 15% white hairline border
val GlassFieldBg = Color(0x0DFFFFFF)      // subtle inner fill

// Glow tints (used as shadow spot/ambient colors)
val GlowViolet = Color(0xFF7C3AED)
val GlowMagenta = Color(0xFFD946EF)
