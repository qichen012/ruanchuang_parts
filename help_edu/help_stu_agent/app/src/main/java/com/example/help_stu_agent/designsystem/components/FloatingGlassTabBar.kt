package com.example.help_stu_agent.designsystem.components

import DpSizeAndPosition
import TabCapsuleBackground
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp


@Composable
fun FloatingGlassTabBar(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Surface(
        color = Color.White.copy(alpha = 0.80f),
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        shadowElevation = 8.dp,
        modifier = modifier
            .background(Color.Transparent)
            .padding(bottom = 32.dp)
            .wrapContentSize()
    ) {
        Box(
            modifier = Modifier
                .background(Color.Transparent)
                .padding(6.dp)
        ) {
            val tabOffsetsAndSizes = remember { mutableStateMapOf<Int, DpSizeAndPosition>() }

            // 流动的浅蓝色胶囊
            TabCapsuleBackground(
                tabOffsetsAndSizes = tabOffsetsAndSizes,
                selectedIndex = selectedIndex
            )

            // 图标与文字行
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    TabItemView(
                        tab = tab,
                        isSelected = index == selectedIndex,
                        onClick = { onTabSelected(index) },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            with(density) {
                                tabOffsetsAndSizes[index] = DpSizeAndPosition(
                                    size = DpSize(coordinates.size.width.toDp(), coordinates.size.height.toDp()),
                                    positionX = coordinates.positionInParent().x.toDp()
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}