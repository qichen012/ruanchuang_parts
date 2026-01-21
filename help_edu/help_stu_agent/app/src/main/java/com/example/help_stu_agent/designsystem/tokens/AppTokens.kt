package com.example.help_stu_agent.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AppTokens {

    // 中密度间距系统：禁止散写 13.dp/17.dp 等
    object Space {
        val xxs = 2.dp
        val xs  = 4.dp
        val s   = 8.dp
        val m   = 12.dp
        val l   = 16.dp
        val xl  = 20.dp
        val xxl = 24.dp
        val xxxl = 32.dp
    }

    // 圆角：简约现代 + 学习场景舒适
    object Radius {
        val s = 8.dp
        val m = 12.dp
        val l = 16.dp
        val xl = 24.dp
        val panel = 28.dp
    }

    // 阴影层级：少而精
    object Elevation {
        val none = 0.dp
        val card = 2.dp
        val floating = 8.dp
        val overlay = 12.dp
        val panel = 20.dp
    }

    // 聚焦/黯淡（你知识树“学习模式”核心）
    object Alpha {
        const val enabled = 1f
        const val subtle  = 0.72f
        const val dim     = 0.25f
        const val disabled = 0.38f
    }

    // 语义色（不属于 Material ColorScheme 的部分）
    object SemanticColor {
        val success = Color(0xFF4CAF50)
        val warning = Color(0xFFF59E0B)
        val error   = Color(0xFFEF4444)
        val info    = Color(0xFF3F51B5)
    }
}
