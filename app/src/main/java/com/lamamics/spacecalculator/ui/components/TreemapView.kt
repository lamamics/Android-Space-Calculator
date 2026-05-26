package com.lamamics.spacecalculator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.lamamics.spacecalculator.model.Node
import com.lamamics.spacecalculator.treemap.NestedTreemap
import com.lamamics.spacecalculator.treemap.RenderTile
import com.lamamics.spacecalculator.ui.theme.TreemapColors
import com.lamamics.spacecalculator.util.formatBytes

/**
 * Recursive (nested) treemap of [node]. Single tap → detail of the smallest
 * tile under the finger (onTileTap); double tap → drill into the deepest folder
 * under the finger (onTileOpen).
 */
@Composable
fun TreemapView(
    node: Node,
    modifier: Modifier = Modifier,
    onTileTap: (Node) -> Unit,
    onTileOpen: (Node) -> Unit,
) {
    val measurer: TextMeasurer = rememberTextMeasurer()
    val tiles = remember(node.path) { mutableListOf<RenderTile>() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(node.path) {
                detectTapGestures(
                    onTap = { pos -> deepestNode(tiles, pos)?.let(onTileTap) },
                    onDoubleTap = { pos -> deepestFolder(tiles, pos)?.let(onTileOpen) },
                )
            },
    ) {
        val laid = NestedTreemap.layout(node, 0f, 0f, size.width, size.height, density)
        tiles.clear(); tiles.addAll(laid)
        // Parents first, children on top → nested frames remain visible.
        laid.forEach { drawTile(it, measurer, density) }
    }
}

/** Smallest (deepest) tile containing the point. */
private fun deepestNode(tiles: List<RenderTile>, pos: Offset): Node? =
    tiles.lastOrNull { it.node != null && it.contains(pos.x, pos.y) }?.node

/** Deepest directory tile containing the point (for drill-down). */
private fun deepestFolder(tiles: List<RenderTile>, pos: Offset): Node? =
    tiles.lastOrNull { it.isDirectory && it.node != null && it.contains(pos.x, pos.y) }?.node

private fun DrawScope.drawTile(tile: RenderTile, measurer: TextMeasurer, density: Float) {
    val topLeft = Offset(tile.x, tile.y)
    val tileSize = Size(tile.w, tile.h)
    drawRect(color = TreemapColors.colorFor(tile.node), topLeft = topLeft, size = tileSize)

    // Free space gets a diagonal hatch so it can't be confused with the grey
    // "small files" residual tiles.
    if (tile.node?.isFree == true) drawHatch(tile, density)

    // Border thins with depth so nesting reads clearly.
    val stroke = (1.6f - tile.depth * 0.15f).coerceAtLeast(0.4f)
    drawRect(Color(0x55000000), topLeft = topLeft, size = tileSize, style = Stroke(width = stroke))

    val wDp = tile.w / density
    val hDp = tile.h / density
    if (tile.isDirectory && tile.headerH > 0f) {
        drawFolderHeader(tile, measurer, density)
    } else if (!tile.isDirectory && wDp >= 40f && hDp >= 16f) {
        drawLeafLabel(tile, measurer, density)
    }
}

/** Title band for a folder: subtle shade, separator line, name + size.
 *  Font size scales with the band height (in dp) so it stays readable. */
private fun DrawScope.drawFolderHeader(tile: RenderTile, measurer: TextMeasurer, density: Float) {
    val band = tile.headerH
    drawRect(Color(0x1F000000), topLeft = Offset(tile.x, tile.y), size = Size(tile.w, band))
    drawLine(
        Color(0x66000000),
        Offset(tile.x, tile.y + band),
        Offset(tile.right, tile.y + band),
        strokeWidth = 0.8f,
    )

    val pad = 4f * density
    val maxW = tile.w - 2 * pad
    if (maxW < 8f * density) return
    val bandDp = band / density
    val fontSp = (bandDp * 0.5f).coerceIn(10f, 14f)
    val nameStyle = TextStyle(color = Color(0xFF1A1A1A), fontSize = fontSp.sp, fontWeight = FontWeight.SemiBold)

    val name = tile.node?.name ?: "petits fichiers"
    val nameLayout = measurer.measure(
        name, nameStyle, maxLines = 1, overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = maxW.toInt().coerceAtLeast(0)),
    )
    val ty = (tile.y + (band - nameLayout.size.height) / 2f).coerceAtLeast(tile.y)
    // Folder bands show the NAME only: their size already includes every child,
    // so printing it at each level invites double-counting when reading the map.
    // The size stays available in the detail sheet (tap) and the breadcrumb header.
    drawText(nameLayout, topLeft = Offset(tile.x + pad, ty))
}

/** Diagonal hatch fill for the free-space tile. */
private fun DrawScope.drawHatch(tile: RenderTile, density: Float) {
    val step = 14f * density
    val line = Color(0x33455A64)
    clipRect(tile.x, tile.y, tile.right, tile.bottom) {
        var x = tile.x - tile.h
        while (x < tile.right) {
            drawLine(line, Offset(x, tile.bottom), Offset(x + tile.h, tile.y), strokeWidth = 1.2f)
            x += step
        }
    }
}

/** Name (+ size if tall enough) for a leaf file tile. */
private fun DrawScope.drawLeafLabel(tile: RenderTile, measurer: TextMeasurer, density: Float) {
    val pad = 3f * density
    val maxW = tile.w - 2 * pad
    val style = TextStyle(color = Color(0xDE000000), fontSize = 11.sp)
    val nameLayout = measurer.measure(
        tile.node?.name ?: "", style, maxLines = 1, overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = maxW.toInt().coerceAtLeast(0)),
    )
    drawText(nameLayout, topLeft = Offset(tile.x + pad, tile.y + 2f))
    val node = tile.node
    if (node != null && tile.h / density > 30f) {
        drawText(
            measurer.measure(
                formatBytes(node.size), style.copy(fontSize = 10.sp), maxLines = 1,
                overflow = TextOverflow.Clip,
                constraints = Constraints(maxWidth = maxW.toInt().coerceAtLeast(0)),
            ),
            topLeft = Offset(tile.x + pad, tile.y + 2f + nameLayout.size.height),
        )
    }
}
