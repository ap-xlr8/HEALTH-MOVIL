package com.healthos.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

enum class WindowHeightSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

@Immutable
data class WindowSizeInfo(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
    val maxWidth: Dp,
    val maxHeight: Dp,
    val isLandscape: Boolean,
) {
    val isCompact: Boolean get() = widthSizeClass == WindowWidthSizeClass.COMPACT
    val isMedium: Boolean get() = widthSizeClass == WindowWidthSizeClass.MEDIUM
    val isExpanded: Boolean get() = widthSizeClass == WindowWidthSizeClass.EXPANDED
    val useNavRail: Boolean get() = !isCompact || (isLandscape && maxHeight < 600.dp)
}

@Composable
fun ProvideWindowSizeInfo(content: @Composable (WindowSizeInfo) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sizeInfo = calculateWindowSizeInfo(maxWidth, maxHeight)
        content(sizeInfo)
    }
}

fun calculateWindowSizeInfo(
    width: Dp,
    height: Dp,
): WindowSizeInfo {
    val widthClass =
        when {
            width < 600.dp -> WindowWidthSizeClass.COMPACT
            width < 840.dp -> WindowWidthSizeClass.MEDIUM
            else -> WindowWidthSizeClass.EXPANDED
        }

    val heightClass =
        when {
            height < 480.dp -> WindowHeightSizeClass.COMPACT
            height < 900.dp -> WindowHeightSizeClass.MEDIUM
            else -> WindowHeightSizeClass.EXPANDED
        }

    return WindowSizeInfo(
        widthSizeClass = widthClass,
        heightSizeClass = heightClass,
        maxWidth = width,
        maxHeight = height,
        isLandscape = width > height,
    )
}

@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 840.dp,
    alignment: Alignment = Alignment.Center,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        BoxWithConstraints(
            modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(),
            content = content,
        )
    }
}
