package com.example.help_stu_agent.ui.treeStructure

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.help_stu_agent.data.repo.KnowledgeTreeRepository
import com.example.help_stu_agent.designsystem.components.AppCard
import com.example.help_stu_agent.designsystem.components.AppTooltipCard
import com.example.help_stu_agent.designsystem.components.AppTopBar
import com.example.help_stu_agent.designsystem.tokens.AppTokens
import com.example.help_stu_agent.ui.theme.BranchPaletteDark
import com.example.help_stu_agent.ui.theme.BranchPaletteLight
import com.example.help_stu_agent.ui.uploadPdf.PdfTreeCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

@Composable
fun KnowledgeTreePageFromId(treeId: String) {
    val context = LocalContext.current
    val repo = remember { KnowledgeTreeRepository(context) }

    var loading by remember { mutableStateOf(true) }
    var json by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(treeId) {
        loading = true
        json = withContext(Dispatchers.IO) { repo.loadJsonById(treeId) }
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
    var currentBranchIndex by rememberSaveable { mutableFloatStateOf(0f) }
    var lastFocusedBranchIdx by rememberSaveable { mutableStateOf(-1) }

    // 业务状态：选中、访问
    var selectedNodeId by rememberSaveable { mutableStateOf<String?>(null) }
    
    val visitedNodes = rememberSaveable(saver = listSaver(
        save = { it.toList() },
        restore = { mutableStateListOf(*it.toTypedArray()) }
    )) { mutableStateListOf<String>() }
    
    var lastShownNodeId by rememberSaveable { mutableStateOf<String?>(null) }

    // 详情面板高度（px）
    var detailPanelHeightPx by remember { mutableStateOf(0) }

    // Slider 拖动提示
    val sliderInteraction = remember { MutableInteractionSource() }
    val isDragging by sliderInteraction.collectIsDraggedAsState()

    val totalNodes = layoutData.size

    // 折叠状态：nodeId -> collapsed?
    val collapsed = rememberSaveable(saver = mapSaver(
        save = { it.toMap() },
        restore = { mutableStateMapOf<String, Boolean>().apply { putAll(it as Map<String, Boolean>) } }
    )) { mutableStateMapOf<String, Boolean>() }

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
                val nextTop = topLevel ?: child.id 
                dfs(child, nextTop)
            }
        }

        map[rootData.id] = null
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

    // parentMap：用于“同级节点切换”
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
            findNodeById(rootData, selectedNodeId!!) ?: rootData
        } else {
            rootData
        }
        buildVisibleGridItems(startRoot)
    }

    val itemsByDepth = remember(visibleItems) {
        visibleItems.groupBy { it.depth }.toSortedMap()
    }

    val maxDepth = itemsByDepth.keys.maxOrNull() ?: 0

    // 用于“吸附到页面左侧”
    val rowState = rememberLazyListState()

    fun depthOfNode(id: String): Int {
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
                val d = depthOfNode(id).coerceIn(0, maxDepth)
                rowState.animateScrollToItem(d)
            }
        }
    }

    LaunchedEffect(selectedNodeId) {
        if (selectedNodeId == null) detailPanelHeightPx = 0
    }

    val config = LocalConfiguration.current
    val halfPeekHeight = remember(config) { (config.screenHeightDp * 0.55f).dp }

    val density = LocalDensity.current
    val imeVisibleNow = WindowInsets.ime.getBottom(density) > 0
    val imeVisibleState = remember { mutableStateOf(false) }
    SideEffect { imeVisibleState.value = imeVisibleNow }

    val currentIdForCloseState = rememberUpdatedState(
        selectedNodeId ?: lastShownNodeId ?: rootData.id
    )

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
        confirmValueChange = { newValue ->
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

    var lastSelectionForSheet by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedNodeId) {
        if (selectedNodeId == null) {
            lastSelectionForSheet = null
            if (sheetState.currentValue != SheetValue.Hidden) sheetState.hide()
        } else {
            if (lastSelectionForSheet == null) {
                sheetState.partialExpand()
            }
            lastSelectionForSheet = selectedNodeId
        }
    }

    val isExpanded = sheetState.currentValue == SheetValue.Expanded
    LaunchedEffect(imeVisibleNow, isExpanded) {
        if (imeVisibleNow && isExpanded && sheetState.currentValue != SheetValue.Expanded) {
            sheetState.expand()
        }
    }

    Scaffold(
        topBar = {
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
                                    null,
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
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            sheetContent = {
                if (selectedNodeId == null) {
                    Spacer(Modifier.height(1.dp))
                    return@BottomSheetScaffold
                }

                val currentId = selectedNodeId!!
                val currentIdState = rememberUpdatedState(currentId)

                val siblingIds = remember(currentId, parentMap, rootData) {
                    val parentId = parentMap[currentId]
                    if (parentId == null) {
                        listOf(currentId)
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

                LaunchedEffect(currentId, siblingIds) {
                    val idx = siblingIds.indexOf(currentId).let { if (it >= 0) it else 0 }
                    if (pagerState.currentPage != idx) {
                        suppressPagerSelectionSync = true
                        pagerState.scrollToPage(idx)
                        suppressPagerSelectionSync = false
                    }
                }

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

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(AppTokens.Space.s)
                            ) {
                                items(itemsThisCol, key = { it.nodeId }) { item ->
                                    val pNode = layoutData[item.nodeId] ?: return@items
                                    val highlighted = !highlightEnabled || highlightSet.contains(pNode.id)
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
                                        dimAlpha = AppTokens.Alpha.dim,
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
            }
        }
    }
}
