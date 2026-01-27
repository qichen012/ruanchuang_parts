package com.example.help_stu_agent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.help_stu_agent.ui.card.KnowledgeCardDetailPage
import com.example.help_stu_agent.ui.elite.EliteIdeasPage
import com.example.help_stu_agent.ui.erudition.EruditionLabPage
import com.example.help_stu_agent.ui.meeting.MeetingMinutesPage
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
        composable(AppRoutes.Main) {
            MainHomePage(
                onGoUpload = {
                    navController.navigate(AppRoutes.Upload) {
                        launchSingleTop = true
                    }
                },
                onGoKnowledgeTreeHistory = {
                    navController.navigate(AppRoutes.KnowledgeTreeHistory) {
                        launchSingleTop = true
                    }
                },
                onGoDailyReport = {
                    navController.navigate(AppRoutes.DailyReport) {
                        launchSingleTop = true
                    }
                },
                onGoSparkyLink = {
                    navController.navigate(AppRoutes.SparkyLink) {
                        launchSingleTop = true
                    }
                },
                onOpenKnowledgeCard = { cardId ->
                    navController.navigate(AppRoutes.knowledgeCardDetail(cardId)) {
                        launchSingleTop = true
                    }
                },
                onGoEliteIdeas = {
                    navController.navigate(AppRoutes.EliteIdeas) {
                        launchSingleTop = true
                    }
                },
                onGoEruditionLab = {
                    navController.navigate(AppRoutes.EruditionLab) {
                        launchSingleTop = true
                    }
                },
                onGoMeetingMinutes = {
                    navController.navigate(AppRoutes.MeetingMinutes) { launchSingleTop = true }
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


    }
}
