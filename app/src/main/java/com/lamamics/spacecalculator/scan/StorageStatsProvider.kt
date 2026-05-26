package com.lamamics.spacecalculator.scan

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import com.lamamics.spacecalculator.model.AppInfo

/**
 * Per-app storage attribution via StorageStatsManager. Requires the "Usage
 * access" special permission, granted from system settings (see
 * [hasUsageAccess] / [usageAccessIntentAction]). Returns aggregate sizes per
 * package — the building block for the "Other" attribution view.
 */
class StorageStatsProvider(private val context: Context) {

    private val statsManager =
        context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    private val pm: PackageManager = context.packageManager
    private val resolver = AppResolver(context)

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Stats for every installed app on the default (internal) volume. */
    fun statsForAllApps(): List<AppInfo> {
        if (!hasUsageAccess()) return emptyList()
        val user: UserHandle = Process.myUserHandle()
        val uuid = StorageManager.UUID_DEFAULT
        return pm.getInstalledApplications(0).mapNotNull { ai ->
            val stats = runCatching {
                statsManager.queryStatsForPackage(uuid, ai.packageName, user)
            }.getOrNull() ?: return@mapNotNull null
            AppInfo(
                packageName = ai.packageName,
                label = resolver.displayName(ai.packageName),
                appBytes = stats.appBytes,
                dataBytes = stats.dataBytes,
                cacheBytes = stats.cacheBytes,
            )
        }.sortedByDescending { it.totalBytes }
    }

    fun statsForPackage(packageName: String): AppInfo? {
        if (!hasUsageAccess()) return null
        val stats = runCatching {
            statsManager.queryStatsForPackage(
                StorageManager.UUID_DEFAULT, packageName, Process.myUserHandle()
            )
        }.getOrNull() ?: return null
        return AppInfo(
            packageName = packageName,
            label = resolver.displayName(packageName),
            appBytes = stats.appBytes,
            dataBytes = stats.dataBytes,
            cacheBytes = stats.cacheBytes,
        )
    }

    companion object {
        const val usageAccessIntentAction = "android.settings.USAGE_ACCESS_SETTINGS"
    }
}
