package com.lamamics.spacecalculator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lamamics.spacecalculator.model.Node
import com.lamamics.spacecalculator.ui.components.TreemapView
import com.lamamics.spacecalculator.util.formatBytes

@Composable
fun TreemapScreen(
    navStack: List<Node>,
    current: Node,
    onTileTap: (Node) -> Unit,
    onTileOpen: (Node) -> Unit,
    onCrumb: (Int) -> Unit,
    onBack: () -> Unit,
    onNewScan: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(canGoUp = navStack.size > 1, onBack = onBack, onNewScan = onNewScan)
        Breadcrumb(navStack, onCrumb)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(current.name, fontWeight = FontWeight.Bold)
            Text(formatBytes(current.size), color = MaterialTheme.colorScheme.outline)
        }
        Box(Modifier.fillMaxSize()) {
            if (current.children.isEmpty()) {
                Text(
                    "Aucun sous-élément suffisamment gros à afficher.",
                    Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                TreemapView(
                    node = current,
                    onTileTap = onTileTap,
                    onTileOpen = onTileOpen,
                )
            }
        }
    }
}

@Composable
private fun TopBar(canGoUp: Boolean, onBack: () -> Unit, onNewScan: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (canGoUp) "Remonter d'un niveau" else "Retour à la configuration",
            )
        }
        Text(
            if (canGoUp) "Remonter / retour" else "Retour à la config",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        TextButton(onClick = onNewScan) { Text("Nouveau scan") }
    }
}

@Composable
private fun Breadcrumb(stack: List<Node>, onCrumb: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stack.forEachIndexed { index, node ->
            if (index > 0) Text(" › ", color = MaterialTheme.colorScheme.outline)
            Text(
                text = node.name.ifBlank { "/" },
                modifier = Modifier.clickable { onCrumb(index) }.padding(horizontal = 2.dp),
                color = if (index == stack.lastIndex) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.primary,
                fontWeight = if (index == stack.lastIndex) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
