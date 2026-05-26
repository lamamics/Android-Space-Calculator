package com.lamamics.spacecalculator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lamamics.spacecalculator.SpaceCalculatorApp
import com.lamamics.spacecalculator.model.AppInfo
import com.lamamics.spacecalculator.model.Node
import com.lamamics.spacecalculator.model.StorageVolumeInfo
import com.lamamics.spacecalculator.scan.ScanRepository
import com.lamamics.spacecalculator.shizuku.ShizukuStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val filesSeen: Long, val bytesSeen: Long) : ScanState
    data class Done(val root: Node) : ScanState
    data class Failed(val message: String) : ScanState
}

/** Detail shown in the bottom sheet when a tile is tapped. */
data class TileDetail(val node: Node, val owner: AppInfo?)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val application = app as SpaceCalculatorApp
    private val repo = application.scanRepository
    private val shizuku = application.shizukuManager
    private val stats = application.statsProvider
    private val volumeProvider = application.volumeProvider

    val shizukuStatus: StateFlow<ShizukuStatus> = shizuku.status

    private val _volumes = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
    val volumes: StateFlow<List<StorageVolumeInfo>> = _volumes.asStateFlow()

    private val _selectedVolume = MutableStateFlow<StorageVolumeInfo?>(null)
    val selectedVolume: StateFlow<StorageVolumeInfo?> = _selectedVolume.asStateFlow()

    private val _useShizuku = MutableStateFlow(true)
    val useShizuku: StateFlow<Boolean> = _useShizuku.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    /** Drill-down stack: first element is the scanned root, last is what's shown. */
    private val _navStack = MutableStateFlow<List<Node>>(emptyList())
    val navStack: StateFlow<List<Node>> = _navStack.asStateFlow()

    private val _detail = MutableStateFlow<TileDetail?>(null)
    val detail: StateFlow<TileDetail?> = _detail.asStateFlow()

    /** Per-app attribution table (the "by application" view). Loaded on demand. */
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _appsLoading = MutableStateFlow(false)
    val appsLoading: StateFlow<Boolean> = _appsLoading.asStateFlow()

    private val _showAppList = MutableStateFlow(false)
    val showAppList: StateFlow<Boolean> = _showAppList.asStateFlow()

    fun openAppList() {
        _showAppList.value = true
        if (_apps.value.isEmpty()) loadApps()
    }

    fun closeAppList() { _showAppList.value = false }

    fun loadApps() {
        viewModelScope.launch {
            _appsLoading.value = true
            _apps.value = withContext(Dispatchers.IO) {
                runCatching { stats.statsForAllApps() }.getOrDefault(emptyList())
            }
            _appsLoading.value = false
        }
    }

    val currentNode: StateFlow<Node?> = _navStack
        .map { it.lastOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun refreshShizuku() = shizuku.refreshStatus()
    fun requestShizukuPermission() = shizuku.requestPermission()
    fun setUseShizuku(value: Boolean) { _useShizuku.value = value }
    fun hasUsageAccess() = stats.hasUsageAccess()

    fun loadVolumes() {
        viewModelScope.launch {
            val vols = runCatching { volumeProvider.volumes() }.getOrDefault(emptyList())
            _volumes.value = vols
            if (_selectedVolume.value == null) _selectedVolume.value = vols.firstOrNull()
        }
    }

    fun selectVolume(volume: StorageVolumeInfo) { _selectedVolume.value = volume }

    fun startScan() {
        val volume = _selectedVolume.value ?: return
        // Adaptive pruning: small enough to reveal real content (photos, etc.),
        // relative to how much data we're scanning so huge volumes stay bounded.
        val used = (volume.totalBytes - volume.freeBytes).coerceAtLeast(0L)
        val minSizeBytes = (used / 20_000).coerceIn(64L * 1024, 32L * 1024 * 1024)
        _scanState.value = ScanState.Scanning(0, 0)
        viewModelScope.launch {
            // Mirror in-process progress into the UI state.
            val progressJob = launch {
                repo.progress.collect { (files, bytes) ->
                    if (_scanState.value is ScanState.Scanning) {
                        _scanState.value = ScanState.Scanning(files, bytes)
                    }
                }
            }
            val result = runCatching {
                repo.scan(volume.rootPath, _useShizuku.value, minSizeBytes)
            }
            progressJob.cancel()
            result
                .onSuccess { scanned ->
                    val root = withFreeSpace(scanned, volume.freeBytes)
                    _navStack.value = listOf(root)
                    _scanState.value = ScanState.Done(root)
                }
                .onFailure { _scanState.value = ScanState.Failed(it.message ?: "Erreur inconnue") }
        }
    }

    /** Append a synthetic free-space tile at the root so the treemap covers the
     *  whole volume (used + free), like the system storage bar. */
    private fun withFreeSpace(root: Node, freeBytes: Long): Node {
        if (freeBytes <= 0) return root
        val freeNode = Node(
            name = "Espace libre",
            path = "(free)",
            size = freeBytes,
            isDirectory = false,
            isReadable = true,
            isFree = true,
        )
        return root.copy(
            size = root.size + freeBytes,
            childCount = root.childCount + 1,
            children = (root.children + freeNode).sortedByDescending { it.size },
        )
    }

    fun navigateInto(node: Node) {
        if (node.isDirectory && node.children.isNotEmpty()) {
            _navStack.value = _navStack.value + node
        }
    }

    /** Jump to a given depth in the breadcrumb (0 = root). */
    fun navigateTo(depth: Int) {
        val stack = _navStack.value
        if (depth in stack.indices) _navStack.value = stack.subList(0, depth + 1)
    }

    fun navigateUp(): Boolean {
        val stack = _navStack.value
        return if (stack.size > 1) { _navStack.value = stack.dropLast(1); true } else false
    }

    fun selectTile(node: Node) {
        val owner = node.ownerPackage?.let { runCatching { stats.statsForPackage(it) }.getOrNull() }
        _detail.value = TileDetail(node, owner)
    }

    fun dismissDetail() { _detail.value = null }

    fun reset() {
        _scanState.value = ScanState.Idle
        _navStack.value = emptyList()
        _detail.value = null
    }
}
