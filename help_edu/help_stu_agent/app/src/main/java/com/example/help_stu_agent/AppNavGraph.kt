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
import com.example.help_stu_agent.ui.card.KnowledgeCardDetailPage
import com.example.help_stu_agent.ui.elite.EliteIdeasPage
import com.example.help_stu_agent.ui.erudition.EruditionLabPage
import com.example.help_stu_agent.ui.login.LoginPage
import com.example.help_stu_agent.ui.login.RegisterPage
import com.example.help_stu_agent.ui.meeting.MeetingMinutesPage
import com.example.help_stu_agent.ui.opensource.OpenSourcePage
import com.example.help_stu_agent.ui.past.PastContentPage
import com.example.help_stu_agent.ui.profile.UserProfilePage
import com.example.help_stu_agent.ui.report.DailyReportPage
import com.example.help_stu_agent.ui.sparky.SparkyLinkPage
import com.example.help_stu_agent.ui.treeHistory.KnowledgeTreeHistoryPage
import com.example.help_stu_agent.ui.treeStructure.KnowledgeTreePageFromCache
import com.example.help_stu_agent.ui.treeStructure.KnowledgeTreePageFromId
import com.example.help_stu_agent.ui.uploadPdf.PdfUploadPage


@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Main,
        modifier = modifier
    ) {
        composable(AppRoutes.Login) {
            LoginPage(
                onLoginSuccess = {
                    navController.navigate(AppRoutes.Main) {
                        popUpTo(AppRoutes.Login) { inclusive = true }
                    }
                },
                onGoRegister = { navController.navigate(AppRoutes.Register) }
            )
        }

        composable(AppRoutes.Register) {
            RegisterPage(
                onRegisterSuccess = {
                    navController.navigate(AppRoutes.Main) {
                        popUpTo(AppRoutes.Login) { inclusive = true }
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
                onGoUpload ={ navController.navigate(AppRoutes.Upload) { launchSingleTop = true } },
                onGoKnowledgeTreeHistory = { navController.navigate(AppRoutes.KnowledgeTreeHistory) { launchSingleTop = true } },
                onGoDailyReport = { navController.navigate(AppRoutes.DailyReport) { launchSingleTop = true } },
                onGoSparkyLink = { navController.navigate(AppRoutes.SparkyLink) { launchSingleTop = true } },
                onOpenKnowledgeCard = { cardId -> navController.navigate(AppRoutes.knowledgeCardDetail(cardId)) { launchSingleTop = true } },
                onGoEliteIdeas = { navController.navigate(AppRoutes.EliteIdeas) { launchSingleTop = true } },
                onGoEruditionLab = { navController.navigate(AppRoutes.EruditionLab) { launchSingleTop = true } },
                onGoMeetingMinutes = { navController.navigate(AppRoutes.MeetingMinutes) { launchSingleTop = true } },
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

        composable(AppRoutes.PastContent) {
            PastContentPage(
                onBack = { navController.popBackStack() } // 处理返回事件
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

        composable(AppRoutes.Upload) {
            PdfUploadPage(
                onBack = { navController.popBackStack() }
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
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppRoutes.EruditionLab) {
            EruditionLabPage(
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppRoutes.MeetingMinutes) {
            MeetingMinutesPage(
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppRoutes.OpenSource) {
            OpenSourcePage(
                onBack = { navController.popBackStack() }
            )
        }


    }
}
