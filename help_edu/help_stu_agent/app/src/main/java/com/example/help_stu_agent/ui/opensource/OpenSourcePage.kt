package com.example.help_stu_agent.ui.opensource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GitHubProjectInfo(
    val title: String,
    val author: String,
    val description: String,
    val stars: String,
    val forks: String,
    val tag: String,
    val isAvatar: Boolean // 用来区分左侧头像是图标还是人像占位
)

@Composable
fun OpenSourcePage(
    onBack: () -> Unit
) {
    val projects = listOf(
        GitHubProjectInfo(
            title = "build-your-own-x",
            author = "@codecrafters-io",
            description = "Master programming by recreating your favorite technologies from scratch.",
            stars = "471.0k",
            forks = "44.2k",
            tag = "MARKDOWN",
            isAvatar = false
        ),
        GitHubProjectInfo(
            title = "awesome",
            author = "@sindresorhus",
            description = "Awesome lists about all kinds of interesting topics",
            stars = "441.9k",
            forks = "33.4k",
            tag = "DOCS",
            isAvatar = true
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F9FE))
            .statusBarsPadding()
    ) {
        // 顶部导航栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
            }
            Text(
                text = "Open Source Explorer",
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }

        // 大标题区
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GitHub Projects",
                fontSize = 38.sp,
                fontFamily = FontFamily.Serif,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Discover world-class open source projects and their contributions to the community.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 滚动列表
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(projects) { project ->
                    GitHubProjectCard(project)
                }
            }
        }
    }
}

@Composable
fun GitHubProjectCard(project: GitHubProjectInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: 头像/图标 + 标题 + 外链图标
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (project.isAvatar) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEDD5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐶", fontSize = 28.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AltRoute, // 借用类似分支的图标
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = project.author,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = "Open",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Description
            Text(
                text = project.description,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFF475569),
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )

            // Footer: Stars, Forks, Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stars
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Stars",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = project.stars,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.width(20.dp))

                // Forks
                Icon(
                    imageVector = Icons.Outlined.AltRoute,
                    contentDescription = "Forks",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = project.forks,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = project.tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}