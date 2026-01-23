package com.example.help_stu_agent.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome

@Composable
fun TodayActionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(26.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {

            // 左上小图标块
            val iconBg = cs.primary.copy(alpha = 0.10f)
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg)
                    .padding(10.dp)
            )

            Spacer(modifier = Modifier.size(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = cs.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface.copy(alpha = 0.70f),
                modifier = Modifier.padding(top = 10.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onPrimaryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary
                )
            ) {
                Text(text = primaryButtonText, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
