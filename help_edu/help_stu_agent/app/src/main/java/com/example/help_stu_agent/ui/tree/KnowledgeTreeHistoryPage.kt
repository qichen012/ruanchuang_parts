package com.example.help_stu_agent.ui.tree

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.help_stu_agent.data.db.KnowledgeTreeEntity
import com.example.help_stu_agent.data.repo.KnowledgeTreeRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeTreeHistoryPage(
    onOpen: (String) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { KnowledgeTreeRepository(context) }

    var list by remember { mutableStateOf<List<KnowledgeTreeEntity>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        list = repo.listAll()
        loading = false
    }

    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Knowledge Trees") }) }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            if (loading) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.id }) { e ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(e.id) }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(e.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    fmt.format(Date(e.createdAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                e.pdfDisplayName?.let {
                                    Spacer(Modifier.height(6.dp))
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
