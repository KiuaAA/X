package com.x.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.x.client.XClientService
import com.x.launcher.ui.screens.HomeScreen
import com.x.launcher.ui.theme.XTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, XClientService::class.java))

        setContent {
            XTheme {
                HomeScreen(
                    onPlay = { /* wired to XClientService.launchGame in the next part */ }
                )
            }
        }
    }
}
