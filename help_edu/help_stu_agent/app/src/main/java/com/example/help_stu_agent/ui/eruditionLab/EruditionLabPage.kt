package com.example.help_stu_agent.ui.eruditionLab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

data class SparkNode(
    val id: String,
    val title: String,
    val summary: String,
    val color: Color
)

class EruditionLabViewModel : ViewModel() {
    var sparks = mutableStateListOf<SparkNode>()
        private set

    var selectedNodeA by mutableStateOf<SparkNode?>(null)
        private set
    var selectedNodeB by mutableStateOf<SparkNode?>(null)
        private set

    var isSelectingA by mutableStateOf(false)
    var isSelectingB by mutableStateOf(false)

    init {
        // 初始化一些模拟星点
        if (sparks.isEmpty()) {
            val initialNodes = listOf(
                SparkNode("1", "量子物理基础", "探索微观世界的波粒二象性...", Color(0xFF6366F1)),
                SparkNode("2", "神经网络", "深度学习的核心架构与权重分配...", Color(0xFFEC4899)),
                SparkNode("3", "热力学定律", "熵增原理与能量守恒的宏观表现...", Color(0xFFF59E0B)),
                SparkNode("4", "线性代数", "矩阵变换与特征向量的几何意义...", Color(0xFF10B981)),
                SparkNode("5", "古典文学", "诗经与楚辞中的意象美学探讨...", Color(0xFF8B5CF6))
            )
            sparks.addAll(initialNodes)
        }
    }

    // 选择 A 节点
    fun selectNodeA(node: SparkNode) {
        selectedNodeA = node
        isSelectingA = false // 关闭弹窗
    }

    // 选择 B 节点
    fun selectNodeB(node: SparkNode) {
        selectedNodeB = node
        isSelectingB = false // 关闭弹窗
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EruditionLabPage(
    onBack: () -> Unit,
    viewModel: EruditionLabViewModel = viewModel()
) {
    // 处理选择 A 的 BottomSheet
    if (viewModel.isSelectingA) {
        SubjectSelectionBottomSheet(
            title = "Select Subject A",
            sparks = viewModel.sparks,
            onClose = { viewModel.isSelectingA = false },
            onSelect = { viewModel.selectNodeA(it) }
        )
    }

    // 处理选择 B 的 BottomSheet
    if (viewModel.isSelectingB) {
        SubjectSelectionBottomSheet(
            title = "Select Subject B",
            sparks = viewModel.sparks,
            onClose = { viewModel.isSelectingB = false },
            onSelect = { viewModel.selectNodeB(it) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // 1. 修改背景为白色
            .statusBarsPadding()
    ) {
        // 顶部操作栏
        NewTopHeader(onBack = onBack)

        // 页面主要内容
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .weight(1f) // 占满剩余空间，让按钮在底部
        ) {
            // 20.dp
            Spacer(Modifier.height(20.dp))

            // 2. 选择区域：两个并排的卡片
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SubjectSelectionCard(
                    modifier = Modifier.weight(1f),
                    label = "SELECT A",
                    selectedNode = viewModel.selectedNodeA,
                    onClick = { viewModel.isSelectingA = true }
                )
                Spacer(Modifier.width(16.dp))
                SubjectSelectionCard(
                    modifier = Modifier.weight(1f),
                    label = "SELECT B",
                    selectedNode = viewModel.selectedNodeB,
                    onClick = { viewModel.isSelectingB = true }
                )
            }

            // 40.dp
            Spacer(Modifier.height(40.dp))

            // 3. 维度区域
            IndicatorsSection(
                isSelectionComplete = viewModel.selectedNodeA != null && viewModel.selectedNodeB != null
            )
        }

        // 4. 底部生成按钮
        GenerateAlignmentButton(
            enabled = viewModel.selectedNodeA != null && viewModel.selectedNodeB != null,
            onClick = { /* TODO: 生成对齐逻辑 */ }
        )
        // 底部 20.dp 间距
        Spacer(Modifier.height(20.dp))
    }
}


@Composable
private fun NewTopHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(vertical = 12.dp, horizontal = 12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.background(Color(0xFFF3F4F6), CircleShape) // 灰色圆圈背景
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.Black)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            "Erudition Lab",
            color = Color.Black,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SubjectSelectionCard(
    modifier: Modifier = Modifier,
    label: String,
    selectedNode: SparkNode?,
    onClick: () -> Unit
) {
    val strokeColor = Color(0xFFE5E7EB) // 浅灰色虚线
    val borderRadius = 20.dp

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(200.dp)
            .drawWithContent {
                drawContent()
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                drawRoundRect(
                    color = strokeColor,
                    cornerRadius = CornerRadius(borderRadius.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect)
                )
            },
        color = Color.White,
        shape = RoundedCornerShape(borderRadius)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 如果选中了，显示选中内容；否则显示大加号
            if (selectedNode != null) {
                // 选中状态
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(selectedNode.color, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = selectedNode.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap to change",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            } else {
                // 未选中状态
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add",
                    tint = Color(0xFF9CA3AF), // 灰色加号
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = label, // 全大写
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.sp // 增加字间距
                )
            }
        }
    }
}

// 3. 指标区域：实现标题和脑部图标卡片
@Composable
private fun IndicatorsSection(isSelectionComplete: Boolean) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Layers, // 堆叠方块图标，代表层级/维度
                contentDescription = null,
                tint = Color(0xFF111827) // 黑色
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "比对维度 / Indicators", // 加粗标题
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF111827),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)) // 浅灰色边框
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 如果未选中两个主题，显示脑部图标占位符
                if (!isSelectionComplete) {
                    Icon(
                        Icons.Filled.Psychology, // 脑部图标
                        contentDescription = null,
                        tint = Color(0xFFE5E7EB), // 浅灰色
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Select subjects to reveal core metrics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9CA3AF) // 灰色
                    )
                } else {
                    // TODO: 显示维度指标数据
                    Text(
                        "Dimension analysis here...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// 4. 生成按钮：全宽，包含闪电图标
@Composable
private fun GenerateAlignmentButton(enabled: Boolean, onClick: () -> Unit) {
    // 启用和禁用时的颜色
    val containerColor = if (enabled) MaterialTheme.colorScheme.primary else Color(0xFFE5E7EB)
    val contentColor = if (enabled) Color.White else Color(0xFF9CA3AF)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(56.dp), // 全通栏高度
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFFE5E7EB),
            disabledContentColor = Color(0xFF9CA3AF)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Icon(
            Icons.Filled.FlashOn, // 闪电图标
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "GENERATE ALIGNMENT", // 全大写
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// 5. 主题选择 BottomSheet：用于让用户选择 SparkNode
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectSelectionBottomSheet(
    title: String,
    sparks: List<SparkNode>,
    onClose: () -> Unit,
    onSelect: (SparkNode) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        dragHandle = null,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, null, tint = Color.Gray)
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn {
                items(sparks) { node ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(node) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(12.dp).background(node.color, CircleShape))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            node.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}