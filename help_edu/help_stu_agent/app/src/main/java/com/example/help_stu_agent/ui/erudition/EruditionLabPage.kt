package com.example.help_stu_agent.ui.erudition

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sqrt


sealed class SparkyMode {
    object Collapsed : SparkyMode()
    data class Focus(val nodeId: String) : SparkyMode()
}

data class SparkNode(
    val id: String,
    val title: String,
    val summary: String,
    val offset: Offset,
    val color: Color
)

class SparkyViewModel : ViewModel() {
    // UI 模式
    var mode by mutableStateOf<SparkyMode>(SparkyMode.Collapsed)
        private set

    // 模拟知识点数据
    var sparks = mutableStateListOf<SparkNode>()
        private set

    init {
        // 初始化一些模拟星点
        if (sparks.isEmpty()) {
            val initialNodes = listOf(
                SparkNode("1", "量子物理基础", "探索微观世界的波粒二象性...", Offset(200f, 400f), Color(0xFF6366F1)),
                SparkNode("2", "神经网络", "深度学习的核心架构与权重分配...", Offset(600f, 700f), Color(0xFFEC4899)),
                SparkNode("3", "热力学定律", "熵增原理与能量守恒的宏观表现...", Offset(300f, 900f), Color(0xFFF59E0B)),
                SparkNode("4", "线性代数", "矩阵变换与特征向量的几何意义...", Offset(700f, 300f), Color(0xFF10B981)),
                SparkNode("5", "古典文学", "诗经与楚辞中的意象美学探讨...", Offset(150f, 1200f), Color(0xFF8B5CF6))
            )
            sparks.addAll(initialNodes)
        }
    }

    fun updateNodePosition(id: String, dragAmount: Offset) {
        val index = sparks.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = sparks[index]
            sparks[index] = old.copy(offset = old.offset + dragAmount)
        }
    }

    fun focusNode(id: String) { mode = SparkyMode.Focus(id) }
    fun resetMode() { mode = SparkyMode.Collapsed }
}

// --- 主页面 ---

@Composable
fun EruditionLabPage(
    onBack: () -> Unit,
    viewModel: SparkyViewModel = viewModel()
) {
    val mode = viewModel.mode
    val sparks = viewModel.sparks

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // 深蓝黑色背景
    ) {
        // 1. 底层：星空背景
        StaticStarField()

        // 2. 连线层：当节点靠近时自动连线
        ConnectionsCanvas(sparks)

        // 3. 节点层
        sparks.forEach { node ->
            val isDimmed = mode is SparkyMode.Focus && (mode as SparkyMode.Focus).nodeId != node.id
            SparkNodeItem(
                node = node,
                isDimmed = isDimmed,
                onDrag = { viewModel.updateNodePosition(node.id, it) },
                onTap = { viewModel.focusNode(node.id) }
            )
        }

        // 4. 交互式详情覆盖层 (Focus 模式)
        AnimatedVisibility(
            visible = mode is SparkyMode.Focus,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            if (mode is SparkyMode.Focus) {
                val focusedNode = sparks.find { it.id == mode.nodeId }
                focusedNode?.let {
                    FocusDetailOverlay(
                        node = it,
                        onClose = { viewModel.resetMode() },
                        onViewReport = { }
                    )
                }
            }
        }

        // 顶部操作栏
        TopHeader(onBack = onBack, isFocusMode = mode is SparkyMode.Focus)
    }
}

// --- 组件部分 ---

@Composable
private fun SparkNodeItem(
    node: SparkNode,
    isDimmed: Boolean,
    onDrag: (Offset) -> Unit,
    onTap: () -> Unit
) {
    val alpha by animateFloatAsState(if (isDimmed) 0.15f else 1f, label = "alpha")
    val scale by animateFloatAsState(if (isDimmed) 0.8f else 1f, label = "scale")

    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(node.offset.x.roundToInt(), node.offset.y.roundToInt()) }
            .size(80.dp)
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale * pulseScale
                this.scaleY = scale * pulseScale
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        // 外层光晕
        Box(
            Modifier
                .fillMaxSize()
                .background(node.color.copy(alpha = 0.2f), CircleShape)
        )
        // 核心点
        Box(
            Modifier
                .size(30.dp)
                .background(
                    Brush.radialGradient(listOf(Color.White, node.color)),
                    CircleShape
                )
        )
        // 文字标签
        Text(
            text = node.title.take(4),
            modifier = Modifier.offset(y = 45.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ConnectionsCanvas(sparks: List<SparkNode>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (i in sparks.indices) {
            for (j in i + 1 until sparks.size) {
                val p1 = sparks[i].offset + Offset(40.dp.toPx(), 40.dp.toPx())
                val p2 = sparks[j].offset + Offset(40.dp.toPx(), 40.dp.toPx())
                val dist = sqrt((p1.x - p2.x).let { it * it } + (p1.y - p2.y).let { it * it })

                // 如果距离小于 600px，绘制连线
                if (dist < 600f) {
                    drawLine(
                        color = Color.White.copy(alpha = (1f - dist / 600f) * 0.2f),
                        start = p1,
                        end = p2,
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusDetailOverlay(
    node: SparkNode,
    onClose: () -> Unit,
    onViewReport: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 背景遮罩
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onClose() }
        )

        // 磨砂玻璃卡片
        Surface(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .blur(if (android.os.Build.VERSION.SDK_INT >= 31) 20.dp else 0.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(node.color, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        node.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    node.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onViewReport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = node.color),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Description, null)
                    Spacer(Modifier.width(8.dp))
                    Text("查看知识报告")
                }
            }
        }
    }
}

@Composable
private fun TopHeader(onBack: () -> Unit, isFocusMode: Boolean) {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .padding(20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White)
        }

        if (!isFocusMode) {
            Text(
                "Sparky Link",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Light
            )
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFDE047),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StaticStarField() {
    val starCount = 60
    val rnd = remember { Random(42) }
    val config = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { config.screenWidthDp.dp.toPx() }
    val screenHeight = with(LocalDensity.current) { config.screenHeightDp.dp.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        repeat(starCount) {
            drawCircle(
                color = Color.White.copy(alpha = rnd.nextFloat() * 0.5f),
                radius = rnd.nextFloat() * 3f,
                center = Offset(rnd.nextFloat() * screenWidth, rnd.nextFloat() * screenHeight)
            )
        }
    }
}