package com.example.help_stu_agent.ui.theme

import androidx.compose.ui.graphics.Color

// 低负担学习系：单主色 + 高质量中性色
val Primary = Color(0xFF3F51B5)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFE8EAF6)
val OnPrimaryContainer = Color(0xFF1F2A6B)

val Background = Color(0xFFF8F9FA)
val OnBackground = Color(0xFF111827)

val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF111827)
val SurfaceVariant = Color(0xFFF2F4F7)
val OnSurfaceVariant = Color(0xFF6B7280)

val Outline = Color(0xFFE5E7EB)
val OutlineVariant = Color(0xFFD1D5DB)

// Dark（保持低负担，不做纯黑）
val PrimaryDark = Color(0xFFB9C2FF)
val OnPrimaryDark = Color(0xFF141A3F)
val PrimaryContainerDark = Color(0xFF2B357C)
val OnPrimaryContainerDark = Color(0xFFE8EAFF)

val BackgroundDark = Color(0xFF0F172A)
val OnBackgroundDark = Color(0xFFE5E7EB)

val SurfaceDark = Color(0xFF111B2E)
val OnSurfaceDark = Color(0xFFE5E7EB)
val SurfaceVariantDark = Color(0xFF16213A)
val OnSurfaceVariantDark = Color(0xFFB6BECE)

val OutlineDark = Color(0xFF2A3550)
val OutlineVariantDark = Color(0xFF324062)

/**
 * 分支色调色板（用于“一级子树”区分）
 * 设计原则：低饱和/偏中性，不抢主色；用于强调条/边框/glow 小面积点缀
 */
val BranchPaletteLight: List<Color> = listOf(
    Color(0xFF3F51B5), // Indigo（接近主色）
    Color(0xFF0F766E), // Teal
    Color(0xFF7C3AED), // Violet
    Color(0xFFB45309), // Amber/Brown
    Color(0xFFBE123C), // Rose
    Color(0xFF2563EB), // Blue
    Color(0xFF15803D)  // Green
)

val BranchPaletteDark: List<Color> = listOf(
    Color(0xFFB9C2FF), // Indigo light
    Color(0xFF5EEAD4), // Teal light
    Color(0xFFC4B5FD), // Violet light
    Color(0xFFFCD34D), // Amber light
    Color(0xFFFDA4AF), // Rose light
    Color(0xFF93C5FD), // Blue light
    Color(0xFF86EFAC)  // Green light
)
