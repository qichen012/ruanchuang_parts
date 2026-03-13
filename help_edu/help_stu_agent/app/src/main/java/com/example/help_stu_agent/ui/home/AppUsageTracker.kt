package com.example.help_stu_agent.ui.home

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun AppUsageTracker(
    userId: Int,
    viewModel: UserViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var startTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    startTime = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_STOP -> {
                    if (startTime > 0) {
                        val endTime = System.currentTimeMillis()
                        val durationSeconds = ((endTime - startTime) / 1000).toInt()

                        if (durationSeconds > 5) {
                            viewModel.recordUsage(userId, startTime, endTime, durationSeconds)
                        }
                        startTime = 0L
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}