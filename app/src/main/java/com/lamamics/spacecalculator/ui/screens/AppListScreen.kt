package com.lamamics.spacecalculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lamamics.spacecalculator.model.AppInfo
import com.lamamics.spacecalculator.util.formatBytes

/**
 * "By application" view: each installed app's storage (app + data + cache),
 * sorted by total descending. Backed by StorageStatsManager (Usage access).
 */
@Composable
fun AppListScreen(
    apps: List<AppInfo>,
    loading: Boolean,
    hasUsageAccess: Boolean,
    onBack: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
            Text("Par application", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onRefresh) { Text("Rafraîchir") }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())

        when {
            !hasUsageAccess && apps.isEmpty() -> EmptyState(
                "Accès aux données d'utilisation requis pour lister les apps.",
                actionLabel = "Ouvrir les réglages",
                onAction = onOpenUsageAccess,
            )
            apps.isEmpty() && !loading -> EmptyState("Aucune donnée.", null, {})
            else -> {
                val total = apps.sumOf { it.totalBytes }.coerceAtLeast(1)
                LazyColumn(Modifier.fillMaxSize()) {
                    items(apps) { app -> AppRow(app, total) }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppInfo, totalAll: Long) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(app.label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
            Text(formatBytes(app.totalBytes), fontWeight = FontWeight.SemiBold)
        }
        Text(
            app.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            fontFamily = FontFamily.Monospace,
        )
        // Proportional bar of this app vs the largest total.
        val frac = (app.totalBytes.toFloat() / totalAll).coerceIn(0f, 1f)
        Box(
            Modifier.fillMaxWidth().height(6.dp).padding(top = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier.fillMaxWidth(frac).height(6.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(
            "App ${formatBytes(app.appBytes)} · Données ${formatBytes(app.dataBytes)} · Cache ${formatBytes(app.cacheBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp),
        )
        HorizontalDivider(Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun EmptyState(message: String, actionLabel: String?, onAction: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.outline)
            if (actionLabel != null) {
                TextButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(actionLabel) }
            }
        }
    }
}
