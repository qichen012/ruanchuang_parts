package com.example.help_stu_agent.ui.treeStructure

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.math.max

@OptIn(InternalSerializationApi::class)
@Serializable
data class KnowledgeJson(
    val id: String,
    val title: String,
    val content: String,
    val relatedNodeId: String? = null,
    val children: List<KnowledgeJson> = emptyList()
)

enum class TreeLayoutMode {
    TOP_DOWN,
    MINDMAP_VERTICAL,
}

// 供 UI 使用的定位节点模型（网格版仍沿用：用于详情面板、关联跳转、childrenIds 等）
data class PositionedNode(
    val id: String,
    val title: String,
    val content: String,
    val relatedNodeId: String?,
    val x: Float,
    val y: Float,
    val childrenIds: List<String>
)

class TreeLayoutEngine(
    private val mode: TreeLayoutMode = TreeLayoutMode.MINDMAP_VERTICAL
) {
    // ====== TOP_DOWN（原逻辑）常量 ======
    private val tdNodeWidth = 400f
    private val tdHorizontalGap = 100f
    private val tdVerticalGap = 450f

    // ====== MINDMAP_VERTICAL（纵向思维导图：只右侧展开）常量 ======
    private val mmRootY = 200f
    private val mmNodeBoxH = 160f
    private val mmChildGapY = 120f
    private val mmBranchGapY = 180f
    private val mmLevelGapX = 520f
    private val mmRootGapX = 420f

    fun calculate(root: KnowledgeJson): Map<String, PositionedNode> {
        return when (mode) {
            TreeLayoutMode.TOP_DOWN -> calculateTopDown(root)
            TreeLayoutMode.MINDMAP_VERTICAL -> calculateMindMapVertical(root)
        }
    }

    private fun calculateTopDown(root: KnowledgeJson): Map<String, PositionedNode> {
        val result = mutableMapOf<String, PositionedNode>()

        fun layoutRecursive(node: KnowledgeJson, depth: Int, startX: Float): Float {
            if (node.children.isEmpty()) {
                val x = startX + tdNodeWidth / 2
                val y = depth * tdVerticalGap + 200f
                result[node.id] = PositionedNode(
                    id = node.id,
                    title = node.title,
                    content = node.content,
                    relatedNodeId = node.relatedNodeId,
                    x = x,
                    y = y,
                    childrenIds = emptyList()
                )
                return tdNodeWidth + tdHorizontalGap
            }

            var currentSubX = startX
            val childIds = mutableListOf<String>()
            node.children.forEach { child ->
                val widthUsed = layoutRecursive(child, depth + 1, currentSubX)
                currentSubX += widthUsed
                childIds.add(child.id)
            }

            val firstChildX = result[node.children.first().id]!!.x
            val lastChildX = result[node.children.last().id]!!.x
            val parentX = (firstChildX + lastChildX) / 2
            val parentY = depth * tdVerticalGap + 200f

            result[node.id] = PositionedNode(
                id = node.id,
                title = node.title,
                content = node.content,
                relatedNodeId = node.relatedNodeId,
                x = parentX,
                y = parentY,
                childrenIds = childIds
            )

            return (currentSubX - startX)
        }

        layoutRecursive(root, 0, 0f)
        return result
    }

    // 纵向思维导图：根在左，所有分支只在右侧展开并纵向排开
    private fun calculateMindMapVertical(root: KnowledgeJson): Map<String, PositionedNode> {
        val result = mutableMapOf<String, PositionedNode>()

        result[root.id] = PositionedNode(
            id = root.id,
            title = root.title,
            content = root.content,
            relatedNodeId = root.relatedNodeId,
            x = 0f,
            y = mmRootY,
            childrenIds = root.children.map { it.id }
        )

        if (root.children.isEmpty()) return result

        val right = root.children
        val rightTotalH = measureForestHeight(right, mmBranchGapY)
        val rightStartTop = mmRootY - rightTotalH / 2f

        layoutForestSide(
            roots = right,
            dir = +1,
            startTop = rightStartTop,
            result = result
        )

        return result
    }

    private fun measureForestHeight(roots: List<KnowledgeJson>, gap: Float): Float {
        if (roots.isEmpty()) return 0f
        val sum = roots.sumOf { measureSubtreeHeight(it).toDouble() }.toFloat()
        return sum + gap * max(0, roots.size - 1)
    }

    private fun measureSubtreeHeight(node: KnowledgeJson): Float {
        if (node.children.isEmpty()) return mmNodeBoxH
        val childHeights = node.children.map { measureSubtreeHeight(it) }
        val totalChildren = childHeights.sum() + mmChildGapY * max(0, node.children.size - 1)
        return max(mmNodeBoxH, totalChildren)
    }

    private fun layoutForestSide(
        roots: List<KnowledgeJson>,
        dir: Int,
        startTop: Float,
        result: MutableMap<String, PositionedNode>
    ) {
        var cursorTop = startTop
        roots.forEach { r ->
            val h = measureSubtreeHeight(r)
            layoutSubtreeSide(
                node = r,
                depth = 1,
                dir = dir,
                top = cursorTop,
                height = h,
                result = result
            )
            cursorTop += h + mmBranchGapY
        }
    }

    private fun layoutSubtreeSide(
        node: KnowledgeJson,
        depth: Int,
        dir: Int,
        top: Float,
        height: Float,
        result: MutableMap<String, PositionedNode>
    ) {
        val x = dir * (mmRootGapX + (depth - 1) * mmLevelGapX)

        if (node.children.isEmpty()) {
            val y = top + height / 2f
            result[node.id] = PositionedNode(
                id = node.id,
                title = node.title,
                content = node.content,
                relatedNodeId = node.relatedNodeId,
                x = x,
                y = y,
                childrenIds = emptyList()
            )
            return
        }

        val childIds = node.children.map { it.id }
        val childHeights = node.children.map { measureSubtreeHeight(it) }
        val childrenTotal = childHeights.sum() + mmChildGapY * max(0, node.children.size - 1)

        var childCursorTop = top + (height - childrenTotal) / 2f

        node.children.forEachIndexed { idx, child ->
            val ch = childHeights[idx]
            layoutSubtreeSide(
                node = child,
                depth = depth + 1,
                dir = dir,
                top = childCursorTop,
                height = ch,
                result = result
            )
            childCursorTop += ch + mmChildGapY
        }

        val firstY = result[node.children.first().id]!!.y
        val lastY = result[node.children.last().id]!!.y
        val y = (firstY + lastY) / 2f

        result[node.id] = PositionedNode(
            id = node.id,
            title = node.title,
            content = node.content,
            relatedNodeId = node.relatedNodeId,
            x = x,
            y = y,
            childrenIds = childIds
        )
    }
}
