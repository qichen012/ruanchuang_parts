package com.example.help_stu_agent.ui.treeStructure

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class ChatUiMessage(
    val role: String, // "user" | "assistant" | "system"
    val text: String
)

class NodeChatViewModel : ViewModel() {

    private val _threads = mutableStateMapOf<String, SnapshotStateList<ChatUiMessage>>()
    val threads: Map<String, SnapshotStateList<ChatUiMessage>> get() = _threads

    private val client = RagBackendClient()

    fun ensureThread(nodeId: String) {
        if (_threads[nodeId] == null) {
            _threads[nodeId] = mutableStateListOf(
                ChatUiMessage("system", "你可以向我提问关于此节点的任何问题，我会结合整份文档为你解答。")
            )
        }
    }

    fun send(nodeId: String, userText: String, contextHint: String?) {
        ensureThread(nodeId)
        val list = _threads[nodeId]!!

        list.add(ChatUiMessage("user", userText))
        list.add(ChatUiMessage("assistant", "知识库检索并思考中…"))

        viewModelScope.launch {
            val answer = client.chatWithRag(
                query = userText,
                nodeId = nodeId,
                contextHint = contextHint
            )

            val lastIdx = list.indexOfLast { it.role == "assistant" && it.text.contains("思考中") }
            if (lastIdx >= 0) list[lastIdx] = ChatUiMessage("assistant", answer)
            else list.add(ChatUiMessage("assistant", answer))
        }
    }

    fun clear(nodeId: String) {
        _threads.remove(nodeId)
    }
}
