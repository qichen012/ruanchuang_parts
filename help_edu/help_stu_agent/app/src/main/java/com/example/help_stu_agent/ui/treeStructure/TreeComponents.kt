package com.example.help_stu_agent.ui.treeStructure

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.help_stu_agent.designsystem.tokens.AppTokens

@Composable
fun GridNodeCard(
    pNode: PositionedNode,
    accentColor: Color,
    accentStrength: Float,
    isVisited: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean,
    dimAlpha: Float,
    isCollapsed: Boolean,
    canCollapse: Boolean,
    onToggleCollapse: () -> Unit,
    onClick: () -> Unit
) {
    val alpha = if (isHighlighted) 1f else dimAlpha

    val scheme = MaterialTheme.colorScheme
    val surfaceBase = scheme.surface
    val surfaceBottom = lerp(scheme.surfaceVariant, surfaceBase, 0.55f).copy(alpha = 0.90f)

    val surfaceVisited = lerp(surfaceBase, scheme.primaryContainer, 0.65f)

    val tintTop = (0.06f + 0.08f * accentStrength).coerceIn(0.05f, 0.16f)
    val tintBottom = (0.03f + 0.05f * accentStrength).coerceIn(0.03f, 0.10f)

    val topBase = if (isVisited) surfaceVisited else surfaceBase
    val topTinted = lerp(topBase, accentColor, tintTop)
    val bottomTinted = lerp(surfaceBottom, accentColor, tintBottom)

    val bgBrush = Brush.verticalGradient(listOf(topTinted, bottomTinted))

    val shape = RoundedCornerShape(AppTokens.Radius.l)

    val rot by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 90f,
        animationSpec = tween(180),
        label = "collapseRot"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .drawBehind {
                if (isSelected) {
                    val glow = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.maxDimension * 0.85f
                    )
                    val cr = AppTokens.Radius.l.toPx()
                    drawRoundRect(
                        brush = glow,
                        cornerRadius = CornerRadius(cr, cr)
                    )
                }
            }
            .shadow(
                elevation = when {
                    isSelected -> AppTokens.Elevation.overlay
                    isHighlighted -> AppTokens.Elevation.card
                    else -> 0.dp
                },
                shape = shape
            )
            .background(bgBrush, shape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                    isSelected -> accentColor.copy(alpha = 0.95f)
                    isHighlighted -> scheme.outlineVariant.copy(alpha = 0.85f)
                    else -> scheme.outlineVariant.copy(alpha = 0.40f)
                },
                shape = shape
            )
            .clickable { onClick() }
            .padding(AppTokens.Space.m)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val railAlpha = when {
                isSelected -> 1.0f
                isVisited -> 0.75f + 0.15f * accentStrength
                else -> 0.22f + 0.55f * accentStrength
            }.coerceIn(0.18f, 1.0f)

            Box(
                modifier = Modifier
                    .height(44.dp)
                    .width(4.dp)
                    .background(
                        color = if (isSelected) accentColor else accentColor.copy(alpha = railAlpha),
                        shape = RoundedCornerShape(99.dp)
                    )
            )

            Spacer(Modifier.width(AppTokens.Space.s))

            Text(
                text = pNode.title,
                modifier = Modifier.weight(1f),
                color = scheme.onSurface.copy(alpha = alpha),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 2
            )

            Spacer(Modifier.width(AppTokens.Space.s))

            if (canCollapse) {
                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.rotate(rot)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(7.dp)
                .background(
                    color = if (isVisited) accentColor.copy(alpha = 0.75f)
                    else scheme.outlineVariant.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(99.dp)
                )
        )
    }
}
