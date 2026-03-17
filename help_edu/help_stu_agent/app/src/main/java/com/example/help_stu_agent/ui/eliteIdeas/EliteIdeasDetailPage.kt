package com.example.help_stu_agent.ui.eliteIdeas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EliteIdeaDetailPage(
    ideaId: String,
    onBack: () -> Unit
) {

    val mockTitle = "期权 Delta 对冲：金融工程中的动态平衡"
    val mockCategory = "PHILOSOPHY"
    val mockContent = """
        在金融衍生品交易的波涛汹涌中，Delta 对冲（Delta Hedging）是做市商和机构投资者保持风险中性的核心策略。它不仅仅是数学计算，更是一种动态的平衡艺术。
        
        什么是 Delta?
        
        Delta (Δ) 衡量的是期权价格对标的资产价格变动的敏感度。
        
        (此处省略更多详细内容...)
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. 顶部头图与返回按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Image(
                painter = painterResource(id = android.R.drawable.ic_menu_gallery), // 替换为你的真实图片资源
                contentDescription = "Header Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 沉浸式返回按钮
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 48.dp, start = 16.dp) // 根据状态栏高度调整 top padding
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // 2. 详情内容区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // 分类标签
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = mockCategory,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 标题
            Text(
                text = mockTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 34.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 正文
            Text(
                text = mockContent,
                fontSize = 16.sp,
                lineHeight = 28.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        }
    }
}