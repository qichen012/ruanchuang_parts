package com.example.help_stu_agent

import android.R.attr.type
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.help_stu_agent.ui.card.KnowledgeCardDetailPage
import com.example.help_stu_agent.ui.tree.KnowledgeTreeHistoryPage

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
                onOpenKnowledgeCard = { cardId ->
                    navController.navigate(AppRoutes.knowledgeCardDetail(cardId)) {
                        launchSingleTop = true
                    }
                }
            )
        }


        composable(AppRoutes.Upload) {
            PdfUploadPage(
                onGoToKnowledgeTree = {
                    // 典型期望：进入知识树后，返回键直接回“主界面”，而不是回到 Upload
                    navController.navigate(AppRoutes.KnowledgeTree) {
                        launchSingleTop = true
                        popUpTo(AppRoutes.Upload) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.KnowledgeTree) {
            KnowledgeTreePageFromCache()
        }

        composable(AppRoutes.KnowledgeTreeHistory) {
            KnowledgeTreeHistoryPage(
                onOpen = {
                    navController.navigate(AppRoutes.KnowledgeTree) {
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


    }
}
