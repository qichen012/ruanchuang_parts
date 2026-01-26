package com.example.help_stu_agent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.help_stu_agent.ui.home.HomeDrawerAction
import com.example.help_stu_agent.ui.home.HomeDrawerSheet
import com.example.help_stu_agent.ui.home.HomePage

@Composable
fun MainHomePage(
    onGoUpload: () -> Unit,
    onGoKnowledgeTreeHistory: () -> Unit,
    onGoDailyReport: () -> Unit,
    onGoSparkyLink: () -> Unit,
    onOpenKnowledgeCard: (String) -> Unit,
    onGoEliteIdeas: () -> Unit
) {
    val drawerOpen = remember { mutableStateOf(false) }

    // 你的主页
    HomePage(
        onMenuClick = { drawerOpen.value = true },
        onGoUploadPdf = onGoUpload,
        onOpenKnowledgeCard = onOpenKnowledgeCard,
    )


    HomeDrawerSheet(
        visible = drawerOpen.value,
        onDismiss = { drawerOpen.value = false },
        onAction = { action ->
            when (action) {
                HomeDrawerAction.DailyReport -> onGoDailyReport()
                HomeDrawerAction.KnowledgeStructure -> onGoKnowledgeTreeHistory()
                HomeDrawerAction.SparkyLink -> onGoSparkyLink()
                HomeDrawerAction.EliteIdeas -> onGoEliteIdeas()

                HomeDrawerAction.MyAccount -> { /* TODO */ }
            }
        }
    )
}
