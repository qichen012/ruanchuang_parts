package com.example.help_stu_agent.ui.openSource

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.help_stu_agent.designsystem.components.GithubIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourcePage(
    onBack: () -> Unit,
    viewModel: OpenSourceViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf(uiState.query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F6)) // 匹配图片的浅米色背景
            .statusBarsPadding()
    ) {
        // 保留原来的导航栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E293B)
                )
            }
            Text(
                text = "Open Source Explorer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxSize()
        ) {
            // 优化后的检索框：胶囊形状 + 阴影
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = Color.Black.copy(alpha = 0.1f))
                    .background(Color.White, CircleShape),
                placeholder = { 
                    Text("Search repositories...", color = Color(0xFF94A3B8), fontSize = 15.sp) 
                },
                leadingIcon = { 
                    Icon(Icons.Default.Search, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(24.dp)) 
                },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    cursorColor = Color.Black,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchText.isNotBlank()) {
                            viewModel.loadProjects(searchText.trim())
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1E293B))
                    }
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                        Button(
                            onClick = { viewModel.loadProjects(uiState.query) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) { Text("Retry") }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(uiState.projects) { project ->
                            GitHubProjectCard(
                                project = project,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(project.url))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GitHubProjectCard(
    project: GitHubProjectInfo,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp), // 大圆角卡片
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 作者/项目 Logo
                if (project.isAvatar && project.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = project.avatarUrl,
                        contentDescription = project.author,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = GithubIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = project.author,
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // 右上角打开按钮
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF8F9FA), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = "Open",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 项目描述
            Text(
                text = project.description,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFF475569),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // 底部统计栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFB800), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = project.stars, fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.width(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AltRoute, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = project.forks, fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.weight(1f))

                // 标签
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8F9FA)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.MenuBook, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = project.tag.uppercase(),
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
