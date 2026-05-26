package com.lamamics.spacecalculator.shizuku

import android.util.Log
import com.lamamics.spacecalculator.IUserService
import com.lamamics.spacecalculator.scan.FileScanner
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The actual code Shizuku runs in a shell-privileged process. Instantiated
 * reflectively by Shizuku, so it MUST keep a public no-arg constructor and
 * survive R8 (see proguard-rules.pro).
 */
class UserService : IUserService.Stub {

    @Suppress("unused")
    constructor() {
        Log.i(TAG, "UserService created in remote process")
    }

    override fun destroy() {
        Log.i(TAG, "destroy()")
        System.exit(0)
    }

    override fun exit() = destroy()

    override fun scanToFile(rootPath: String, minSizeBytes: Long, outputPath: String): String? {
        return try {
            val scanner = FileScanner(minSizeBytes)
            val tree = scanner.scan(rootPath)
            val json = Json.encodeToString(com.lamamics.spacecalculator.model.Node.serializer(), tree)
            File(outputPath).writeText(json)
            null
        } catch (t: Throwable) {
            Log.e(TAG, "scan failed", t)
            t.message ?: t.javaClass.simpleName
        }
    }

    private companion object {
        const val TAG = "SC-UserService"
    }
}
