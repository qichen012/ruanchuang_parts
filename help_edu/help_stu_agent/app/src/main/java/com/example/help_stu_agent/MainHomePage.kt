package com.example.help_stu_agent

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainHomePage(
    onGoUpload: () -> Unit,
    onGoKnowledgeTree: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("主界面（占位）", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onGoUpload, modifier = Modifier.width(220.dp)) {
                Text("上传 PDF")
            }
            Button(onClick = onGoKnowledgeTree, modifier = Modifier.width(220.dp)) {
                Text("进入知识树")
            }
        }
    }
}
