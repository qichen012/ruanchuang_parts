package com.example.help_stu_agent.ui.treeStructure

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.help_stu_agent.designsystem.components.AppBottomPanel
import com.example.help_stu_agent.designsystem.tokens.AppTokens
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import kotlinx.coroutines.launch

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
                .onSizeChanged { onPanelSizeChanged(it.height) },
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .padding(horizontal = AppTokens.Space.l, vertical = AppTokens.Space.m),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = node.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (showSwipeHint) {
                            Text(
                                text = "左右滑动切换节点 (${page + 1}/${siblingIds.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                // Main Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(AppTokens.Space.l)
                ) {
                    item {
                        MathJaxWebView(
                            content = node.content,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(AppTokens.Space.xl))
                    }

                    item {
                        ChatInlinePanel(
                            nodeId = node.id,
                            nodeTitle = node.title,
                            nodeContentForContext = node.content,
                            onRequestExpand = onRequestExpand,
                        )
                    }

                    if (node.relatedNodeId != null) {
                        item {
                            Spacer(Modifier.height(AppTokens.Space.l))
                            Button(
                                onClick = { onJump(node.relatedNodeId) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(AppTokens.Radius.m)
                            ) {
                                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(AppTokens.Space.s))
                                Text("跳转到关联点: $relatedTitle")
                            }
                        }
                    }
                    
                    item {
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatInlinePanel(
    nodeId: String,
    nodeTitle: String,
    nodeContentForContext: String,
    onRequestExpand: () -> Unit,
    vm: NodeChatViewModel = viewModel()
) {
    vm.ensureThread(nodeId)
    val thread = vm.threads[nodeId]

    var input by remember(nodeId) {
        mutableStateOf(TextFieldValue(""))
    }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun doSend() {
        val t = input.text.trim()
        if (t.isNotEmpty()) {
            vm.send(nodeId, t, nodeContentForContext)
            input = TextFieldValue("")
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Card(
        shape = RoundedCornerShape(AppTokens.Radius.l),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(AppTokens.Space.m)
                .animateContentSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.size(4.dp, 16.dp)
                ) {}
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AI 知识助手",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { vm.clear(nodeId); vm.ensureThread(nodeId) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(AppTokens.Space.m))

            // Message List
            thread?.forEach { m ->
                if (m.role != "system") {
                    ChatBubbleItem(role = m.role, text = m.text)
                    Spacer(Modifier.height(AppTokens.Space.s))
                }
            }

            Spacer(Modifier.height(AppTokens.Space.m))

            // Input
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(AppTokens.Radius.m))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusChanged { st ->
                            if (st.isFocused) {
                                onRequestExpand()
                                scope.launch { bringIntoViewRequester.bringIntoView() }
                            }
                        },
                    placeholder = { Text("有问题尽管问我...", fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { doSend() })
                )
                
                IconButton(
                    onClick = { doSend() },
                    enabled = input.text.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Send, null)
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(role: String, text: String) {
    val isUser = role == "user"
    val hasMath = text.contains("$") || text.contains("\\(") || text.contains("\\[")
    
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Text(
            text = if (isUser) "你" else "AI 助手",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
        )
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 12.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            if (hasMath) {
                // 对于含有数学公式的消息，使用支持 MathJax 的 WebView 渲染
                MarkdownMathJaxWebView(
                    markdown = text,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    textColorCss = textColor.toCssHex(),
                    linkColorCss = (if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary).toCssHex(),
                    codeBgCss = Color.Black.copy(alpha = 0.1f).toCssHex(),
                    bubbleBgCss = bubbleColor.toCssHex()
                )
            } else {
                // 对于普通文本，使用原生组件
                val richTextState = rememberRichTextState()
                LaunchedEffect(text) {
                    richTextState.setMarkdown(text)
                }
                SelectionContainer {
                    RichText(
                        state = richTextState,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
