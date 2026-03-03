package com.example.help_stu_agent.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.help_stu_agent.AppRoutes

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(AppRoutes.TabHome, "主页", Icons.Default.Home)
    object Features : BottomNavItem(AppRoutes.TabFeatures, "功能区", Icons.Default.Star)
    object User : BottomNavItem(AppRoutes.TabUser, "用户", Icons.Default.Person)
}