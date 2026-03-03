package com.example.help_stu_agent.designsystem.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.help_stu_agent.ui.home.BottomNavItem

data class TabItem(
    val icon: ImageVector,
    val label: String,
    val navItem: BottomNavItem
)