package com.example.help_stu_agent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
    onGoPastContent: () -> Unit,
    onGoOpenSource: () -> Unit
) {
    val pages = listOf(
        BottomNavItem.Home,
        BottomNavItem.Features,
        BottomNavItem.User
    )

    // 初始化 PagerState
    val pagerState = rememberPagerState(pageCount = { pages.size })
    // 用于在点击底部 Tab 时触发滚动动画的协程作用域
    val coroutineScope = rememberCoroutineScope()

    // 底部导航栏的 UI 配置模型
    val tabItems = listOf(
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
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // 可选：禁用超出边缘的拉伸回弹效果，让页面显得更干脆
                beyondViewportPageCount = 1
            ) { page ->
                when (pages[page]) {
                    BottomNavItem.Home -> {
                        HomePage(
                            onOpenKnowledgeCard = onOpenKnowledgeCard,
                            onGoPastContent = onGoPastContent
                        )
                    }
                    BottomNavItem.Features -> {
                        FeaturesPage(
                            onGoUploadPdf = onGoUpload,
                            onGoOpenSource = onGoOpenSource,
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

            // 悬浮导航栏
            FloatingGlassTabBar(
                tabs = tabItems,
                // 当前选中的 Index 直接绑定 pagerState.currentPage
                selectedIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    // 点击 Tab 时，触发页面平滑滚动
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            )
        }
    }
}