package com.example.help_stu_agent.ui.meetingMem

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun copyUriToLocalCache(context: Context, uri: Uri): String? {
    return try {
        val contentResolver = context.contentResolver
        // 获取真实后缀名
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri)) ?: "m4a"
        val dir = File(context.cacheDir, "meeting_audio").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(dir, "uploaded_$ts.$extension")

        contentResolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        outFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}