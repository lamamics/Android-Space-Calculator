package com.lamamics.spacecalculator.model

/** A mountable volume the user can pick to scan (internal storage, SD card, USB...). */
data class StorageVolumeInfo(
    val label: String,
    val rootPath: String,
    val isRemovable: Boolean,
    val isPrimary: Boolean,
    val totalBytes: Long = 0,
    val freeBytes: Long = 0,
)
