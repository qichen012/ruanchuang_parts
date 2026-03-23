package com.example.help_stu_agent.ui.sparkyLink

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class TopConcaveShape(
    private val cornerRadius: Float = 34f,
    private val notchRadius: Float = 56f,
    private val notchDepth: Float = 22f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = cornerRadius
        val nr = notchRadius
        val nd = notchDepth
        val cx = size.width / 2f

        val path = Path().apply {
            moveTo(r, 0f)
            quadraticTo(0f, 0f, 0f, r)

            lineTo(0f, size.height - r)
            quadraticTo(0f, size.height, r, size.height)

            lineTo(cx - nr, size.height)

            cubicTo(
                cx - nr * 0.55f, size.height,
                cx - nr * 0.35f, size.height - nd,
                cx, size.height - nd
            )

            cubicTo(
                cx + nr * 0.35f, size.height - nd,
                cx + nr * 0.55f, size.height,
                cx + nr, size.height
            )

            lineTo(size.width - r, size.height)
            quadraticTo(size.width, size.height, size.width, size.height - r)

            lineTo(size.width, r)
            quadraticTo(size.width, 0f, size.width - r, 0f)

            close()
        }
        return Outline.Generic(path)
    }
}

class BottomConcaveShape(
    private val cornerRadius: Float = 34f,
    private val notchRadius: Float = 56f,
    private val notchHeight: Float = 20f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = cornerRadius
        val nr = notchRadius
        val nh = notchHeight
        val cx = size.width / 2f

        val path = Path().apply {
            moveTo(r, nh)
            quadraticTo(0f, nh, 0f, nh + r)

            lineTo(0f, size.height - r)
            quadraticTo(0f, size.height, r, size.height)

            lineTo(size.width - r, size.height)
            quadraticTo(size.width, size.height, size.width, size.height - r)

            lineTo(size.width, nh + r)
            quadraticTo(size.width, nh, size.width - r, nh)

            lineTo(cx + nr, nh)

            cubicTo(
                cx + nr * 0.55f, nh,
                cx + nr * 0.35f, 0f,
                cx, 0f
            )

            cubicTo(
                cx - nr * 0.35f, 0f,
                cx - nr * 0.55f, nh,
                cx - nr, nh
            )

            lineTo(r, nh)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun SparkyHeader(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = Color(0xFFF3F6F8),
            shadowElevation = 4.dp,
            onClick = onBackClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Spark Link",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.width(52.dp))
    }
}

@Composable
fun SparkySplitCard(
    rawDate: String,
    transformedDate: String,
    onRawClick: () -> Unit,
    onTransformedClick: () -> Unit
) {
    val outerShape = RoundedCornerShape(34.dp)

    val topShape = remember {
        TopConcaveShape(
            cornerRadius = 34f,
            notchRadius = 64f,
            notchDepth = 20f
        )
    }

    val bottomShape = remember {
        BottomConcaveShape(
            cornerRadius = 34f,
            notchRadius = 64f,
            notchHeight = 18f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
            .clip(outerShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEAF4F7),
                        Color(0xFFF5F6F7),
                        Color(0xFFF1F0EB)
                    )
                )
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        shape = topShape
                        clip = true
                    }
                    .background(Color(0xFFF4F5F7))
                    .clickable { onRawClick() }
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Image Context",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFECEFF3))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "HISTORY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(70.dp),
                                shape = RoundedCornerShape(22.dp),
                                color = Color.White,
                                shadowElevation = 6.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "T",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            Column {
                                Text(
                                    text = "Upload Photo",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Source Data",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Text(
                            text = formatDisplayDate(rawDate),
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC7D0DC)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(0.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        shape = bottomShape
                        clip = true
                    }
                    .background(Color(0xFFF7F1E5))
                    .clickable { onTransformedClick() }
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Report Data",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD1A78B)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFFFE8D1))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "HISTORY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF97316)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(70.dp),
                                shape = RoundedCornerShape(22.dp),
                                color = Color.White,
                                shadowElevation = 6.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "🌱",
                                        fontSize = 28.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            Column {
                                Text(
                                    text = "Daily Report",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Reframed Data",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFD1A78B)
                                )
                            }
                        }

                        Text(
                            text = formatDisplayDate(transformedDate),
                            fontSize = 27.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 2.dp)
                .size(84.dp),
            shape = CircleShape,
            color = Color(0xFFF8F8F8),
            shadowElevation = 10.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "T",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun SlideToSparkButton(
    enabled: Boolean,
    isLoading: Boolean,
    onSlideComplete: () -> Unit
) {
    val trackHeight = 76.dp
    val thumbSize = 62.dp
    val horizontalPadding = 8.dp

    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    val animatedOffset = remember { Animatable(0f) }

    LaunchedEffect(dragOffsetPx) {
        animatedOffset.snapTo(dragOffsetPx)
    }

    LaunchedEffect(enabled, isLoading) {
        if (!enabled || isLoading) {
            dragOffsetPx = 0f
            animatedOffset.snapTo(0f)
        }
    }

    val maxDragPx = (trackWidthPx - thumbSizePx - horizontalPaddingPx * 2).coerceAtLeast(0f)
    val progress = if (maxDragPx > 0f) {
        (animatedOffset.value / maxDragPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .onSizeChanged { size ->
                trackWidthPx = size.width.toFloat()
            }
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black)
            .pointerInput(enabled, isLoading, trackWidthPx) {
                if (!enabled || isLoading) return@pointerInput

                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, maxDragPx)
                    },
                    onDragEnd = {
                        if (dragOffsetPx >= maxDragPx * 0.88f && enabled && !isLoading) {
                            onSlideComplete()
                        }
                        dragOffsetPx = 0f
                    },
                    onDragCancel = {
                        dragOffsetPx = 0f
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            val textAlpha = (1f - progress * 0.45f).coerceIn(0.55f, 1f)

            Text(
                text = if (isLoading) "Sparking..." else "Slide to Spark",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = textAlpha),
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Text(
                    text = "›",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.15f + index * 0.12f)
                )
                if (index != 2) Spacer(modifier = Modifier.width(2.dp))
            }
        }

        Surface(
            modifier = Modifier
                .padding(start = horizontalPadding, top = horizontalPadding, bottom = horizontalPadding)
                .offset {
                    IntOffset(
                        x = animatedOffset.value.roundToInt(),
                        y = 0
                    )
                }
                .size(thumbSize),
            shape = CircleShape,
            color = Color(0xFFF8F8FC),
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFF0F172A)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Slide",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        if (!enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.10f))
            )
        }
    }
}

@Composable
fun InsightResultCard(
    insight: String,
    concepts: String
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color(0xFF6366F1).copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Posterior Insight",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(
                            AnnotatedString(insight + "\n\n" + concepts)
                        )
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = insight,
                    color = Color(0xFF334155),
                    fontSize = 15.sp,
                    lineHeight = 24.sp
                )
            }

            if (concepts.isNotBlank()) {
                Spacer(modifier = Modifier.height(22.dp))
                StructuredConceptsSection(concepts = concepts)
            }
        }
    }
}

@Composable
private fun StructuredConceptsSection(concepts: String) {
    val sections = remember(concepts) { parseConceptSections(concepts) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Key Concepts",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (sections.isEmpty()) {
            PlainConceptCard(
                title = "内容",
                body = concepts
            )
        } else {
            sections.forEachIndexed { index, section ->
                PlainConceptCard(
                    title = section.title,
                    body = section.body,
                    accentColor = when {
                        section.title.contains("课外") -> Color(0xFF6366F1)
                        section.title.contains("课内") -> Color(0xFFF59E0B)
                        section.title.contains("关联") -> Color(0xFF10B981)
                        section.title.contains("复习") || section.title.contains("练习") -> Color(0xFFEC4899)
                        else -> Color(0xFF64748B)
                    }
                )
                if (index != sections.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

private data class ConceptSection(
    val title: String,
    val body: String
)

private fun parseConceptSections(text: String): List<ConceptSection> {
    val normalized = text
        .replace("\r\n", "\n")
        .replace(Regex("""^\s*1\.\s*课外信息要点"""), "## 课外信息要点")
        .replace(Regex("""\n\s*2\.\s*课内简报要点"""), "\n## 课内简报要点")
        .replace(Regex("""\n\s*3\.\s*关联点列表"""), "\n## 关联点列表")
        .replace(Regex("""\n\s*4\.\s*建议的复习/练习路径"""), "\n## 建议的复习/练习路径")
        .trim()

    val regex = Regex(
        pattern = """##\s*(.+?)\n(.*?)(?=\n##\s*.+?$|\Z)""",
        option = RegexOption.DOT_MATCHES_ALL
    )

    return regex.findAll(normalized).map {
        ConceptSection(
            title = it.groupValues[1].trim(),
            body = it.groupValues[2].trim()
        )
    }.toList()
}

@Composable
private fun PlainConceptCard(
    title: String,
    body: String,
    accentColor: Color = Color(0xFF6366F1)
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accentColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formatConceptBody(body),
                color = Color(0xFF475569),
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

private fun formatConceptBody(text: String): String {
    return text
        .replace("**", "")
        .replace(" - ", "\n• ")
        .replace(Regex("""\n\s*-\s+"""), "\n• ")
        .replace(Regex("""\n\s*(\d+\))"""), "\n$1")
        .trim()
}
