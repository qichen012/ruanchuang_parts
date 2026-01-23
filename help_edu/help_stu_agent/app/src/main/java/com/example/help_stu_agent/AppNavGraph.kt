package com.example.help_stu_agent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

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
    }
}
