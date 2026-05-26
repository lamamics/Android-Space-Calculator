package com.lamamics.spacecalculator.scan

import android.content.Context
import android.os.storage.StorageManager
import com.lamamics.spacecalculator.model.StorageVolumeInfo
import java.io.File

/** Lists mountable volumes (internal storage, SD card, USB) the user can scan. */
class VolumeProvider(private val context: Context) {

    fun volumes(): List<StorageVolumeInfo> {
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        return sm.storageVolumes.mapNotNull { vol ->
            val dir: File = vol.directory ?: return@mapNotNull null
            StorageVolumeInfo(
                label = vol.getDescription(context) ?: dir.name,
                rootPath = dir.absolutePath,
                isRemovable = vol.isRemovable,
                isPrimary = vol.isPrimary,
                totalBytes = runCatching { dir.totalSpace }.getOrDefault(0L),
                freeBytes = runCatching { dir.freeSpace }.getOrDefault(0L),
            )
        }.sortedByDescending { it.isPrimary }
    }
}
