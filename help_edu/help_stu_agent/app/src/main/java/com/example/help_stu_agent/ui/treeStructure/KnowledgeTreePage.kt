package com.example.help_stu_agent.ui.treeStructure

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.animateContentSize
import com.example.help_stu_agent.designsystem.components.AppBottomPanel
import com.example.help_stu_agent.designsystem.components.AppCard
import com.example.help_stu_agent.designsystem.components.AppTopBar
import com.example.help_stu_agent.designsystem.components.AppTooltipCard
import com.example.help_stu_agent.designsystem.tokens.AppTokens
import com.example.help_stu_agent.ui.theme.BranchPaletteDark
import com.example.help_stu_agent.ui.theme.BranchPaletteLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import android.annotation.SuppressLint
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.help_stu_agent.data.repo.KnowledgeTreeRepository
import com.example.help_stu_agent.ui.uploadPdf.PdfTreeCache

@Composable
fun KnowledgeTreePageFromId(treeId: String) {
    val context = LocalContext.current
    val repo = remember { KnowledgeTreeRepository(context) }

    var loading by remember { mutableStateOf(true) }
    var json by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(treeId) {
        loading = true
        json = withContext(Dispatchers.IO) { repo.loadJsonById(treeId) }
        // 可选：顺便回填缓存，便于你其它地方仍用 FromCache 也能显示
        PdfTreeCache.latestJson = json
        loading = false
    }

    when {
        loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        json.isNullOrBlank() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("该知识树记录不存在或内容已被删除")
            }
        }
        else -> {
            KnowledgeTreePageFromJson(json!!)
        }
    }
}

@Composable
fun KnowledgeTreePageFromJson(jsonContent: String) {
    KnowledgeTree(jsonContent)
}

@Composable
fun KnowledgeTreePageFromCache() {
    val json = PdfTreeCache.latestJson
    if (!json.isNullOrBlank()) {
        KnowledgeTreePageFromJson(json)
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("没有可展示的知识树，请先上传 PDF")
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

    // 详情面板高度（px）——保留（不影响功能）
    var detailPanelHeightPx by remember { mutableStateOf(0) }

    // Slider 拖动提示
    val sliderInteraction = remember { MutableInteractionSource() }
    val isDragging by sliderInteraction.collectIsDraggedAsState()

    val totalNodes = layoutData.size

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

    // 颜色：一级子树区分
    val isDark = isSystemInDarkTheme()
    val branchPalette = remember(isDark) { if (isDark) BranchPaletteDark else BranchPaletteLight }

    // topLevelMap：nodeId -> root 的哪个一级 child（用于上色）
    val topLevelMap = remember(rootData) {
        val map = HashMap<String, String?>()

        fun dfs(node: KnowledgeJson, topLevel: String?) {
            map[node.id] = topLevel
            node.children.forEach { child ->
                val nextTop = topLevel ?: child.id // root 的孩子作为 topLevel
                dfs(child, nextTop)
            }
        }

        // root 自己 topLevel = null
        map[rootData.id] = null
        // root 的孩子：topLevel = 自己
        rootData.children.forEach { c -> dfs(c, c.id) }

        map
    }

    val branchColorMap = remember(mainBranches, branchPalette) {
        val m = HashMap<String, Color>()
        mainBranches.forEachIndexed { idx, branch ->
            m[branch.id] = branchPalette[idx % branchPalette.size]
        }
        m
    }

    @Composable
    fun accentColorFor(nodeId: String): Color {
        val scheme = MaterialTheme.colorScheme
        if (nodeId == rootData.id) return scheme.primary
        val top = topLevelMap[nodeId]
        return if (top != null) branchColorMap[top] ?: scheme.primary else scheme.primary
    }

    // parentMap：用于“同级节点切换”（必须基于全树，而不是 visibleItems 的子树）
    val parentMap = remember(rootData) {
        val m = HashMap<String, String?>()
        fun dfs(node: KnowledgeJson, parentId: String?) {
            m[node.id] = parentId
            node.children.forEach { child -> dfs(child, node.id) }
        }
        dfs(rootData, null)
        m
    }

    fun findNodeById(root: KnowledgeJson, targetId: String): KnowledgeJson? {
        if (root.id == targetId) return root
        root.children.forEach { c ->
            val r = findNodeById(c, targetId)
            if (r != null) return r
        }
        return null
    }

    fun depthInFullTree(id: String): Int {
        var found = -1
        fun dfs(node: KnowledgeJson, depth: Int) {
            if (found != -1) return
            if (node.id == id) {
                found = depth
                return
            }
            node.children.forEach { dfs(it, depth + 1) }
        }
        dfs(rootData, 0)
        return if (found == -1) 0 else found
    }

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
        // visibleItems 优先（可见时有），不可见时 fallback：从 rootData dfs 找 depth
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

                // 吸附到左侧：把该节点所在列滚到最左侧（选中后是“子树视图”，滚到 0 列符合原有逻辑）
                val d = depthOfNode(id).coerceIn(0, maxDepth)
                rowState.animateScrollToItem(d)
            }
        }
    }

    // 关闭面板后高度归零
    LaunchedEffect(selectedNodeId) {
        if (selectedNodeId == null) detailPanelHeightPx = 0
    }

    // === BottomSheetScaffold 相关：半屏 peekHeight（按屏幕高度 55% 近似半屏） ===
    val config = LocalConfiguration.current
    val halfPeekHeight = remember(config) { (config.screenHeightDp * 0.55f).dp }

    // IME 可见（用来：满屏输入时锁定 Expanded，不允许掉回半屏）
    val density = LocalDensity.current
    val imeVisibleNow = WindowInsets.ime.getBottom(density) > 0
    val imeVisibleState = remember { mutableStateOf(false) }
    SideEffect { imeVisibleState.value = imeVisibleNow }

    // 当前节点 id（用于隐藏时定位回该节点）
    val currentIdForCloseState = rememberUpdatedState(
        selectedNodeId ?: lastShownNodeId ?: rootData.id
    )

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
        confirmValueChange = { newValue ->
            // 关键：当键盘可见时，锁定 Expanded，避免满屏输入掉回半屏/隐藏
            if (imeVisibleState.value && newValue != SheetValue.Expanded) return@rememberStandardBottomSheetState false

            if (newValue == SheetValue.Hidden) {
                coroutineScope.launch {
                    val id = currentIdForCloseState.value
                    val dFull = depthInFullTree(id).coerceAtLeast(0)
                    selectedNodeId = null
                    rowState.animateScrollToItem(dFull)
                }
            }
            true
        }
    )

    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    // 选中节点时：第一次打开 -> 半屏；已打开时切换节点 -> 保持当前 sheet 状态不变
    var lastSelectionForSheet by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedNodeId) {
        if (selectedNodeId == null) {
            lastSelectionForSheet = null
            if (sheetState.currentValue != SheetValue.Hidden) sheetState.hide()
        } else {
            // 第一次从 null -> 非 null：半屏出现
            if (lastSelectionForSheet == null) {
                sheetState.partialExpand()
            }
            lastSelectionForSheet = selectedNodeId
        }
    }

    // 若已在满屏且键盘弹出：保持满屏（冗余保险）
    val isExpanded = sheetState.currentValue == SheetValue.Expanded
    LaunchedEffect(imeVisibleNow, isExpanded) {
        if (imeVisibleNow && isExpanded && sheetState.currentValue != SheetValue.Expanded) {
            sheetState.expand()
        }
    }

    Scaffold(
        topBar = {
            // ✅ 全屏(Expanded)时隐藏顶部区域（包含进度条、slider、提示卡等）
            AnimatedVisibility(visible = !isExpanded) {
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
        }
    ) { padding ->
        val scheme = MaterialTheme.colorScheme
        val bg0 = scheme.background
        val bg1 = scheme.surfaceVariant.copy(alpha = 0.35f)
        val canvasBgBrush = remember(bg0, bg1) { Brush.verticalGradient(listOf(bg0, bg1)) }

        BottomSheetScaffold(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            scaffoldState = scaffoldState,
            sheetPeekHeight = if (selectedNodeId != null) halfPeekHeight else 0.dp,
            // 关键：让 sheet 本体不要吃 insets（尤其 IME），由内容层 imePadding 处理
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            sheetContent = {
                if (selectedNodeId == null) {
                    Spacer(Modifier.height(1.dp))
                    return@BottomSheetScaffold
                }

                val currentId = selectedNodeId!!
                val currentIdState = rememberUpdatedState(currentId)

                // 同级 siblings：基于“全树 parentMap + parent.children”，不受子树视图影响
                val siblingIds = remember(currentId, parentMap, rootData) {
                    val parentId = parentMap[currentId]
                    if (parentId == null) {
                        listOf(currentId) // root 无同级
                    } else {
                        val parentNode = findNodeById(rootData, parentId)
                        val siblings = parentNode?.children?.map { it.id }.orEmpty()
                        if (siblings.isEmpty()) listOf(currentId) else siblings
                    }
                }

                val initialPage = remember(currentId, siblingIds) {
                    siblingIds.indexOf(currentId).let { if (it >= 0) it else 0 }
                }

                val pagerState = rememberPagerState(
                    initialPage = initialPage,
                    pageCount = { siblingIds.size }
                )

                var suppressPagerSelectionSync by remember { mutableStateOf(false) }

                // 保证：点击进入/外部更新选中节点时，Pager 跟随到正确 page
                LaunchedEffect(currentId, siblingIds) {
                    val idx = siblingIds.indexOf(currentId).let { if (it >= 0) it else 0 }
                    if (pagerState.currentPage != idx) {
                        suppressPagerSelectionSync = true
                        pagerState.scrollToPage(idx)
                        suppressPagerSelectionSync = false
                    }
                }

                // 全屏时：左右滑切页 -> 同级节点切换（保持原逻辑）
                LaunchedEffect(pagerState.currentPage, siblingIds, isExpanded) {
                    if (!isExpanded) return@LaunchedEffect
                    if (suppressPagerSelectionSync) return@LaunchedEffect
                    val targetId = siblingIds.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
                    if (targetId != selectedNodeId) {
                        focusOnNode(targetId)
                    }
                }

                val requestExpandToFull: () -> Unit = {
                    coroutineScope.launch { sheetState.expand() }
                }

                // ✅ 内容层处理 IME：保证键盘弹出时输入区上移并贴键盘
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    DetailPanelPager(
                        layoutData = layoutData,
                        siblingIds = siblingIds,
                        pagerState = pagerState,
                        userScrollEnabled = isExpanded && siblingIds.size > 1,
                        onRequestExpand = requestExpandToFull,
                        onClose = {
                            coroutineScope.launch {
                                val id = currentIdState.value
                                val dFull = depthInFullTree(id).coerceAtLeast(0)
                                selectedNodeId = null
                                rowState.animateScrollToItem(dFull)
                                sheetState.hide()
                            }
                        },
                        onJump = { targetNodeId ->
                            val branchIndex = mainBranches.indexOfFirst { branch -> branch.id == targetNodeId }
                            if (branchIndex != -1) {
                                currentBranchIndex = branchIndex.toFloat()
                                lastFocusedBranchIdx = branchIndex
                            }
                            focusOnNode(targetNodeId)
                        },
                        onPanelSizeChanged = { h -> detailPanelHeightPx = h },
                        showSwipeHint = isExpanded && siblingIds.size > 1
                    )
                }
            },
            // 让 sheet 的 drag handle 保留（你需要上拉满屏）
            sheetDragHandle = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .padding(top = 10.dp, bottom = 12.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(999.dp)
                            )
                    )
                }
            }

        ) { innerPadding ->
            // ===== 网格主体（按 depth 分列）=====
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(canvasBgBrush)
            ) {
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

                                    val accent = accentColorFor(pNode.id)

                                    val hasFocus = selectedNodeId != null
                                    val accentStrength = when {
                                        !hasFocus -> 0.35f
                                        highlighted -> 1.0f
                                        else -> 0.15f
                                    }

                                    GridNodeCard(
                                        pNode = pNode,
                                        accentColor = accent,
                                        accentStrength = accentStrength,
                                        isVisited = visitedNodes.contains(pNode.id),
                                        isSelected = selectedNodeId == pNode.id,
                                        isHighlighted = highlighted,
                                        dimAlpha = dimAlpha,
                                        isCollapsed = isCollapsed(pNode.id),
                                        canCollapse = pNode.childrenIds.isNotEmpty(),
                                        onToggleCollapse = {
                                            val willCollapse = !isCollapsed(pNode.id)
                                            collapsed[pNode.id] = willCollapse

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
                                            val branchIndex =
                                                mainBranches.indexOfFirst { branch -> branch.id == pNode.id }
                                            if (branchIndex != -1) {
                                                currentBranchIndex = branchIndex.toFloat()
                                                lastFocusedBranchIdx = branchIndex
                                            }
                                            // ✅ 半屏状态下点下一级节点：直接切到该节点内容，不会 dismiss 回父节点（因为已非模态）
                                            focusOnNode(pNode.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridNodeCard(
    pNode: PositionedNode,
    accentColor: Color,
    accentStrength: Float,
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
    val surfaceBottom = lerp(scheme.surfaceVariant, surfaceBase, 0.55f).copy(alpha = 0.90f)

    val surfaceVisited = lerp(surfaceBase, scheme.primaryContainer, 0.65f)

    val tintTop = (0.06f + 0.08f * accentStrength).coerceIn(0.05f, 0.16f)
    val tintBottom = (0.03f + 0.05f * accentStrength).coerceIn(0.03f, 0.10f)

    val topBase = if (isVisited) surfaceVisited else surfaceBase
    val topTinted = lerp(topBase, accentColor, tintTop)
    val bottomTinted = lerp(surfaceBottom, accentColor, tintBottom)

    val bgBrush = Brush.verticalGradient(listOf(topTinted, bottomTinted))

    val shape = RoundedCornerShape(AppTokens.Radius.l)

    val rot by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 90f,
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
                            accentColor.copy(alpha = 0.18f),
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
                    isSelected -> accentColor.copy(alpha = 0.95f)
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
            val railAlpha = when {
                isSelected -> 1.0f
                isVisited -> 0.75f + 0.15f * accentStrength
                else -> 0.22f + 0.55f * accentStrength
            }.coerceIn(0.18f, 1.0f)

            Box(
                modifier = Modifier
                    .height(44.dp)
                    .width(4.dp)
                    .background(
                        color = if (isSelected) accentColor else accentColor.copy(alpha = railAlpha),
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

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(7.dp)
                .background(
                    color = if (isVisited) accentColor.copy(alpha = 0.75f)
                    else scheme.outlineVariant.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(99.dp)
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailPanelPager(
    layoutData: Map<String, PositionedNode>,
    siblingIds: List<String>,
    pagerState: PagerState,
    userScrollEnabled: Boolean,
    onRequestExpand: () -> Unit,
    onClose: () -> Unit,
    onJump: (String) -> Unit,
    onPanelSizeChanged: (Int) -> Unit,
    showSwipeHint: Boolean
) {
    BackHandler { onClose() }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = userScrollEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) { page ->
        val id = siblingIds.getOrNull(page) ?: return@HorizontalPager
        val node = layoutData[id] ?: return@HorizontalPager
        val relatedTitle = node.relatedNodeId?.let { layoutData[it]?.title }

        AppBottomPanel(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .onSizeChanged { onPanelSizeChanged(it.height) }
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

            if (showSwipeHint) {
                Spacer(Modifier.height(AppTokens.Space.s))
                Text(
                    text = "左右滑动：切换同级节点（${page + 1}/${siblingIds.size}）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(AppTokens.Space.l))

            MathJaxWebView(
                content = node.content,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // ✅ 满屏时输入：键盘弹出贴边 + 保持满屏
            DeepSeekChatInlinePanel(
                nodeId = node.id,
                nodeTitle = node.title,
                nodeContentForContext = node.content,
                onRequestExpand = onRequestExpand,
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
                    tex2jax: {inlineMath: [['$','$'], ['\(','\)']]},
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

/**
 * - 输入框聚焦时：若未满屏，先请求 expand（用户体验：开始输入=进入“问答模式”）
 * - 组合 imePadding（在 sheetContent 外层已加），确保贴键盘
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeepSeekChatInlinePanel(
    nodeId: String,
    nodeTitle: String,
    nodeContentForContext: String,
    onRequestExpand: () -> Unit,
    vm: NodeChatViewModel = viewModel()
) {
    vm.ensureThread(nodeId)
    val thread = vm.threads.getValue(nodeId)

    var input by remember(nodeId, nodeTitle) {
        val prefix = "关于「$nodeTitle」："
        mutableStateOf(TextFieldValue(prefix, selection = TextRange(prefix.length)))
    }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun doSend() {
        val t = input.text.trim()
        if (t.isNotEmpty() && t != "关于「$nodeTitle」：") {
            vm.send(nodeId, t, nodeContentForContext)

            keyboardController?.hide()
            focusManager.clearFocus(force = true)

            val prefix = "关于「$nodeTitle」："
            input = TextFieldValue(prefix, selection = TextRange(prefix.length))
        }
    }

    val cfg = LocalConfiguration.current
    val maxPanelHeight = remember(cfg) { (cfg.screenHeightDp * 0.50f).dp }
    val density = LocalDensity.current

    var topChromePx by remember { mutableIntStateOf(0) }
    var bottomChromePx by remember { mutableIntStateOf(0) }

    val messagesMaxHeight = remember(maxPanelHeight, topChromePx, bottomChromePx, density) {
        with(density) {
            val maxPx = maxPanelHeight.roundToPx()
            val msgPx = (maxPx - topChromePx - bottomChromePx).coerceAtLeast(96.dp.roundToPx())
            msgPx.toDp()
        }
    }

    Surface(
        shape = RoundedCornerShape(AppTokens.Radius.l),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxPanelHeight)
                .animateContentSize()
                .padding(AppTokens.Space.m)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AI 问答",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { vm.clear(nodeId); vm.ensureThread(nodeId) }) { Text("清空") }
            }

            Spacer(Modifier.height(AppTokens.Space.s))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = messagesMaxHeight),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(thread) { m ->
                    if (m.role == "system") return@items
                    ChatBubbleMarkdown(role = m.role, text = m.text)
                }
            }

            Spacer(Modifier.height(AppTokens.Space.s))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusChanged { st ->
                            if (st.isFocused) {
                                // 满屏输入：保持满屏；半屏输入：自动进入满屏
                                onRequestExpand()
                                scope.launch {
                                    // 把输入框强制搬到键盘上方（彻底消除遮挡）
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    placeholder = { Text("输入问题…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            doSend()
                        }
                    )
                )
                Spacer(Modifier.width(AppTokens.Space.s))
                Button(
                    onClick = {
                        doSend()
                    }
                ) {
                    Text("发送")
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleMarkdown(role: String, text: String) {
    val isUser = role == "user"
    val bg = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
            val textCss = fg.toCssHex()
            val linkCss = MaterialTheme.colorScheme.primary.toCssHex()
            val codeBgCss = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f).toCssHex()

            MarkdownMathJaxWebView(
                markdown = text,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(),
                textColorCss = textCss,
                linkColorCss = linkCss,
                codeBgCss = codeBgCss,
                bubbleBgCss = bg.toCssHex()
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownMathJaxWebView(
    markdown: String,
    modifier: Modifier = Modifier,
    textColorCss: String,
    linkColorCss: String,
    codeBgCss: String,
    bubbleBgCss: String
) {
    val density = LocalDensity.current
    var heightDp by remember { mutableStateOf<Dp>(1.dp) }

    val parser = remember {
        Parser.builder()
            .extensions(listOf(TablesExtension.create(), TaskListItemsExtension.create()))
            .build()
    }
    val renderer = remember {
        HtmlRenderer.builder().escapeHtml(true).build()
    }

    // --- 核心修改：预处理 Markdown 内容 ---
    val html = remember(markdown, textColorCss, linkColorCss, codeBgCss, bubbleBgCss) {
        // 1. 处理反斜杠，防止 CommonMark 吞掉 LaTeX 标识符
        // 将 \( 替换为 \\(，将 \[ 替换为 \\[
        val preProcessed = markdown
            .replace("\\(", "\\\\(")
            .replace("\\)", "\\\\)")
            .replace("\\[", "\\\\[")
            .replace("\\]", "\\\\]")

        val htmlBody = renderer.render(parser.parse(preProcessed))

        """
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
          <style>
            body {
              margin:0; padding:0;
              color: $textColorCss;
              background: transparent;
              font-family: sans-serif;
              line-height: 1.55;
              font-size: 15px;
            }
            /* 防止公式溢出 */
            .MathJax_Display { overflow-x: auto; overflow-y: hidden; }
            pre { background: $codeBgCss; padding: 10px; border-radius: 10px; overflow-x: auto; }
          </style>

          <script type="text/x-mathjax-config">
            MathJax.Hub.Config({
              messageStyle: "none",
              tex2jax: { 
                inlineMath: [['$','$'], ['\\(','\\)']], 
                displayMath: [['$$','$$'], ['\\[','\\]']],
                processEscapes: true 
              },
              CommonHTML: { linebreaks: { automatic: true } }
            });
          </script>
          <script type="text/javascript" async
            src="https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.7/MathJax.js?config=TeX-MML-AM_CHTML">
          </script>
        </head>
        <body>
          <div id="content">$htmlBody</div>
          <script>
            function reportHeight() {
              var h = document.body.scrollHeight;
              return h;
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier.height(heightDp), // 确保高度生效
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        // 给 MathJax 留出排版时间后测量高度
                        view.postDelayed({
                            view.evaluateJavascript("reportHeight()") { raw ->
                                val v = raw?.trim()?.trim('"')?.toFloatOrNull()
                                if (v != null) {
                                    heightDp = with(density) { v.toDp() }
                                }
                            }
                        }, 600)
                    }
                }
            }
        },
        update = { wv ->
            wv.loadDataWithBaseURL("https://cdnjs.cloudflare.com/", html, "text/html", "UTF-8", null)
        }
    )
}