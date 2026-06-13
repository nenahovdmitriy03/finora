package com.finora.presentation.util

/** Curated color palette (ARGB longs) for categories, accounts and goals. */
val finoraPalette: List<Long> = listOf(
    0xFF6366F1, // indigo
    0xFF10B981, // emerald
    0xFF3B82F6, // blue
    0xFFF59E0B, // amber
    0xFFEC4899, // pink
    0xFF14B8A6, // teal
    0xFF8B5CF6, // violet
    0xFFF97316, // orange
    0xFFEF4444, // red
    0xFF06B6D4, // cyan
    0xFF84CC16, // lime
    0xFFD946EF  // fuchsia
)

/**
 * Hue-spaced palette for charts/legends so neighbouring slices never blend.
 * Maximise perceptual distance between adjacent entries.
 */
val chartPalette: List<Long> = listOf(
    0xFF6366F1, // indigo
    0xFF10B981, // emerald
    0xFFF59E0B, // amber
    0xFFEC4899, // pink
    0xFF3B82F6, // blue
    0xFFF97316, // orange
    0xFF8B5CF6, // violet
    0xFF14B8A6, // teal
    0xFFEF4444, // red
    0xFF06B6D4, // cyan
    0xFF84CC16, // lime
    0xFFD946EF  // fuchsia
)

fun chartColorAt(index: Int): Long = chartPalette[index % chartPalette.size]
