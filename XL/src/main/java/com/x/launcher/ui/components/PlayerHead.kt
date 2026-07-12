package com.x.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Renders the actual head from the player's current skin — updates
 * automatically whenever the account's skin changes, since we're
 * just pointing at a render service keyed by UUID (not caching a
 * static image). Crafatar is a free, open, no-auth skin-render API
 * that reads directly from Mojang's session server.
 */
@Composable
fun PlayerHead(uuid: String, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    val url = "https://crafatar.com/avatars/$uuid?overlay=true&size=160"
    AsyncImage(
        model = url,
        contentDescription = "Player head",
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(androidx.compose.ui.graphics.Color(0xFF2A2A2A))
    )
}
