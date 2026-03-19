package com.example.help_stu_agent.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.help_stu_agent.designsystem.components.GithubIcon

data class FeatureItemData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val onClick: () -> Unit
)

@Composable
fun FeaturesPage(
    onGoUploadPdf: () -> Unit,
    onGoUploadPhoto: () -> Unit,
    onGoOpenSource: () -> Unit,
    onGoDailyReport: () -> Unit,
    onGoKnowledgeStructure: () -> Unit,
    onGoSparkyLink: () -> Unit,
    onGoEliteIdeas: () -> Unit,
    onGoEruditionLab: () -> Unit,
    onGoMeetingMinutes: () -> Unit,
    onGoMeetingHistory: () -> Unit
) {

    val features = listOf(
        FeatureItemData("Visual AI", "Photo analysis", Icons.Outlined.CameraAlt, Color(0xFFF1EAFF), onGoUploadPhoto),
        FeatureItemData("Daily Briefing", "Review your day", Icons.AutoMirrored.Outlined.ListAlt, Color(0xFFE8EFFF), onGoDailyReport),
        FeatureItemData("Elite Ideas", "Top tier insights", Icons.Outlined.Lightbulb, Color(0xFFFFF4E5), onGoEliteIdeas),
        FeatureItemData("Sparky Link", "Connect ideas", Icons.Outlined.Link, Color(0xFFE6F7ED), onGoSparkyLink),
        FeatureItemData("Erudition Lab", "Deep dive research", Icons.Outlined.Science, Color(0xFFFFF0F5), onGoEruditionLab),
        FeatureItemData("Meet Memo", "Voice to text", Icons.Outlined.Mic, Color(0xFFE8F4FD), onGoMeetingMinutes)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F9FE))
            .statusBarsPadding()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        "WORKSPACE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tools & Features",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }


            item(span = { GridItemSpan(maxLineSpan) }) {
                AiUploadCenterCard(
                    onPdfClick = onGoUploadPdf,
                    onPhotoClick = onGoUploadPhoto
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                KnowledgeTreeBannerCard(onClick = onGoKnowledgeStructure)
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                OpenSourceCard(onClick = onGoOpenSource)
            }

            items(features) { feature ->
                FeatureCard(feature)
            }
        }
    }
}

@Composable
fun AiUploadCenterCard(onPdfClick: () -> Unit, onPhotoClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF101613).copy(alpha = 0.4f))
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(colors = listOf(Color(0xFF242A27), Color(0xFF121614))))
            .clickable { onPdfClick() } 
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFF2E3834), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "INTELLIGENCE HUB",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("AI Upload Center", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Unified space for PDF and Visual\nAI processing.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF9CA3AF)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Color(0xFF2A3430), RoundedCornerShape(20.dp))
                    .clickable { onPhotoClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "Image",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.TopStart)
                    )
                    Icon(
                        imageVector = Icons.Outlined.UploadFile,
                        contentDescription = "Upload",
                        tint = Color(0xFF34D399),
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}

@Composable
fun KnowledgeTreeBannerCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFF0FDF4), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountTree,
                    contentDescription = null,
                    tint = Color(0xFF047857),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Knowledge Tree",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Visualize your learning\nconnections.",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                onClick = onClick,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(0xFF059669)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OpenSourceCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = GithubIcon,
                    contentDescription = "GitHub",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Open Source", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Explore curated GitHub projects.", fontSize = 13.sp, color = Color(0xFF94A3B8), lineHeight = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                onClick = onClick,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(0xFF1E1E1E)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureCard(feature: FeatureItemData) {
    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp).clickable { feature.onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(feature.iconBgColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(feature.icon, null, tint = Color(0xFF1E293B), modifier = Modifier.size(24.dp))
            }
            Column {
                Text(feature.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text(feature.subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}