package com.lamamics.spacecalculator.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.lamamics.spacecalculator.ui.TileDetail
import com.lamamics.spacecalculator.util.MediaKind
import com.lamamics.spacecalculator.util.formatBytes
import com.lamamics.spacecalculator.util.loadThumbnail
import com.lamamics.spacecalculator.util.mediaKind
import com.lamamics.spacecalculator.util.openFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailBottomSheet(detail: TileDetail, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val node = detail.node
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                text = node.name.ifBlank { node.path },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when {
                    node.isFree -> "Espace libre"
                    node.isDirectory -> "Dossier"
                    else -> "Fichier"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            // Thumbnail preview for readable image/video files.
            val kind = remember(node.path) { mediaKind(node.name) }
            if (!node.isDirectory && node.isReadable && kind != MediaKind.OTHER) {
                val thumb by produceState<ImageBitmap?>(initialValue = null, node.path) {
                    value = withContext(Dispatchers.IO) { loadThumbnail(node.path, kind, 600) }
                }
                thumb?.let { img ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = img,
                            contentDescription = "Aperçu de ${node.name}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (!node.isDirectory && !node.isFree && node.isReadable) {
                Button(
                    onClick = { openFile(context, node.path) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) { Text("Ouvrir le fichier") }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            DetailRow("Taille", formatBytes(node.size))
            if (node.isDirectory) {
                DetailRow("Éléments", node.childCount.toString())
                if (node.hiddenSize > 0) {
                    DetailRow("Dont petits fichiers", formatBytes(node.hiddenSize))
                }
            }
            if (!node.isReadable) {
                DetailRow("Lisibilité", "Non lisible (zone protégée)")
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            val owner = detail.owner
            if (node.ownerPackage != null) {
                DetailRow("Application", owner?.label ?: node.ownerPackage)
                DetailRow("Paquet", node.ownerPackage, mono = true)
                if (owner != null) {
                    DetailRow("App (APK)", formatBytes(owner.appBytes))
                    DetailRow("Données", formatBytes(owner.dataBytes))
                    DetailRow("Cache", formatBytes(owner.cacheBytes))
                }
            } else {
                DetailRow("Application", "Non attribué")
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            DetailRow("Chemin", node.path, mono = true)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, mono: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        )
    }
}
