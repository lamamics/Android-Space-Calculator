package com.lamamics.spacecalculator

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lamamics.spacecalculator.scan.StorageStatsProvider
import com.lamamics.spacecalculator.ui.MainViewModel
import com.lamamics.spacecalculator.ui.ScanState
import com.lamamics.spacecalculator.ui.components.DetailBottomSheet
import com.lamamics.spacecalculator.ui.screens.ScanningScreen
import com.lamamics.spacecalculator.ui.screens.SetupCallbacks
import com.lamamics.spacecalculator.ui.screens.SetupScreen
import com.lamamics.spacecalculator.ui.screens.TreemapScreen
import com.lamamics.spacecalculator.ui.theme.SpaceCalculatorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SpaceCalculatorTheme { Root() } }
    }

    @Composable
    private fun Root() {
        val vm: MainViewModel = viewModel()
        // Recompute permission flags on each recomposition triggered by refresh.
        var permTick by remember { mutableStateOf(0) }

        val shizukuStatus by vm.shizukuStatus.collectAsStateWithLifecycle()
        val scanState by vm.scanState.collectAsStateWithLifecycle()
        val volumes by vm.volumes.collectAsStateWithLifecycle()
        val selectedVolume by vm.selectedVolume.collectAsStateWithLifecycle()
        val useShizuku by vm.useShizuku.collectAsStateWithLifecycle()
        val navStack by vm.navStack.collectAsStateWithLifecycle()
        val current by vm.currentNode.collectAsStateWithLifecycle()
        val detail by vm.detail.collectAsState()
        val showAppList by vm.showAppList.collectAsStateWithLifecycle()
        val apps by vm.apps.collectAsStateWithLifecycle()
        val appsLoading by vm.appsLoading.collectAsStateWithLifecycle()

        // Load volumes once.
        androidx.compose.runtime.LaunchedEffect(Unit) { vm.loadVolumes() }

        Scaffold(Modifier.fillMaxSize()) { padding ->
          androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding)) {
            if (showAppList) {
                com.lamamics.spacecalculator.ui.screens.AppListScreen(
                    apps = apps,
                    loading = appsLoading,
                    hasUsageAccess = vm.hasUsageAccess(),
                    onBack = vm::closeAppList,
                    onOpenUsageAccess = ::openUsageAccess,
                    onRefresh = vm::loadApps,
                )
            } else {
            when (val state = scanState) {
                is ScanState.Idle -> {
                    permTick // read to recompute on refresh
                    SetupScreen(
                        shizukuStatus = shizukuStatus,
                        useShizuku = useShizuku,
                        hasAllFilesAccess = hasAllFilesAccess(),
                        hasUsageAccess = vm.hasUsageAccess(),
                        volumes = volumes,
                        selectedVolume = selectedVolume,
                        cb = SetupCallbacks(
                            onRequestShizuku = vm::requestShizukuPermission,
                            onOpenAllFilesAccess = ::openAllFilesAccess,
                            onOpenUsageAccess = ::openUsageAccess,
                            onToggleShizuku = vm::setUseShizuku,
                            onSelectVolume = vm::selectVolume,
                            onStartScan = vm::startScan,
                            onRefresh = { vm.refreshShizuku(); permTick++ },
                            onOpenAppList = vm::openAppList,
                        ),
                    )
                }

                is ScanState.Scanning ->
                    ScanningScreen(state.filesSeen, state.bytesSeen, useShizuku)

                is ScanState.Failed -> ErrorView(state.message, onRetry = vm::reset)

                is ScanState.Done -> {
                    val node = current
                    if (node == null) {
                        ErrorView("Arbre vide", onRetry = vm::reset)
                    } else {
                        TreemapScreen(
                            navStack = navStack,
                            current = node,
                            onTileTap = vm::selectTile,
                            onTileOpen = vm::navigateInto,
                            onCrumb = vm::navigateTo,
                            onBack = { if (!vm.navigateUp()) vm.reset() },
                            onNewScan = vm::reset,
                        )
                    }
                }
            }
            }
          }
        }

        detail?.let { DetailBottomSheet(it, onDismiss = vm::dismissDetail) }

        // Hardware back: close the app list first, then drill up, then setup.
        androidx.activity.compose.BackHandler(enabled = navStack.isNotEmpty() && !showAppList) {
            if (!vm.navigateUp()) vm.reset()
        }
        androidx.activity.compose.BackHandler(enabled = showAppList) { vm.closeAppList() }
    }

    // --- System permission intents ---

    private fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else true // pre-R relies on the legacy READ_EXTERNAL_STORAGE grant

    private fun openAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData(Uri.parse("package:$packageName"))
            runCatching { startActivity(intent) }
                .onFailure { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        }
    }

    private fun openUsageAccess() {
        runCatching { startActivity(Intent(StorageStatsProvider.usageAccessIntentAction)) }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Échec du scan", style = MaterialTheme.typography.titleLarge)
        Text(message, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Retour") }
    }
}
