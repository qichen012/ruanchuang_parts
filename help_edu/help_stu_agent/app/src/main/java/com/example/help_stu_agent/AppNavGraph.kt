package com.example.help_stu_agent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.help_stu_agent.data.local.UserManager
import com.example.help_stu_agent.ui.knowledgeCard.KnowledgeCardDetailPage
import com.example.help_stu_agent.ui.eliteIdeas.EliteIdeasPage
import com.example.help_stu_agent.ui.eruditionLab.EruditionLabPage
import com.example.help_stu_agent.ui.eliteIdeas.EliteIdeaDetailPage
import com.example.help_stu_agent.ui.login.LoginPage
import com.example.help_stu_agent.ui.register.RegisterPage
import com.example.help_stu_agent.ui.meetingMem.MeetingMinutesPage
import com.example.help_stu_agent.ui.meetingMem.MeetingHistoryPage
import com.example.help_stu_agent.ui.meetingMem.MeetingDetailPage
import com.example.help_stu_agent.ui.openSource.OpenSourcePage
import com.example.help_stu_agent.ui.past.PastContentPage
import com.example.help_stu_agent.ui.userProfile.UserProfilePage
import com.example.help_stu_agent.ui.dailyReport.DailyReportPage
import com.example.help_stu_agent.ui.sparkyLink.SparkyLinkPage
import com.example.help_stu_agent.ui.treeHistory.KnowledgeTreeHistoryPage
import com.example.help_stu_agent.ui.treeStructure.KnowledgeTreePageFromCache
import com.example.help_stu_agent.ui.treeStructure.KnowledgeTreePageFromId
import com.example.help_stu_agent.ui.uploadPdf.PdfUploadPage
import com.example.help_stu_agent.ui.uploadPhoto.UploadPhotoPage
import kotlinx.coroutines.launch


@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Login,
        modifier = modifier
    ) {
        composable(AppRoutes.Login) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val userManager = remember { UserManager(context) }
            LoginPage(
                // 接收从 LoginPage 传出来的 token 和 userId
                onLoginSuccess = { token, userId ->
                    coroutineScope.launch {
                        userManager.saveUserSession(userId, token)

                        navController.navigate(AppRoutes.Main) {
                            popUpTo(AppRoutes.Login) { inclusive = true }
                        }
                    }
                },
                onGoRegister = { navController.navigate(AppRoutes.Register) }
            )
        }

        composable(AppRoutes.Register) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val userManager = remember { UserManager(context) }

            RegisterPage(
                // 接收 token 和 userId
                onRegisterSuccess = { token, userId ->
                    coroutineScope.launch {
                        // 注册成功也直接保存 Session
                        userManager.saveUserSession(userId, token)
                        // 然后跳转主页
                        navController.navigate(AppRoutes.Main) {
                            popUpTo(AppRoutes.Login) { inclusive = true }
                        }
                    }
                },
                onGoLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = AppRoutes.Main,
            exitTransition = {
                if (targetState.destination.route == AppRoutes.PastContent) {
                    scaleOut(targetScale = 0.92f, animationSpec = tween(400)) +
                            fadeOut(animationSpec = tween(400))
                } else null
            },
            popEnterTransition = {
                if (initialState.destination.route == AppRoutes.PastContent) {
                    scaleIn(initialScale = 0.92f, animationSpec = tween(400)) +
                            fadeIn(animationSpec = tween(400))
                } else null
            }
        ) {
            MainHomePage(
                onGoUploadPdf ={ navController.navigate(AppRoutes.UploadPdf) { launchSingleTop = true } },
                onGoUploadPhoto = { navController.navigate(AppRoutes.UploadPhoto) { launchSingleTop = true } },
                onGoKnowledgeTreeHistory = { navController.navigate(AppRoutes.KnowledgeTreeHistory) { launchSingleTop = true } },
                onGoDailyReport = { navController.navigate(AppRoutes.DailyReport) { launchSingleTop = true } },
                onGoSparkyLink = { navController.navigate(AppRoutes.SparkyLink) { launchSingleTop = true } },
                onOpenKnowledgeCard = { cardId -> navController.navigate(AppRoutes.knowledgeCardDetail(cardId)) { launchSingleTop = true } },
                onGoEliteIdeas = { navController.navigate(AppRoutes.EliteIdeas) { launchSingleTop = true } },
                onGoEruditionLab = { navController.navigate(AppRoutes.EruditionLab) { launchSingleTop = true } },
                onGoMeetingMinutes = { navController.navigate(AppRoutes.MeetingMinutes) { launchSingleTop = true } },
                onGoMeetingHistory = { navController.navigate(AppRoutes.MeetingHistory) { launchSingleTop = true } },
                onGoMyAccount = { navController.navigate(AppRoutes.UserProfile) { launchSingleTop = true } },
                onLogout = { navController.navigate(AppRoutes.Login) { popUpTo(AppRoutes.Main) { inclusive = true } } },
                onGoPastContent = { navController.navigate(AppRoutes.PastContent) { launchSingleTop = true } } ,
                onGoOpenSource = { navController.navigate(AppRoutes.OpenSource) { launchSingleTop = true } }
            )
        }

        composable(
            route = AppRoutes.PastContent,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(400)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(400)
                )
            }
        ) {
            PastContentPage(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.UserProfile) {
            UserProfilePage(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(AppRoutes.Login) {
                        popUpTo(AppRoutes.Main) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.UploadPdf) {
            PdfUploadPage(
                onBack = { navController.popBackStack() } ,
                onSwitchToPhoto = {
                    navController.navigate(AppRoutes.UploadPhoto) {
                        popUpTo(AppRoutes.UploadPdf) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoutes.UploadPhoto) {
            UploadPhotoPage(
                onBack = { navController.popBackStack() },
                onSwitchToPdf = {
                    navController.navigate(AppRoutes.UploadPdf) {
                        popUpTo(AppRoutes.UploadPhoto) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.KnowledgeTree) {
            KnowledgeTreePageFromCache()
        }

        composable(AppRoutes.KnowledgeTreeHistory) {
            KnowledgeTreeHistoryPage(
                onBack = { navController.popBackStack() },
                onOpen = { treeId ->
                    navController.navigate(AppRoutes.knowledgeTreeById(treeId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = AppRoutes.KnowledgeTreeById,
            arguments = listOf(navArgument("treeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val treeId = backStackEntry.arguments?.getString("treeId").orEmpty()
            KnowledgeTreePageFromId(treeId = treeId)
        }

        composable(AppRoutes.DailyReport) {
            DailyReportPage(
                onBack = { navController.popBackStack() },
                onOpenKnowledgeCard = { cardId ->
                    navController.navigate(AppRoutes.knowledgeCardDetail(cardId)) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = AppRoutes.KnowledgeCardDetail,
            arguments = listOf(navArgument("cardId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId").orEmpty()
            KnowledgeCardDetailPage(
                cardId = cardId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppRoutes.SparkyLink) {
            SparkyLinkPage(
                onBack = { navController.popBackStack() },
                onOpenReport = { cardId ->
                    navController.navigate(AppRoutes.knowledgeCardDetail(cardId)) { launchSingleTop = true }
                }
            )
        }
        composable(AppRoutes.EliteIdeas) {
            EliteIdeasPage(
                onBack = { navController.popBackStack() },
                onOpenIdeaDetail = { ideaId ->
                    navController.navigate(AppRoutes.eliteIdeaDetail(ideaId)) {
                        launchSingleTop = true
                    }
                }

            )
        }
        composable(AppRoutes.EruditionLab) {
            EruditionLabPage(
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppRoutes.MeetingMinutes) {
            MeetingMinutesPage(
                onBack = { navController.popBackStack() },
                onGoHistory = { navController.navigate(AppRoutes.MeetingHistory) }
            )
        }
        composable(AppRoutes.MeetingHistory) {
            MeetingHistoryPage(
                onBack = { navController.popBackStack() },
                onOpenDetail = { id -> navController.navigate(AppRoutes.meetingDetail(id)) }
            )
        }
        composable(
            route = AppRoutes.MeetingDetail,
            arguments = listOf(navArgument("meetingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId").orEmpty()
            MeetingDetailPage(
                meetingId = meetingId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppRoutes.OpenSource) {
            OpenSourcePage(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoutes.EliteIdeaDetail,
            arguments = listOf(navArgument("ideaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ideaId = backStackEntry.arguments?.getString("ideaId").orEmpty()
            EliteIdeaDetailPage(
                ideaId = ideaId,
                onBack = { navController.popBackStack() }
            )
        }

    }
}
