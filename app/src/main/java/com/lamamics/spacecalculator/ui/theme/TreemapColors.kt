package com.lamamics.spacecalculator.ui.theme

import androidx.compose.ui.graphics.Color
import com.lamamics.spacecalculator.model.Node

/** Maps a node to a fill color by category, echoing the system storage screen. */
object TreemapColors {

    val UNREADABLE = Color(0xFFB0BEC5)
    val RESIDUAL = Color(0xFFCFD8DC)   // pruned "small files" tile
    val FOLDER = Color(0xFFFFE082)
    val FREE = Color(0xFFE0E0E0)       // free space (neutral light grey, like the system bar)

    private val images = Color(0xFFEF7C71)
    private val videos = Color(0xFFC79BE8)
    private val audio = Color(0xFF8FA8FF)
    private val docs = Color(0xFFD2B48C)
    private val archives = Color(0xFF90C695)
    private val apk = Color(0xFFAEDC52)
    private val other = Color(0xFF80CBC4)

    fun colorFor(node: Node?): Color {
        if (node == null) return RESIDUAL
        if (node.isFree) return FREE
        if (!node.isReadable) return UNREADABLE
        if (node.isDirectory) return FOLDER
        return when (node.name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> images
            "mp4", "mkv", "avi", "mov", "webm", "3gp", "m4v" -> videos
            "mp3", "aac", "flac", "wav", "ogg", "m4a", "opus" -> audio
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub" -> docs
            "zip", "rar", "7z", "gz", "tar", "xz" -> archives
            "apk", "obb", "xapk" -> apk
            else -> other
        }
    }
}
