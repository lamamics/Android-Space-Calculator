package com.lamamics.spacecalculator.model

/**
 * Per-application storage attribution. [appBytes]/[dataBytes]/[cacheBytes] come
 * from StorageStatsManager; [label] from PackageManager. Used by the "Other"
 * detail view to answer "which app does this belong to, and how much".
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val appBytes: Long = 0,
    val dataBytes: Long = 0,
    val cacheBytes: Long = 0,
) {
    val totalBytes: Long get() = appBytes + dataBytes + cacheBytes
}
