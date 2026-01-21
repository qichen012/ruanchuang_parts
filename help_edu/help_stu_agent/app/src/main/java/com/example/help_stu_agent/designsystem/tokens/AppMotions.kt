package com.example.help_stu_agent.designsystem.tokens

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object AppMotion {

    // 时长（ms）
    const val fast = 140    // 按钮/轻反馈
    const val medium = 220  // 切换/Tooltip
    const val slow = 320    // 面板/结构变化
    const val focus = 420   // 聚焦/树居中（强反馈）

    // 默认曲线
    val standardEasing: Easing = FastOutSlowInEasing

    // 常用 tween spec（避免各处散写）
    fun tweenFast() = tween<Float>(durationMillis = fast, easing = standardEasing)
    fun tweenMedium() = tween<Float>(durationMillis = medium, easing = standardEasing)
    fun tweenSlow() = tween<Float>(durationMillis = slow, easing = standardEasing)

    // 丝滑聚焦：轻弹性但不晃
    fun springFocus() = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 450f,
        visibilityThreshold = 0.001f
    )

}
