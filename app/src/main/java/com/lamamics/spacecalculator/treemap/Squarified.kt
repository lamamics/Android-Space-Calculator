package com.lamamics.spacecalculator.treemap

import com.lamamics.spacecalculator.model.Node

/** A laid-out rectangle for one node, in pixel coordinates. */
data class Tile(
    val node: Node?,   // null for the synthetic "small files" residual tile
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
) {
    val right get() = x + w
    val bottom get() = y + h
    fun contains(px: Float, py: Float) = px >= x && px < right && py >= y && py < bottom
}

/**
 * Squarified treemap layout (Bruls, Huizing & van Wijk, 2000). Produces tiles
 * whose aspect ratios stay close to 1, matching the SpaceMonger look. Lays out
 * a single level — nesting is handled by drill-down in the UI.
 */
object Squarified {

    /**
     * @param children nodes to place (only size>0 are used).
     * @param hiddenSize bytes of pruned small files; if > 0 a residual tile is
     *        appended so the visualization stays area-accurate.
     */
    fun layout(
        children: List<Node>,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        hiddenSize: Long = 0L,
    ): List<Tile> {
        if (w <= 0f || h <= 0f) return emptyList()

        // Parallel lists: a node (null = residual) and its raw value.
        val nodes = ArrayList<Node?>()
        val values = ArrayList<Double>()
        children.asSequence()
            .filter { it.size > 0 }
            .sortedByDescending { it.size }
            .forEach { nodes.add(it); values.add(it.size.toDouble()) }
        if (hiddenSize > 0) { nodes.add(null); values.add(hiddenSize.toDouble()) }
        if (values.isEmpty()) return emptyList()

        val total = values.sum()
        val scale = (w.toDouble() * h.toDouble()) / total
        val areas = values.map { it * scale }

        val tiles = ArrayList<Tile>(nodes.size)
        var rx = x; var ry = y; var rw = w; var rh = h

        val rowAreas = ArrayList<Double>()
        val rowNodes = ArrayList<Node?>()
        var i = 0
        while (i < areas.size) {
            val side = minOf(rw, rh).toDouble()
            val a = areas[i]
            if (rowAreas.isEmpty() || worst(rowAreas, side) >= worst(rowAreas + a, side)) {
                rowAreas.add(a); rowNodes.add(nodes[i]); i++
            } else {
                val rem = placeRow(rowAreas, rowNodes, rx, ry, rw, rh, tiles)
                rx = rem[0]; ry = rem[1]; rw = rem[2]; rh = rem[3]
                rowAreas.clear(); rowNodes.clear()
            }
        }
        if (rowAreas.isNotEmpty()) placeRow(rowAreas, rowNodes, rx, ry, rw, rh, tiles)
        return tiles
    }

    /** Lays the row along the shorter side; returns the remaining rect [x,y,w,h]. */
    private fun placeRow(
        rowAreas: List<Double>,
        rowNodes: List<Node?>,
        x: Float, y: Float, w: Float, h: Float,
        tiles: MutableList<Tile>,
    ): FloatArray {
        val rowSum = rowAreas.sum()
        return if (w >= h) {
            val colW = (rowSum / h).toFloat()
            var yy = y
            for (k in rowAreas.indices) {
                val cellH = (rowAreas[k] / rowSum * h).toFloat()
                tiles.add(Tile(rowNodes[k], x, yy, colW, cellH))
                yy += cellH
            }
            floatArrayOf(x + colW, y, w - colW, h)
        } else {
            val rowH = (rowSum / w).toFloat()
            var xx = x
            for (k in rowAreas.indices) {
                val cellW = (rowAreas[k] / rowSum * w).toFloat()
                tiles.add(Tile(rowNodes[k], xx, y, cellW, rowH))
                xx += cellW
            }
            floatArrayOf(x, y + rowH, w, h - rowH)
        }
    }

    /** Highest aspect ratio in a row given the fixed side length. */
    private fun worst(row: List<Double>, side: Double): Double {
        if (row.isEmpty() || side <= 0.0) return Double.MAX_VALUE
        val sum = row.sum()
        val max = row.max()
        val min = row.min()
        val s2 = sum * sum
        val side2 = side * side
        return maxOf(side2 * max / s2, s2 / (side2 * min))
    }
}
