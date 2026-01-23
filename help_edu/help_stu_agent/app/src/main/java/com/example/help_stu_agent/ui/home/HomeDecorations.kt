package com.example.help_stu_agent.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

@Composable
fun BoxScope.HomeDecorations(
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    // 全部从 colorScheme 派生，不写死颜色常量
    val blob1 = cs.primary.copy(alpha = 0.10f)
    val blob2 = cs.secondary.copy(alpha = 0.10f)
    val blob3 = cs.tertiary.copy(alpha = 0.08f)
    val dot = cs.outlineVariant.copy(alpha = 0.25f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 背景抽象“纸片”形状 1（右下）
        translate(left = w * 0.45f, top = h * 0.38f) {
            rotate(degrees = -12f) {
                drawRoundRect(
                    color = blob2,
                    topLeft = Offset.Zero,
                    size = Size(w * 0.62f, h * 0.42f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(80f, 80f)
                )
            }
        }

        // 背景抽象“纸片”形状 2（中下）
        translate(left = w * 0.18f, top = h * 0.52f) {
            rotate(degrees = 8f) {
                drawRoundRect(
                    color = blob1,
                    topLeft = Offset.Zero,
                    size = Size(w * 0.68f, h * 0.34f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(90f, 90f)
                )
            }
        }

        // 顶部柔和 blob（左上）
        val p = Path().apply {
            moveTo(w * 0.05f, h * 0.12f)
            cubicTo(
                w * 0.18f, h * 0.02f,
                w * 0.40f, h * 0.02f,
                w * 0.48f, h * 0.16f
            )
            cubicTo(
                w * 0.58f, h * 0.33f,
                w * 0.38f, h * 0.42f,
                w * 0.22f, h * 0.34f
            )
            cubicTo(
                w * 0.08f, h * 0.27f,
                w * 0.00f, h * 0.22f,
                w * 0.05f, h * 0.12f
            )
            close()
        }
        drawPath(color = blob3, path = p)

        // 轻量点阵（增加空间感）
        val step = w.coerceAtMost(h) / 14f
        val r = step / 18f
        var y = h * 0.12f
        while (y < h) {
            var x = w * 0.06f
            while (x < w) {
                val a = if (((x / step).toInt() + (y / step).toInt()) % 6 == 0) 0.35f else 0.18f
                drawCircle(color = dot.copy(alpha = dot.alpha * a), radius = r, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}
