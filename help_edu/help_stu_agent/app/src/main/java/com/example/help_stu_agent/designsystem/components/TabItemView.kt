package com.example.help_stu_agent.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val damping = 0.8f
private val stiffness = Spring.StiffnessMediumLow
@Composable
fun TabItemView(
    tab: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(vertical = 12.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = if (isSelected) Color(0xFF1E293B) else Color(0xFF94A3B8), // 选中时深色
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(if (isSelected) 8.dp else 0.dp))

        // 文字的展开与胶囊的滑动严格共享同样的弹簧物理参数，防止动画脱节
        AnimatedVisibility(
            visible = isSelected,
            enter = expandHorizontally(spring(dampingRatio = damping, stiffness = stiffness)) + fadeIn(tween(200)),
            exit = shrinkHorizontally(spring(dampingRatio = damping, stiffness = stiffness)) + fadeOut(tween(150))
        ) {
            Text(
                text = tab.label,
                color = Color(0xFF1E293B), // 选中文字为深色
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
        }
    }
}