package com.lamamics.spacecalculator.scan

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** Resolves package names to human labels/icons, with a small cache. */
class AppResolver(context: Context) {

    private val pm: PackageManager = context.packageManager
    private val labelCache = HashMap<String, String?>()

    fun label(packageName: String): String? = labelCache.getOrPut(packageName) {
        runCatching {
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        }.getOrNull()
    }

    /** Best-effort display name: app label if known, otherwise the package id. */
    fun displayName(packageName: String): String = label(packageName) ?: packageName

    fun icon(packageName: String): Drawable? = runCatching {
        pm.getApplicationIcon(packageName)
    }.getOrNull()
}
