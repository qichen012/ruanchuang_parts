package com.example.help_stu_agent.ui.treeHistory

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun TreeIcon(
    modifier: Modifier,
    color: Color = Color(0xFF6366F1)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 线宽随尺寸自适应，保持卡通“粗线条”感觉
        val strokeW = (minOf(w, h) * 0.10f).coerceAtLeast(2.dp.toPx())
        val stroke = Stroke(width = strokeW, cap = StrokeCap.Round)

        // 1) 树冠（云朵/团簇式，更卡通）
        val crown = Path().apply {
            // 使用几段圆润曲线拼一个“树冠团”
            moveTo(w * 0.25f, h * 0.52f)
            cubicTo(w * 0.18f, h * 0.42f, w * 0.28f, h * 0.30f, w * 0.40f, h * 0.34f)
            cubicTo(w * 0.42f, h * 0.22f, w * 0.62f, h * 0.22f, w * 0.64f, h * 0.34f)
            cubicTo(w * 0.77f, h * 0.30f, w * 0.86f, h * 0.44f, w * 0.76f, h * 0.54f)
            cubicTo(w * 0.78f, h * 0.68f, w * 0.58f, h * 0.76f, w * 0.50f, h * 0.66f)
            cubicTo(w * 0.42f, h * 0.78f, w * 0.22f, h * 0.68f, w * 0.25f, h * 0.52f)
            close()
        }

        // 树冠填充（淡色），再描边（深色）
        drawPath(
            path = crown,
            color = color.copy(alpha = 0.18f)
        )
        drawPath(
            path = crown,
            color = color.copy(alpha = 0.95f),
            style = stroke
        )

        // 2) 树干（圆角矩形）
        val trunkW = w * 0.16f
        val trunkH = h * 0.22f
        val trunkLeft = (w - trunkW) / 2f
        val trunkTop = h * 0.62f
        drawRoundRect(
            color = color.copy(alpha = 0.18f),
            topLeft = Offset(trunkLeft, trunkTop),
            size = Size(trunkW, trunkH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trunkW * 0.35f, trunkW * 0.35f)
        )
        drawRoundRect(
            color = color.copy(alpha = 0.95f),
            topLeft = Offset(trunkLeft, trunkTop),
            size = Size(trunkW, trunkH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trunkW * 0.35f, trunkW * 0.35f),
            style = stroke
        )

        // 3) 地面小弧线（增加“卡通贴纸感”）
        val ground = Path().apply {
            moveTo(w * 0.22f, h * 0.88f)
            cubicTo(w * 0.36f, h * 0.94f, w * 0.64f, h * 0.94f, w * 0.78f, h * 0.88f)
        }
        drawPath(ground, color.copy(alpha = 0.55f), style = stroke)
    }
}
