package com.lamamics.spacecalculator.ui.theme

import androidx.compose.ui.graphics.Color
import com.lamamics.spacecalculator.model.Node

/** Maps a node to a fill color by category, echoing the system storage screen. */
object TreemapColors {

    val UNREADABLE = Color(0xFFB0BEC5)
    val RESIDUAL = Color(0xFFCFD8DC)   // pruned "small files" tile
    val FOLDER = Color(0xFFFFE082)
    val FREE = Color(0xFFE0E0E0)       // free space (neutral light grey, like the system bar)

    // App data / cache (under Android/data or Android/obb) — the bulk of "Other".
    val APP_FOLDER = Color(0xFF80DEEA) // cyan tint for app data/cache folders
    val APP_FILE = Color(0xFF26C6DA)   // cyan for app data/cache files

    private val APP_DATA_REGEX = Regex("/Android/(data|obb)(/|$)")

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
        val isAppData = APP_DATA_REGEX.containsMatchIn(node.path)
        if (node.isDirectory) return if (isAppData) APP_FOLDER else FOLDER
        if (isAppData) return APP_FILE   // app data/cache, regardless of extension
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

    /** Color → label pairs for the on-screen legend. */
    val legend: List<Pair<Color, String>> = listOf(
        FOLDER to "Dossier",
        videos to "Vidéo",
        images to "Image",
        audio to "Audio",
        docs to "Document",
        archives to "Archive",
        apk to "App (APK/OBB)",
        APP_FILE to "Données/cache d'app",
        other to "Autre fichier",
        FREE to "Espace libre",
        RESIDUAL to "Petits fichiers",
        UNREADABLE to "Non lisible",
    )
}
