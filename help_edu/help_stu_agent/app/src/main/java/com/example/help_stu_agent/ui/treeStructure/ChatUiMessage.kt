package com.example.help_stu_agent.ui.treeStructure

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.help_stu_agent.BuildConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ChatUiMessage(
    val role: String, // "user" | "assistant" | "system"
    val text: String
)

class NodeChatViewModel : ViewModel() {

    // nodeId -> messages
    private val _threads = mutableStateMapOf<String, SnapshotStateList<ChatUiMessage>>()
    val threads: Map<String, SnapshotStateList<ChatUiMessage>> get() = _threads

    private val client = DeepSeekClient(apiKey = BuildConfig.DEEPSEEK_API_KEY)

    fun ensureThread(nodeId: String) {
        if (_threads[nodeId] == null) {
            _threads[nodeId] = mutableStateListOf(
                ChatUiMessage("system", "你是一个学习助手。请结合当前节点内容进行回答，给出清晰、可操作的解释。")
            )
        }
    }

    fun send(nodeId: String, userText: String, contextHint: String?) {
        ensureThread(nodeId)
        val list = _threads[nodeId]!!

        list.add(ChatUiMessage("user", userText))
        list.add(ChatUiMessage("assistant", "思考中…"))

        viewModelScope.launch {
            val dsMsgs = buildList {
                // system
                list.firstOrNull { it.role == "system" }?.let { add(DSMessage("system", it.text)) }

                // 可选：把当前节点内容作为上下文 hint（建议做截断）
                if (!contextHint.isNullOrBlank()) {
                    add(DSMessage("system", "当前节点内容摘要（用于参考，不要原样复述）：\n${contextHint.take(1500)}"))
                }

                // 最近若干轮对话（避免 token 爆炸，取后 12 条即可）
                list.filter { it.role == "user" || it.role == "assistant" }
                    .takeLast(12)
                    .forEach { add(DSMessage(it.role, it.text.replace("思考中…", ""))) }
            }

            val answer = withContext(Dispatchers.IO) { client.chat(dsMsgs) }

            // 替换最后一条“思考中…”
            val lastIdx = list.indexOfLast { it.role == "assistant" && it.text == "思考中…" }
            if (lastIdx >= 0) list[lastIdx] = ChatUiMessage("assistant", answer)
            else list.add(ChatUiMessage("assistant", answer))
        }
    }

    fun clear(nodeId: String) {
        _threads.remove(nodeId)
    }
}
