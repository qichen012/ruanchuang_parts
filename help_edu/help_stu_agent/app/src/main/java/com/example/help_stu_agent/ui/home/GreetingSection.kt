package com.example.help_stu_agent.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun GreetingSection(
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    val greet = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    Column(modifier = modifier.padding(horizontal = 22.dp)) {
        Text(
            text = greet,
            style = MaterialTheme.typography.headlineLarge,
            color = cs.onBackground
        )
        Text(
            text = "Ready to reflect on your day?",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onBackground.copy(alpha = 0.68f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

