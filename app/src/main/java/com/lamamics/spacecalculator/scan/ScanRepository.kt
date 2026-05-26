package com.lamamics.spacecalculator.scan

import android.content.Context
import com.lamamics.spacecalculator.model.Node
import com.lamamics.spacecalculator.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Orchestrates a scan. With Shizuku, the walk runs in the shell process and the
 * tree comes back as JSON; without it, we walk in-process (accessible paths
 * only). Either way the result is a single [Node] tree for the treemap.
 */
class ScanRepository(
    private val context: Context,
    private val shizuku: ShizukuManager,
) {
    /** Last in-process progress; Shizuku scans report only start/finish. */
    val progress = MutableStateFlow(0L to 0L) // filesSeen to bytesSeen

    suspend fun scan(rootPath: String, useShizuku: Boolean, minSizeBytes: Long): Node =
        withContext(Dispatchers.IO) {
            if (useShizuku) scanViaShizuku(rootPath, minSizeBytes)
            else scanInProcess(rootPath, minSizeBytes)
        }

    private suspend fun scanViaShizuku(rootPath: String, minSizeBytes: Long): Node {
        val out = File(outputDir(), "scan-${System.currentTimeMillis()}.json")
        shizuku.runScan(rootPath, minSizeBytes, out.absolutePath)
        val json = out.readText()
        out.delete()
        return Json.decodeFromString(Node.serializer(), json)
    }

    private fun scanInProcess(rootPath: String, minSizeBytes: Long): Node {
        val scanner = FileScanner(minSizeBytes) { files, bytes, _ ->
            progress.value = files to bytes
        }
        return scanner.scan(rootPath)
    }

    /** A directory both this app and the shell process can write/read. */
    private fun outputDir(): File =
        (context.externalCacheDir ?: context.cacheDir).also { it.mkdirs() }

    companion object {
        /** Default pruning threshold: 8 MB — tune from the UI later. */
        const val DEFAULT_MIN_SIZE = 8L * 1024 * 1024
    }
}
