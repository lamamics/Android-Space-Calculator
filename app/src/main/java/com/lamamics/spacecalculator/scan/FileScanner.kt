package com.lamamics.spacecalculator.scan

import com.lamamics.spacecalculator.model.Node
import java.io.File

/**
 * Pure JVM recursive walker — no Android Context — so it can run unchanged
 * inside the Shizuku shell process. Computes accurate directory sizes while
 * pruning the tree: only children >= [minSizeBytes] are retained, but every
 * byte stays counted in the parent's size.
 */
class FileScanner(
    private val minSizeBytes: Long,
    /** Hard cap on children kept per directory (largest first), bounds tree size. */
    private val maxChildrenPerDir: Int = 400,
    /** Invoked periodically with (filesSeen, bytesSeen, currentPath). */
    private val onProgress: ((Long, Long, String) -> Unit)? = null,
) {
    private var filesSeen = 0L
    private var bytesSeen = 0L

    fun scan(rootPath: String): Node {
        val root = File(rootPath)
        return walk(root)
    }

    private fun walk(file: File): Node {
        val owner = ownerPackageOf(file.absolutePath)

        if (!file.isDirectory) {
            val len = runCatching { file.length() }.getOrDefault(0L)
            filesSeen++
            bytesSeen += len
            if (filesSeen % PROGRESS_EVERY == 0L) {
                onProgress?.invoke(filesSeen, bytesSeen, file.absolutePath)
            }
            return Node(
                name = file.name,
                path = file.absolutePath,
                size = len,
                isDirectory = false,
                isReadable = true,
                ownerPackage = owner,
            )
        }

        // Directory.
        val entries = file.listFiles()
        if (entries == null) {
            // Unreadable directory (blocked zone / permission). Report it as a
            // leaf so the UI can show it honestly as "non lisible".
            onProgress?.invoke(filesSeen, bytesSeen, file.absolutePath)
            return Node(
                name = file.name,
                path = file.absolutePath,
                size = 0L,
                isDirectory = true,
                isReadable = false,
                ownerPackage = owner,
            )
        }

        val childNodes = ArrayList<Node>(entries.size)
        var total = 0L
        for (entry in entries) {
            // Skip symlinks to avoid loops and double counting.
            if (isSymlink(entry)) continue
            val child = walk(entry)
            total += child.size
            childNodes.add(child)
        }

        val kept = childNodes
            .filter { it.size >= minSizeBytes }
            .sortedByDescending { it.size }
            .take(maxChildrenPerDir)

        return Node(
            name = file.name,
            path = file.absolutePath,
            size = total,
            isDirectory = true,
            isReadable = true,
            ownerPackage = owner,
            childCount = childNodes.size,
            children = kept,
        )
    }

    private fun isSymlink(file: File): Boolean = runCatching {
        file.canonicalFile != file.absoluteFile
    }.getOrDefault(false)

    companion object {
        private const val PROGRESS_EVERY = 2000L

        private val OWNER_REGEX =
            Regex("/Android/(?:data|obb|media)/([A-Za-z][A-Za-z0-9_.]+)(?:/|$)")

        /** Extract the owning package from an Android/data|obb|media/<pkg> path. */
        fun ownerPackageOf(path: String): String? =
            OWNER_REGEX.find(path)?.groupValues?.get(1)
    }
}
