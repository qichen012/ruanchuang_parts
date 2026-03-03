package com.example.help_stu_agent


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.help_stu_agent.designsystem.components.FloatingGlassTabBar
import com.example.help_stu_agent.designsystem.components.TabItem

import com.example.help_stu_agent.ui.home.BottomNavItem
import com.example.help_stu_agent.ui.home.FeaturesPage
import com.example.help_stu_agent.ui.home.HomePage
import com.example.help_stu_agent.ui.home.UserPage


@Composable
fun MainHomePage(
    onGoUpload: () -> Unit,
    onGoKnowledgeTreeHistory: () -> Unit,
    onGoDailyReport: () -> Unit,
    onGoSparkyLink: () -> Unit,
    onOpenKnowledgeCard: (String) -> Unit,
    onGoEliteIdeas: () -> Unit,
    onGoEruditionLab: () -> Unit,
    onGoMeetingMinutes: () -> Unit,
    onGoMyAccount: () -> Unit,
    onLogout: () -> Unit,
    onGoPastContent: () -> Unit
) {
    var currentTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    val tabs = listOf(
        TabItem(icon = Icons.Outlined.Home, label = "Home", navItem = BottomNavItem.Home),
        TabItem(icon = Icons.Outlined.GridView, label = "Features", navItem = BottomNavItem.Features),
        TabItem(icon = Icons.Outlined.Person, label = "Profile", navItem = BottomNavItem.User)
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()) // 只保留顶部边距
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (currentTab) {
                    BottomNavItem.Home -> {
                        HomePage(
                            onOpenKnowledgeCard = onOpenKnowledgeCard,
                            onGoPastContent = onGoPastContent
                        )
                    }
                    BottomNavItem.Features -> {
                        FeaturesPage(
                            onGoUploadPdf = onGoUpload,
                            onGoDailyReport = onGoDailyReport,
                            onGoKnowledgeStructure = onGoKnowledgeTreeHistory,
                            onGoSparkyLink = onGoSparkyLink,
                            onGoEliteIdeas = onGoEliteIdeas,
                            onGoEruditionLab = onGoEruditionLab,
                            onGoMeetingMinutes = onGoMeetingMinutes
                        )
                    }
                    BottomNavItem.User -> {
                        UserPage(
                            onGoMyAccount = onGoMyAccount,
                            onLogout = onLogout
                        )
                    }
                }
            }

            FloatingGlassTabBar(
                tabs = tabs,
                selectedIndex = tabs.indexOfFirst { it.navItem == currentTab },
                onTabSelected = { index ->
                    currentTab = tabs[index].navItem
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            )
        }
    }
}
