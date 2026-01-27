@file:Suppress("unused")

package com.example.help_stu_agent.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Schema
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.help_stu_agent.ui.theme.BranchPaletteDark
import com.example.help_stu_agent.ui.theme.BranchPaletteLight


enum class HomeDrawerAction {
    DailyReport,
    KnowledgeStructure,
    SparkyLink,
    EliteIdeas,
    EruditionLab,
    MyAccount,
    MeetingMinutes,

}

@Composable
fun HomeDrawerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAction: (HomeDrawerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val dark = cs.background.luminance() < 0.3f
    val palette = if (dark) BranchPaletteDark else BranchPaletteLight

    Box(modifier = modifier.fillMaxSize()) {

        // Scrim
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(160))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (dark) 0.45f else 0.32f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismiss() }
            )
        }

        // Panel
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(animationSpec = tween(220)) { fullWidth -> -fullWidth } + fadeIn(tween(180)),
            exit = slideOutHorizontally(animationSpec = tween(220)) { fullWidth -> -fullWidth } + fadeOut(tween(180))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f)
                    .shadow(18.dp, RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)),
                color = cs.surface,
                shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp)
                        .padding(top = 18.dp, bottom = 14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Menu",
                                style = MaterialTheme.typography.titleLarge,
                                color = cs.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Explore your workspace",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(cs.surfaceVariant, RoundedCornerShape(14.dp))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = cs.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    SectionLabel("DASHBOARD")

                    DrawerItem(
                        icon = Icons.Outlined.ListAlt,
                        iconBg = palette.getOrNull(0)?.copy(alpha = 0.16f) ?: cs.primary.copy(alpha = 0.14f),
                        iconTint = palette.getOrNull(0) ?: cs.primary,
                        title = "Daily Report",
                        onClick = { onAction(HomeDrawerAction.DailyReport); onDismiss() }
                    )

                    DrawerItem(
                        icon = Icons.Outlined.Schema,
                        iconBg = palette.getOrNull(1)?.copy(alpha = 0.16f) ?: cs.primary.copy(alpha = 0.14f),
                        iconTint = palette.getOrNull(1) ?: cs.primary,
                        title = "Knowledge Structure",
                        onClick = { onAction(HomeDrawerAction.KnowledgeStructure); onDismiss() }
                    )

                    DrawerItem(
                        icon = Icons.Outlined.Bolt,
                        iconBg = palette.getOrNull(2)?.copy(alpha = 0.16f) ?: cs.primary.copy(alpha = 0.14f),
                        iconTint = palette.getOrNull(2) ?: cs.primary,
                        title = "Sparky Link",
                        onClick = { onAction(HomeDrawerAction.SparkyLink); onDismiss() }
                    )
                    DrawerItem(
                        icon = Icons.Outlined.Science,
                        iconBg = palette.getOrNull(4)?.copy(alpha = 0.16f) ?: cs.primary.copy(alpha = 0.14f),
                        iconTint = palette.getOrNull(4) ?: cs.primary,
                        title = "Erudition Lab",
                        onClick = { onAction(HomeDrawerAction.EruditionLab); onDismiss() }
                    )

                    DrawerItem(
                        icon = Icons.Outlined.Lightbulb,
                        iconBg = palette.getOrNull(3)?.copy(alpha = 0.16f) ?: cs.primary.copy(alpha = 0.14f),
                        iconTint = palette.getOrNull(3) ?: cs.primary,
                        title = "Elite Ideas",
                        onClick = { onAction(HomeDrawerAction.EliteIdeas); onDismiss() }
                    )

                    DrawerItem(
                        icon = Icons.Outlined.RecordVoiceOver,
                        iconBg = palette.getOrNull(2)?.copy(alpha = 0.16f) ?: cs.primary.copy(alpha = 0.14f),
                        iconTint = palette.getOrNull(2) ?: cs.primary,
                        title = "Meet Memo",
                        onClick = { onAction(HomeDrawerAction.MeetingMinutes); onDismiss() }
                    )


                    DrawerItem(
                        icon = Icons.Outlined.AccountCircle,
                        iconBg = palette.getOrNull(4)?.copy(alpha = 0.16f) ?: cs.primary.copy(alpha = 0.14f),
                        iconTint = palette.getOrNull(4) ?: cs.primary,
                        title = "My Account",
                        onClick = { onAction(HomeDrawerAction.MyAccount); onDismiss() }
                    )

                    Spacer(Modifier.height(10.dp))
                    SectionLabel("BOARD")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                color = cs.surfaceVariant.copy(alpha = if (dark) 0.35f else 0.65f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Coming Soon",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = "V 2.4.0 • DAILY REFLECT",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val cs = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = cs.onSurfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(iconBg, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = cs.onSurfaceVariant
        )
    }
}

/**
 * 让 Color.luminance 可用（不引入额外依赖）
 */
private fun Color.luminance(): Float {
    fun channel(c: Float): Float = if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).let { it * it }
    val r = channel(red)
    val g = channel(green)
    val b = channel(blue)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
