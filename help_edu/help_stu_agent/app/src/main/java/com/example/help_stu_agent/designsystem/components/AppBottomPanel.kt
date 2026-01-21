package com.example.help_stu_agent.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.help_stu_agent.designsystem.tokens.AppTokens

@Composable
fun AppBottomPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AppTokens.Space.xxl),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppTokens.Elevation.panel)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
