import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class DpSizeAndPosition(val size: DpSize, val positionX: Dp)

private val damping = 0.8f
private val stiffness = Spring.StiffnessMediumLow

@Composable
fun TabCapsuleBackground(
    tabOffsetsAndSizes: Map<Int, DpSizeAndPosition>,
    selectedIndex: Int
) {
    val scope = rememberCoroutineScope()

    val animatedOffsetX = remember { Animatable(0.dp, Dp.VectorConverter) }
    val animatedWidth = remember { Animatable(0.dp, Dp.VectorConverter) }
    val animatedHeight = remember { Animatable(0.dp, Dp.VectorConverter) }

    // 使用弹簧物理效果替代生硬的 tween 线性动画
    val animSpec = spring<Dp>(dampingRatio = damping, stiffness = stiffness)

    LaunchedEffect(selectedIndex, tabOffsetsAndSizes[selectedIndex]) {
        val currentTab = tabOffsetsAndSizes[selectedIndex] ?: return@LaunchedEffect

        scope.launch { animatedOffsetX.animateTo(currentTab.positionX, animSpec) }
        scope.launch { animatedWidth.animateTo(currentTab.size.width, animSpec) }
        scope.launch { animatedHeight.animateTo(currentTab.size.height, animSpec) }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(animatedOffsetX.value.roundToPx(), 0) }
            .size(animatedWidth.value, animatedHeight.value)
            .background(Color(0xFFE8EFFF), CircleShape) // 浅蓝色背景
    )
}