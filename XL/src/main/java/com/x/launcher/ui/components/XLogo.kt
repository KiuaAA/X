package com.x.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.x.launcher.ui.theme.XCream

/**
 * Placeholder for the real "X" mark — two crossing blade shapes.
 * Swap this composable's body for a real vector asset
 * (res/drawable/x_logo.xml, exported from your design file)
 * once you have the exact artwork; this approximates it.
 */
@Composable
fun XLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(280.dp)) {
        drawBlade(this, mirrored = false)
        drawBlade(this, mirrored = true)
    }
}

private fun drawBlade(scope: DrawScope, mirrored: Boolean) {
    val w = scope.size.width
    val h = scope.size.height
    val sign = if (mirrored) -1f else 1f

    val path = Path().apply {
        moveTo(w * (0.5f - sign * 0.42f), h * 0.08f)
        lineTo(w * (0.5f - sign * 0.30f), h * 0.08f)
        lineTo(w * (0.5f + sign * 0.42f), h * 0.92f)
        lineTo(w * (0.5f + sign * 0.30f), h * 0.92f)
        close()
    }
    scope.drawPath(path, color = XCream)
}
