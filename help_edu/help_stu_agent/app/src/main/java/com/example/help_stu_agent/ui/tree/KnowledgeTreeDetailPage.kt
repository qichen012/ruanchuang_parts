package com.example.help_stu_agent.ui.tree

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.help_stu_agent.data.repo.KnowledgeTreeRepository
import com.example.help_stu_agent.KnowledgeTreePageFromJson

@Composable
fun KnowledgeTreeDetailPage(
    recordId: String
) {
    val context = LocalContext.current
    val repo = remember { KnowledgeTreeRepository(context) }
    var json by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recordId) {
        json = repo.loadJsonById(recordId)
    }

    if (json == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        KnowledgeTreePageFromJson(json!!)
    }
}
