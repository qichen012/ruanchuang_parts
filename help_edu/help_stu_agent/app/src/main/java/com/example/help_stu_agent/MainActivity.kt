package com.example.help_stu_agent

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.help_stu_agent.designsystem.components.AppBottomPanel
import com.example.help_stu_agent.designsystem.components.AppCard
import com.example.help_stu_agent.designsystem.components.AppTopBar
import com.example.help_stu_agent.designsystem.components.AppTooltipCard
import com.example.help_stu_agent.designsystem.tokens.AppTokens
import com.example.help_stu_agent.ui.theme.HelpStuAgentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assetFileName = "demo.json"

        setContent {
            HelpStuAgentTheme {
                var jsonStr by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    jsonStr = loadJsonFromAssets(assetFileName)
                }

                if (jsonStr != null) {
                    KnowledgeTree(jsonStr!!)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    private suspend fun loadJsonFromAssets(fileName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

    @Suppress("unused")
    private suspend fun loadJsonFromFile(path: String): String {
        return withContext(Dispatchers.IO) {
            try {
                File(path).readText()
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeTree(jsonContent: String) {
    val coroutineScope = rememberCoroutineScope()

    // 解析 + layoutData
    val rootData = remember(jsonContent) { Json.decodeFromString<KnowledgeJson>(jsonContent) }
    val layoutData = remember(rootData) {
        TreeLayoutEngine(TreeLayoutMode.MINDMAP_VERTICAL).calculate(rootData)
    }

    // 主分支（一级子节点）用于 Slider 导航
    val mainBranches = remember(rootData) { rootData.children }
    var currentBranchIndex by remember { mutableFloatStateOf(0f) }
    var lastFocusedBranchIdx by remember { mutableStateOf(-1) }

    // 业务状态：选中、访问
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val visitedNodes = remember { mutableStateListOf<String>() }
    var lastShownNodeId by remember { mutableStateOf<String?>(null) }

    // 详情面板高度（px）
    var detailPanelHeightPx by remember { mutableStateOf(0) }

    // Slider 拖动提示
    val sliderInteraction = remember { MutableInteractionSource() }
    val isDragging by sliderInteraction.collectIsDraggedAsState()

    val totalNodes = layoutData.size
    val selectedNode = selectedNodeId?.let { layoutData[it] }

    // 折叠状态：nodeId -> collapsed?
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }

    fun isCollapsed(id: String): Boolean = collapsed[id] == true

    fun collectSubtreeIds(startId: String): Set<String> {
        val result = LinkedHashSet<String>()
        val stack = ArrayDeque<String>()
        val seen = HashSet<String>()
        stack.add(startId)

        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (!seen.add(id)) continue
            result.add(id)
            val node = layoutData[id] ?: continue
            node.childrenIds.forEach { childId -> stack.add(childId) }
        }
        return result
    }

    fun isLeaf(id: String): Boolean = (layoutData[id]?.childrenIds?.isEmpty() != false)

    // 高亮集合
    val highlightEnabled = selectedNodeId != null
    val focusId = selectedNodeId
    val highlightSet by remember(focusId, layoutData, highlightEnabled) {
        derivedStateOf {
            if (!highlightEnabled || focusId == null) emptySet()
            else {
                if (isLeaf(focusId)) setOf(focusId) else collectSubtreeIds(focusId)
            }
        }
    }

    // 规则：父节点折叠 -> 子树全部不可见
    data class GridItem(val nodeId: String, val depth: Int, val parentId: String?)

    fun findNodeById(root: KnowledgeJson, targetId: String): KnowledgeJson? {
        if (root.id == targetId) return root
        root.children.forEach { c ->
            val r = findNodeById(c, targetId)
            if (r != null) return r
        }
        return null
    }

    fun buildVisibleGridItems(root: KnowledgeJson): List<GridItem> {
        val out = ArrayList<GridItem>(layoutData.size)

        fun dfs(node: KnowledgeJson, depth: Int, parentId: String?) {
            out.add(GridItem(node.id, depth, parentId))
            if (isCollapsed(node.id)) return
            node.children.forEach { child -> dfs(child, depth + 1, node.id) }
        }

        dfs(root, 0, null)
        return out
    }


    val visibleItems = remember(rootData, selectedNodeId, collapsed.toMap()) {
        val startRoot = if (selectedNodeId != null) {
            // 选中时：只显示该子树
            findNodeById(rootData, selectedNodeId!!) ?: rootData
        } else {
            // 未选中：显示整棵树
            rootData
        }
        buildVisibleGridItems(startRoot)
    }


    val itemsByDepth = remember(visibleItems) {
        visibleItems.groupBy { it.depth }.toSortedMap()
    }

    val maxDepth = itemsByDepth.keys.maxOrNull() ?: 0

    // 用于“吸附到页面左侧”：选中节点时，横向滚动把该 depth 列滚到最左
    val rowState = rememberLazyListState()

    fun depthOfNode(id: String): Int {
        // 通过 visibleItems 优先（可见时有），不可见时 fallback：从 rootData dfs 找 depth
        visibleItems.firstOrNull { it.nodeId == id }?.let { return it.depth }

        var found = -1
        fun find(node: KnowledgeJson, depth: Int) {
            if (found != -1) return
            if (node.id == id) {
                found = depth
                return
            }
            node.children.forEach { find(it, depth + 1) }
        }
        find(rootData, 0)
        return if (found == -1) 0 else found
    }

    val focusOnNode: (String) -> Unit = { id ->
        layoutData[id]?.let {
            coroutineScope.launch {
                if (!visitedNodes.contains(id)) visitedNodes.add(id)

                selectedNodeId = id
                lastShownNodeId = id

                // 吸附到左侧：把该节点所在列滚到最左侧
                val d = depthOfNode(id).coerceIn(0, maxDepth)
                rowState.animateScrollToItem(d)
            }
        }
    }

    // 关闭面板后高度归零
    LaunchedEffect(selectedNodeId) {
        if (selectedNodeId == null) detailPanelHeightPx = 0
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                AppTopBar(
                    title = rootData.title,
                    subtitle = "知识网格浏览",
                    progressText = "${visitedNodes.size}/$totalNodes"
                )

                if (mainBranches.isNotEmpty()) {
                    val currentIdx = currentBranchIndex.roundToInt().coerceIn(0, mainBranches.lastIndex)

                    val secondLevelNodes = remember(currentIdx, layoutData) {
                        val branchId = mainBranches[currentIdx].id
                        val childrenIds = layoutData[branchId]?.childrenIds ?: emptyList()
                        childrenIds.mapNotNull { layoutData[it] }
                    }

                    AppCard(
                        modifier = Modifier.padding(
                            horizontal = AppTokens.Space.l,
                            vertical = AppTokens.Space.s
                        )
                    ) {
                        AnimatedVisibility(visible = isDragging) {
                            AppTooltipCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                            ) {
                                Text(
                                    text = "当前分支：${mainBranches[currentIdx].title}（二级节点 ${secondLevelNodes.size}）",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(Modifier.height(AppTokens.Space.s))

                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    items(secondLevelNodes) { n ->
                                        Text(
                                            text = "• ${n.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = AppTokens.Space.xs)
                                        )
                                        Divider(color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(AppTokens.Space.s))
                            Text(
                                text = "快速跳至：${mainBranches[currentIdx].title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(AppTokens.Space.s))

                        Slider(
                            value = currentBranchIndex,
                            onValueChange = { v ->
                                currentBranchIndex = v
                                val idx = v.roundToInt().coerceIn(0, mainBranches.lastIndex)
                                if (idx != lastFocusedBranchIdx) {
                                    lastFocusedBranchIdx = idx
                                    focusOnNode(mainBranches[idx].id)
                                }
                            },
                            onValueChangeFinished = {
                                val idx = currentBranchIndex.roundToInt().coerceIn(0, mainBranches.lastIndex)
                                lastFocusedBranchIdx = idx
                                focusOnNode(mainBranches[idx].id)
                            },
                            valueRange = 0f..(mainBranches.size - 1).toFloat(),
                            steps = if (mainBranches.size > 1) mainBranches.size - 2 else 0,
                            interactionSource = sliderInteraction,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = if (totalNodes > 0) visitedNodes.size.toFloat() / totalNodes else 0f,
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTokens.SemanticColor.success
                )
            }
        }
    ) { padding ->
        val scheme = MaterialTheme.colorScheme
        val bg0 = scheme.background
        val bg1 = scheme.surfaceVariant.copy(alpha = 0.35f)
        val canvasBgBrush = remember(bg0, bg1) { Brush.verticalGradient(listOf(bg0, bg1)) }

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(canvasBgBrush)
        ) {
            // ===== 网格主体（按 depth 分列）=====
            LazyRow(
                state = rowState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = AppTokens.Space.l,
                        vertical = AppTokens.Space.m
                    ),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.Space.m)
            ) {
                items(count = maxDepth + 1, key = { it }) { depth ->
                    val itemsThisCol = itemsByDepth[depth].orEmpty()

                    Column(
                        modifier = Modifier
                            .widthIn(min = 260.dp, max = 340.dp)
                            .fillMaxHeight()
                    ) {
                        // 列标题：第0列=Root，其余=Level N
                        Text(
                            text = if (depth == 0) "Root" else "Level $depth",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = AppTokens.Space.s)
                        )

                        val colState = rememberLazyListState()

                        LazyColumn(
                            state = colState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(AppTokens.Space.s)
                        ) {
                            items(itemsThisCol, key = { it.nodeId }) { item ->
                                val pNode = layoutData[item.nodeId] ?: return@items

                                val highlighted = !highlightEnabled || highlightSet.contains(pNode.id)
                                val dimAlpha = AppTokens.Alpha.dim

                                GridNodeCard(
                                    pNode = pNode,
                                    isVisited = visitedNodes.contains(pNode.id),
                                    isSelected = selectedNodeId == pNode.id,
                                    isHighlighted = highlighted,
                                    dimAlpha = dimAlpha,
                                    isCollapsed = isCollapsed(pNode.id),
                                    canCollapse = pNode.childrenIds.isNotEmpty(),
                                    onToggleCollapse = {
                                        val willCollapse = !isCollapsed(pNode.id)
                                        collapsed[pNode.id] = willCollapse

                                        // 如果收起后把当前选中节点“藏起来”，就把选中提升到当前节点，避免 UI 无焦点
                                        if (willCollapse && selectedNodeId != null) {
                                            val subtree = collectSubtreeIds(pNode.id)
                                            if (selectedNodeId != pNode.id && subtree.contains(selectedNodeId!!)) {
                                                selectedNodeId = pNode.id
                                                lastShownNodeId = pNode.id
                                                coroutineScope.launch {
                                                    rowState.animateScrollToItem(depthOfNode(pNode.id))
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        // 分支同步到 Slider（仅一级分支有效）
                                        val branchIndex = mainBranches.indexOfFirst { branch -> branch.id == pNode.id }
                                        if (branchIndex != -1) {
                                            currentBranchIndex = branchIndex.toFloat()
                                            lastFocusedBranchIdx = branchIndex
                                        }
                                        focusOnNode(pNode.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ===== 详情面板=====
            AnimatedVisibility(
                visible = selectedNodeId != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { detailPanelHeightPx = it.height }
            ) {
                val nodeToShow = selectedNode
                    ?: (lastShownNodeId?.let { layoutData[it] })
                    ?: layoutData[rootData.id]

                nodeToShow?.let { node ->
                    DetailPanel(
                        node = node,
                        relatedTitle = layoutData[node.relatedNodeId]?.title,
                        onClose = { selectedNodeId = null },
                        onJump = { targetNodeId ->
                            val branchIndex = mainBranches.indexOfFirst { branch -> branch.id == targetNodeId }
                            if (branchIndex != -1) {
                                currentBranchIndex = branchIndex.toFloat()
                                lastFocusedBranchIdx = branchIndex
                            }
                            focusOnNode(targetNodeId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GridNodeCard(
    pNode: PositionedNode,
    isVisited: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean,
    dimAlpha: Float,
    isCollapsed: Boolean,
    canCollapse: Boolean,
    onToggleCollapse: () -> Unit,
    onClick: () -> Unit
) {
    val alpha = if (isHighlighted) 1f else dimAlpha

    val scheme = MaterialTheme.colorScheme
    val surfaceBase = scheme.surface
    val surfaceVisited = lerp(surfaceBase, scheme.primaryContainer, 0.65f)
    val surfaceBottom = lerp(scheme.surfaceVariant, surfaceBase, 0.55f).copy(alpha = 0.90f)

    val bgBrush = Brush.verticalGradient(
        listOf(
            if (isVisited) surfaceVisited else surfaceBase,
            surfaceBottom
        )
    )

    val shape = RoundedCornerShape(AppTokens.Radius.l)

    val rot by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 90f, // 右箭头旋转为“下/展开”视觉
        animationSpec = tween(180),
        label = "collapseRot"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .drawBehind {
                if (isSelected) {
                    val glow = Brush.radialGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.maxDimension * 0.85f
                    )
                    val cr = AppTokens.Radius.l.toPx()
                    drawRoundRect(
                        brush = glow,
                        cornerRadius = CornerRadius(cr, cr)
                    )
                }
            }
            .shadow(
                elevation = when {
                    isSelected -> AppTokens.Elevation.overlay
                    isHighlighted -> AppTokens.Elevation.card
                    else -> 0.dp
                },
                shape = shape
            )
            .background(bgBrush, shape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                    isSelected -> scheme.primary.copy(alpha = 0.95f)
                    isHighlighted -> scheme.outlineVariant.copy(alpha = 0.85f)
                    else -> scheme.outlineVariant.copy(alpha = 0.40f)
                },
                shape = shape
            )
            .clickable { onClick() }
            .padding(AppTokens.Space.m)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧强调条
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .width(4.dp)
                    .background(
                        color = when {
                            isSelected -> scheme.primary
                            isVisited -> scheme.primary.copy(alpha = 0.70f)
                            else -> scheme.outlineVariant
                        },
                        shape = RoundedCornerShape(99.dp)
                    )
            )

            Spacer(Modifier.width(AppTokens.Space.s))

            Text(
                text = pNode.title,
                modifier = Modifier.weight(1f),
                color = scheme.onSurface.copy(alpha = alpha),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 2
            )

            Spacer(Modifier.width(AppTokens.Space.s))

            // 折叠三角（仅有子节点时显示）
            if (canCollapse) {
                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.rotate(rot)
                    )
                }
            }
        }

        // 访问角标
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(7.dp)
                .background(
                    color = if (isVisited) scheme.primary.copy(alpha = 0.70f)
                    else scheme.outlineVariant.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(99.dp)
                )
        )
    }
}

@Composable
fun DetailPanel(
    node: PositionedNode,
    relatedTitle: String?,
    onClose: () -> Unit,
    onJump: (String) -> Unit
) {
    BackHandler { onClose() }

    AppBottomPanel(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = node.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        }

        Spacer(Modifier.height(AppTokens.Space.l))

        MathJaxWebView(
            content = node.content,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Spacer(Modifier.height(AppTokens.Space.l))

        if (node.relatedNodeId != null) {
            Button(
                onClick = { onJump(node.relatedNodeId) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(AppTokens.Radius.m)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(AppTokens.Space.s))
                Text("跳转到关联点: $relatedTitle")
            }
        }
    }
}

@Composable
fun MathJaxWebView(content: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val textColor = scheme.onSurface.toCssHex()
    val linkColor = scheme.primary.toCssHex()
    val mutedColor = scheme.onSurfaceVariant.toCssHex()

    val htmlData = remember(content, textColor, linkColor, mutedColor) {
        """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <script type="text/x-mathjax-config">
                MathJax.Hub.Config({
                    tex2jax: {inlineMath: [['$','$'], ['\\(','\\)']]},
                    displayAlign: "left",
                    CommonHTML: { linebreaks: { automatic: true } }
                });
            </script>
            <script type="text/javascript" async
                src="https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.7/MathJax.js?config=TeX-MML-AM_CHTML">
            </script>
            <style>
                body { font-family: sans-serif; font-size: 16px; line-height: 1.6; color: ${'$'}textColor; background-color: transparent; }
                a { color: ${'$'}linkColor; }
                .muted { color: ${'$'}mutedColor; }
                .MathJax_Display { margin: 1em 0 !important; overflow-x: auto; overflow-y: hidden; }
            </style>
        </head>
        <body>
            ${content.replace("\n", "<br>")}
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://cdnjs.cloudflare.com/",
                htmlData,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}

private fun Color.toCssHex(): String {
    val rgb = this.toArgb() and 0x00FFFFFF
    return String.format("#%06X", rgb)
}
