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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // [新增导入]
import com.example.help_stu_agent.data.local.UserManager

import com.example.help_stu_agent.designsystem.components.FloatingGlassTabBar
import com.example.help_stu_agent.designsystem.components.TabItem
import com.example.help_stu_agent.ui.home.BottomNavItem
import com.example.help_stu_agent.ui.home.FeaturesPage
import com.example.help_stu_agent.ui.home.HomePage
import com.example.help_stu_agent.ui.home.UserPage
import com.example.help_stu_agent.ui.home.UserViewModel // [新增导入]
import com.example.help_stu_agent.ui.home.AppUsageTracker // [新增导入]
import kotlinx.coroutines.launch

@Composable
fun MainHomePage(
    onGoUploadPdf: () -> Unit,
    onGoUploadPhoto: () -> Unit,
    onGoKnowledgeTreeHistory: () -> Unit,
    onGoDailyReport: () -> Unit,
    onGoSparkyLink: () -> Unit,
    onOpenKnowledgeCard: (String) -> Unit,
    onGoEliteIdeas: () -> Unit,
    onGoEruditionLab: () -> Unit,
    onGoMeetingMinutes: () -> Unit,
    onGoMeetingHistory: () -> Unit,
    onGoMyAccount: () -> Unit,
    onLogout: () -> Unit,
    onGoPastContent: () -> Unit,
    onGoOpenSource: () -> Unit,
    userViewModel: UserViewModel = viewModel()
) {
    val pages = listOf(
        BottomNavItem.Home,
        BottomNavItem.Features,
        BottomNavItem.User
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val userManager = remember { UserManager(context) }

    val currentUserId by userManager.userIdFlow.collectAsState(initial = null)

    LaunchedEffect(currentUserId) {
        currentUserId?.let { id ->
            userViewModel.fetchUsageStats(id)
            userViewModel.fetchUserInfo(id)
        }
    }

    currentUserId?.let { id ->
        AppUsageTracker(userId = id, viewModel = userViewModel)
    }

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
                            onGoUploadPdf = onGoUploadPdf,
                            onGoUploadPhoto = onGoUploadPhoto,
                            onGoOpenSource = onGoOpenSource,
                            onGoDailyReport = onGoDailyReport,
                            onGoKnowledgeStructure = onGoKnowledgeTreeHistory,
                            onGoSparkyLink = onGoSparkyLink,
                            onGoEliteIdeas = onGoEliteIdeas,
                            onGoEruditionLab = onGoEruditionLab,
                            onGoMeetingMinutes = onGoMeetingMinutes,
                            onGoMeetingHistory = onGoMeetingHistory
                        )
                    }
                    BottomNavItem.User -> {
                        UserPage(
                            userName = userViewModel.userName,
                            userEmail = userViewModel.userEmail,
                            dataPoints = userViewModel.usageDataPoints,
                            peakTimeLabel = userViewModel.peakTimeLabel,
                            userAge = userViewModel.userAge,
                            userGender = userViewModel.userGender,
                            onGoMyAccount = onGoMyAccount,
                            onLogout = {
                                coroutineScope.launch {
                                    userManager.clearUserSession()
                                    onLogout()
                                }
                            }
                        )
                    }
                }
            }

            FloatingGlassTabBar(
                tabs = tabItems,
                selectedIndex = pagerState.currentPage,
                onTabSelected = { index ->
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