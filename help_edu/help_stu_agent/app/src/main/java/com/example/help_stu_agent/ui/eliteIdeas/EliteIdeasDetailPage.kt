package com.example.help_stu_agent.ui.eliteIdeas

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import androidx.core.net.toUri

@Composable
fun EliteIdeaDetailPage(
    instance: RealizationInstance,
    category: String = "",
    onBack: () -> Unit
) {
    val headerImageModel = when {
        instance.localImagePath.isNotBlank() -> File(instance.localImagePath)
        !instance.imageUrl.isNullOrBlank() -> instance.imageUrl
        else -> null
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            if (headerImageModel != null) {
                AsyncImage(
                    model = headerImageModel,
                    contentDescription = instance.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE9ECF3))
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color(0xFFF7F8FC).copy(alpha = 0.8f),
                                Color(0xFFF7F8FC)
                            )
                        )
                    )
            )

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(start = 16.dp, top = 48.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                if (category.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFF315EFB).copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF315EFB)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                Text(
                    text = instance.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp,
                    color = Color(0xFF111827)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-8).dp)
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 1.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp)
                ) {
                    if (instance.description.isNotBlank()) {
                        DetailSectionTitle("Overview")

                        Spacer(modifier = Modifier.height(14.dp))

                        DetailBodyText(
                            text = cleanMarkdownText(instance.description)
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    instance.searchAndGenerate?.let { sg ->
                        val hasConcept = sg.concept.isNotBlank()
                        val hasExpansion = sg.expansion.isNotBlank()
                        val hasSummary = sg.zipped.isNotBlank()

                        if (hasConcept) {
                            DetailSectionTitle("Core Concept")
                            Spacer(modifier = Modifier.height(14.dp))
                            DetailBodyText(
                                text = cleanMarkdownText(sg.concept)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        if (hasExpansion) {
                            DetailSectionTitle("Deep Insights")
                            Spacer(modifier = Modifier.height(14.dp))

                            RichLinkTextCard(
                                text = cleanMarkdownText(sg.expansion)
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        if (hasSummary) {
                            DetailSectionTitle("Executive Summary")
                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(all = 18.dp)
                                ) {
                                    RichLinkText(
                                        text = cleanMarkdownText(sg.zipped),
                                        isSummary = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailSectionTitle(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF315EFB))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF111827),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun DetailBodyText(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        lineHeight = 28.sp,
        color = Color(0xFF374151),
        style = LocalTextStyle.current.copy(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
        )
    )
}

@Composable
private fun RichLinkTextCard(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFD),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 18.dp)
        ) {
            if (containsUrl(text)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = Color(0xFF315EFB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Related Resources",
                        fontSize = 13.sp,
                        color = Color(0xFF315EFB),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            RichLinkText(text = text)
        }
    }
}

@Composable
private fun RichLinkText(text: String, isSummary: Boolean = false) {
    val context = LocalContext.current
    val annotated = remember(text) { buildLinkAnnotatedString(text) }

    ClickableText(
        text = annotated,
        style = LocalTextStyle.current.copy(
            fontSize = if (isSummary) 15.sp else 16.sp,
            lineHeight = if (isSummary) 26.sp else 28.sp,
            color = if (isSummary) Color(0xFF1E293B) else Color(0xFF374151),
            fontWeight = if (isSummary) FontWeight.Medium else FontWeight.Normal
        ),
        onClick = { offset ->
            annotated
                .getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.let { ann ->
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, ann.item.toUri())
                        )
                    }
                }
        }
    )
}

private fun buildLinkAnnotatedString(text: String): AnnotatedString {
    val urlRegex = Regex("""(https?://\S+)""")

    return buildAnnotatedString {
        var currentIndex = 0

        urlRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            val url = match.value

            if (currentIndex < start) {
                append(text.substring(currentIndex, start))
            }

            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(
                SpanStyle(
                    color = Color(0xFF315EFB),
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(url)
            }
            pop()

            currentIndex = end
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

private fun containsUrl(text: String): Boolean {
    val urlRegex = Regex("""(https?://\S+)""")
    return urlRegex.containsMatchIn(text)
}

private fun cleanMarkdownText(text: String): String {
    return text
        .replace("**", "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .replace("\r\n", "\n")
        .trim()
}
