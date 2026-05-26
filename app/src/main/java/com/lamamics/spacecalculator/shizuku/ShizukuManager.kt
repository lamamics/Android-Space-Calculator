package com.lamamics.spacecalculator.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.lamamics.spacecalculator.BuildConfig
import com.lamamics.spacecalculator.IUserService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/** High-level state of the Shizuku link, surfaced to the onboarding screen. */
enum class ShizukuStatus {
    NOT_INSTALLED,      // Shizuku app/binder absent
    NOT_RUNNING,        // installed but service not started (via ADB/root)
    PERMISSION_REQUIRED,// running, but the user hasn't granted us access
    READY,              // bound and usable
}

/**
 * Owns the Shizuku binder lifecycle, permission flow, and the bound
 * [IUserService]. A single instance lives on the Application.
 */
class ShizukuManager(private val appContext: Context) {

    private val _status = MutableStateFlow(ShizukuStatus.NOT_INSTALLED)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    @Volatile
    private var userService: IUserService? = null
    private var bindDeferred: CompletableDeferred<IUserService>? = null

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == PERMISSION_REQUEST_CODE) {
            refreshStatus()
            if (result != PackageManager.PERMISSION_GRANTED) {
                bindDeferred?.completeExceptionally(SecurityException("Shizuku permission denied"))
            }
        }
    }
    private val binderReceived = Shizuku.OnBinderReceivedListener { refreshStatus() }
    private val binderDead = Shizuku.OnBinderDeadListener {
        userService = null
        refreshStatus()
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = binder?.takeIf { it.pingBinder() }?.let { IUserService.Stub.asInterface(it) }
            userService = svc
            if (svc != null) {
                _status.value = ShizukuStatus.READY
                bindDeferred?.complete(svc)
            } else {
                bindDeferred?.completeExceptionally(IllegalStateException("Null user service binder"))
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
            refreshStatus()
        }
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(appContext.packageName, UserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("scan")
        .debuggable(BuildConfig.DEBUG)
        .version(SERVICE_VERSION)

    fun register() {
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        refreshStatus()
    }

    fun unregister() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
    }

    fun refreshStatus() {
        _status.value = when {
            !pingSafe() ->
                if (isShizukuInstalled()) ShizukuStatus.NOT_RUNNING else ShizukuStatus.NOT_INSTALLED
            !hasPermission() -> ShizukuStatus.PERMISSION_REQUIRED
            // Permission granted: usable. Binding happens lazily on first scan.
            else -> ShizukuStatus.READY
        }
    }

    private fun pingSafe(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    private fun hasPermission(): Boolean = runCatching {
        !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    private fun isShizukuInstalled(): Boolean = runCatching {
        appContext.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    }.getOrDefault(false)

    fun requestPermission() {
        if (pingSafe() && !hasPermission()) {
            runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
        }
    }

    /** Bind (if needed) and return the shell-privileged service. */
    suspend fun ensureService(): IUserService = withContext(Dispatchers.IO) {
        userService?.let { return@withContext it }
        if (!pingSafe()) error("Shizuku not running")
        if (!hasPermission()) error("Shizuku permission not granted")

        val deferred = CompletableDeferred<IUserService>()
        bindDeferred = deferred
        runCatching { Shizuku.bindUserService(userServiceArgs, connection) }
            .onFailure { deferred.completeExceptionally(it) }
        deferred.await()
    }

    /** Runs the scan in the shell process, writing the JSON tree to [outputPath]. */
    suspend fun runScan(rootPath: String, minSizeBytes: Long, outputPath: String) {
        withContext(Dispatchers.IO) {
            val svc = ensureService()
            val err = svc.scanToFile(rootPath, minSizeBytes, outputPath)
            if (err != null) error("Scan failed: $err")
        }
    }

    fun unbind() {
        runCatching { Shizuku.unbindUserService(userServiceArgs, connection, true) }
        userService = null
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 4011
        const val SERVICE_VERSION = 1
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val TAG = "SC-ShizukuManager"
    }
}
