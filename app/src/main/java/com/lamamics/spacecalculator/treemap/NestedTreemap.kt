package com.lamamics.spacecalculator.treemap

import com.lamamics.spacecalculator.model.Node

/** A rectangle to draw, with its nesting depth. Folders are emitted before
 *  their children, so painting in list order yields the nested SpaceMonger look
 *  (a folder's frame + header band remain visible around its children). */
data class RenderTile(
    val node: Node?,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val depth: Int,
    val isDirectory: Boolean,
    /** Height of the folder's title band (0 = no room for a label). */
    val headerH: Float = 0f,
) {
    val right get() = x + w
    val bottom get() = y + h
    fun contains(px: Float, py: Float) = px >= x && px < right && py >= y && py < bottom
}

/**
 * Recursive squarified treemap. Each level is laid out with [Squarified], then
 * directories recurse into their inner area (minus a header band + 1px frame).
 * Recursion stops below [minPx] pixels or [maxDepth].
 */
object NestedTreemap {

    private const val PAD = 1.5f

    /**
     * Title band height in PIXELS. Capped at ~12% of the tile height (so it
     * barely distorts the children's true proportions) AND at 22dp absolute,
     * with a readability floor below which no band is drawn. Density-aware so it
     * looks the same on any screen.
     */
    private fun headerHeight(wPx: Float, hPx: Float, density: Float): Float {
        if (wPx / density < 38f) return 0f                  // too narrow for a name
        val band = (hPx * 0.12f).coerceAtMost(22f * density)
        return if (band >= 12f * density) band else 0f      // hide if unreadable
    }

    fun layout(
        root: Node,
        x: Float, y: Float, w: Float, h: Float,
        density: Float,
        maxDepth: Int = 12,
    ): List<RenderTile> {
        val out = ArrayList<RenderTile>()
        val minPx = 4f * density   // don't draw/recurse below ~4dp
        placeChildren(root, x, y, w, h, 0, density, minPx, maxDepth, out)
        return out
    }

    private fun placeChildren(
        node: Node,
        x: Float, y: Float, w: Float, h: Float,
        depth: Int, density: Float, minPx: Float, maxDepth: Int,
        out: MutableList<RenderTile>,
    ) {
        val tiles = Squarified.layout(node.children, x, y, w, h, node.hiddenSize)
        for (t in tiles) {
            val child = t.node
            val isDir = child?.isDirectory == true
            val header = if (isDir) headerHeight(t.w, t.h, density) else 0f
            out.add(RenderTile(child, t.x, t.y, t.w, t.h, depth, isDir, header))

            if (child != null && isDir && child.children.isNotEmpty() && depth < maxDepth && header > 0f) {
                val ix = t.x + PAD
                val iy = t.y + header
                val iw = t.w - 2 * PAD
                val ih = t.h - header - PAD
                if (iw >= minPx && ih >= minPx) {
                    placeChildren(child, ix, iy, iw, ih, depth + 1, density, minPx, maxDepth, out)
                }
            }
        }
    }
}
