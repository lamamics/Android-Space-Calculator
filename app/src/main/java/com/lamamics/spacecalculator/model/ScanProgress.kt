package com.lamamics.spacecalculator.model

/** Streamed from the scan engine while it walks the filesystem. */
data class ScanProgress(
    val filesSeen: Long = 0,
    val bytesSeen: Long = 0,
    val currentPath: String = "",
)
