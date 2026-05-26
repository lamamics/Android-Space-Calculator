package com.lamamics.spacecalculator.model

import kotlinx.serialization.Serializable

/**
 * A node in the storage tree. Produced by the scan engine and consumed by the
 * treemap UI. The tree is pruned: children smaller than the scan threshold are
 * dropped from [children] but their bytes remain counted in the parent [size].
 * The gap between [size] and the sum of the kept children is exposed as
 * [hiddenSize] so the UI can draw a residual "small files" tile.
 */
@Serializable
data class Node(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    /** false when the path could not be read (permission / blocked zone). */
    val isReadable: Boolean = true,
    /** Owning package when the path lives under Android/data|obb/<package>/. */
    val ownerPackage: String? = null,
    /** Synthetic tile representing the volume's free space (not a real file). */
    val isFree: Boolean = false,
    /** Number of immediate children before pruning (for the detail view). */
    val childCount: Int = 0,
    val children: List<Node> = emptyList(),
) {
    /** Bytes held by children too small to be kept in the tree. */
    val hiddenSize: Long
        get() = (size - children.sumOf { it.size }).coerceAtLeast(0L)

    val isLeaf: Boolean get() = !isDirectory || children.isEmpty()
}
