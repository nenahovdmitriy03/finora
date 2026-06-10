package com.finora.presentation.util

/** Curated color palette (ARGB longs) for categories, accounts and goals. */
val finoraPalette: List<Long> = listOf(
    0xFF6C5CE7, // violet
    0xFF3FB18C, // green
    0xFF0984E3, // blue
    0xFFE17055, // coral
    0xFFE84393, // pink
    0xFFFDCB6E, // amber
    0xFF00CEC9, // teal
    0xFFA29BFE, // lavender
    0xFFE07685, // red
    0xFF74B9FF, // sky
    0xFF55EFC4, // mint
    0xFFFAB1A0  // peach
)

/**
 * Hue-spaced palette for charts/legends so neighbouring slices never blend.
 * Assign by index (chartColorAt) instead of the raw category color.
 */
val chartPalette: List<Long> = listOf(
    0xFF6C5CE7, // violet
    0xFFFDCB6E, // amber
    0xFF00CEC9, // teal
    0xFFE84393, // pink
    0xFF0984E3, // blue
    0xFFE17055, // coral
    0xFF55EFC4, // mint
    0xFFA29BFE, // lavender
    0xFFE07685, // red
    0xFF74B9FF, // sky
    0xFF3FB18C, // green
    0xFFFAB1A0  // peach
)

fun chartColorAt(index: Int): Long = chartPalette[index % chartPalette.size]
