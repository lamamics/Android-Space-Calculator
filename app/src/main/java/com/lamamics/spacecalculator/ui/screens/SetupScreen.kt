package com.lamamics.spacecalculator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lamamics.spacecalculator.model.StorageVolumeInfo
import com.lamamics.spacecalculator.shizuku.ShizukuStatus
import com.lamamics.spacecalculator.util.formatBytes

/** Pure-UI state bundle for [SetupScreen]. */
data class SetupCallbacks(
    val onRequestShizuku: () -> Unit,
    val onOpenAllFilesAccess: () -> Unit,
    val onOpenUsageAccess: () -> Unit,
    val onToggleShizuku: (Boolean) -> Unit,
    val onSelectVolume: (StorageVolumeInfo) -> Unit,
    val onStartScan: () -> Unit,
    val onRefresh: () -> Unit,
    val onOpenAppList: () -> Unit,
)

@Composable
fun SetupScreen(
    shizukuStatus: ShizukuStatus,
    useShizuku: Boolean,
    hasAllFilesAccess: Boolean,
    hasUsageAccess: Boolean,
    volumes: List<StorageVolumeInfo>,
    selectedVolume: StorageVolumeInfo?,
    cb: SetupCallbacks,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text(
            "Space Calculator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Analyse de l'espace de stockage",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(20.dp))

        // --- Accès étendu (Shizuku) ---
        StatusCard(
            title = "Accès étendu (Shizuku)",
            subtitle = shizukuSubtitle(shizukuStatus),
            ok = shizukuStatus == ShizukuStatus.READY,
            actionLabel = shizukuAction(shizukuStatus),
            onAction = cb.onRequestShizuku,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Utiliser Shizuku pour le scan", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = useShizuku, onCheckedChange = cb.onToggleShizuku)
        }
        Text(
            "Sans Shizuku, les dossiers Android/data et Android/obb (gros du « Autre ») " +
                "ne seront pas lisibles.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(12.dp))

        // --- Permissions système ---
        StatusCard(
            title = "Accès à tous les fichiers",
            subtitle = if (hasAllFilesAccess) "Accordé" else "Requis pour lire le stockage partagé",
            ok = hasAllFilesAccess,
            actionLabel = if (hasAllFilesAccess) null else "Ouvrir les réglages",
            onAction = cb.onOpenAllFilesAccess,
        )
        StatusCard(
            title = "Accès aux données d'utilisation",
            subtitle = if (hasUsageAccess) "Accordé" else "Requis pour l'attribution par application",
            ok = hasUsageAccess,
            actionLabel = if (hasUsageAccess) null else "Ouvrir les réglages",
            onAction = cb.onOpenUsageAccess,
        )
        Spacer(Modifier.height(16.dp))

        // --- Choix du volume ---
        Text("Emplacement à analyser", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (volumes.isEmpty()) {
            Text("Aucun volume détecté.", color = MaterialTheme.colorScheme.outline)
        }
        volumes.forEach { vol ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = vol == selectedVolume,
                        onClick = { cb.onSelectVolume(vol) },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = vol == selectedVolume, onClick = { cb.onSelectVolume(vol) })
                Column(Modifier.padding(start = 8.dp)) {
                    Text(
                        vol.label + if (vol.isRemovable) " (amovible)" else "",
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${formatBytes(vol.totalBytes - vol.freeBytes)} / ${formatBytes(vol.totalBytes)} • ${vol.rootPath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = cb.onStartScan,
            enabled = selectedVolume != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Lancer le scan") }
        OutlinedButton(
            onClick = cb.onRefresh,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Rafraîchir l'état") }
        OutlinedButton(
            onClick = cb.onOpenAppList,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Voir l'espace par application") }
    }
}

@Composable
private fun StatusCard(
    title: String,
    subtitle: String,
    ok: Boolean,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ok) Color(0x2200C853) else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(
                    (if (ok) "✓ " else "") + subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (actionLabel != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

private fun shizukuSubtitle(status: ShizukuStatus) = when (status) {
    ShizukuStatus.NOT_INSTALLED -> "Shizuku n'est pas installé"
    ShizukuStatus.NOT_RUNNING -> "Installé mais non démarré (lancer via ADB)"
    ShizukuStatus.PERMISSION_REQUIRED -> "Autorisation requise"
    ShizukuStatus.READY -> "Prêt"
}

private fun shizukuAction(status: ShizukuStatus): String? = when (status) {
    ShizukuStatus.PERMISSION_REQUIRED -> "Autoriser"
    ShizukuStatus.READY -> null
    else -> null
}
