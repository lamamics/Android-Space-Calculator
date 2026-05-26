package com.lamamics.spacecalculator.util

import java.util.Locale

/** Human-readable byte sizes, e.g. 51.92 Go (decimal units, like the system UI). */
fun formatBytes(bytes: Long): String {
    if (bytes < 1000) return "$bytes o"
    val units = arrayOf("ko", "Mo", "Go", "To", "Po")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1000 && unit < units.lastIndex) {
        value /= 1000.0
        unit++
    }
    return String.format(Locale.getDefault(), "%.2f %s", value, units[unit])
}
