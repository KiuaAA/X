package com.x.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val XColorScheme = darkColorScheme(
    background = XBlack,
    surface = XCardBlack,
    primary = XCream,
    onPrimary = XTextOnCream,
    onBackground = XTextOnBlack,
    onSurface = XTextOnBlack
)

@Composable
fun XTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = XColorScheme,
        content = content
    )
}
