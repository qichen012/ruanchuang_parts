package com.example.help_stu_agent.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Description

@Composable
fun HomeTopBar(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit,
    onRightActionClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val chipBg = cs.surface.copy(alpha = 0.92f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(chipBg)
        ) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Menu",
                tint = cs.onSurface
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onRightActionClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(chipBg)
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = "Notes",
                tint = cs.onSurface
            )
        }
    }
}
