package com.lamamics.spacecalculator.util

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

enum class MediaKind { IMAGE, VIDEO, OTHER }

private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
private val VIDEO_EXT = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "m4v")

fun mediaKind(name: String): MediaKind =
    when (name.substringAfterLast('.', "").lowercase()) {
        in IMAGE_EXT -> MediaKind.IMAGE
        in VIDEO_EXT -> MediaKind.VIDEO
        else -> MediaKind.OTHER
    }

/**
 * Decodes a thumbnail for a media file. Returns null if the file is unreadable
 * by this process (e.g. inside Android/data, only listed via Shizuku) or not a
 * media type. Call off the main thread.
 */
fun loadThumbnail(path: String, kind: MediaKind, sizePx: Int): ImageBitmap? {
    val file = File(path)
    if (!file.canRead()) return null
    val size = Size(sizePx, sizePx)
    val bmp: Bitmap? = runCatching {
        when (kind) {
            MediaKind.IMAGE -> ThumbnailUtils.createImageThumbnail(file, size, null)
            MediaKind.VIDEO -> ThumbnailUtils.createVideoThumbnail(file, size, null)
            MediaKind.OTHER -> null
        }
    }.getOrNull()
    return bmp?.asImageBitmap()
}
