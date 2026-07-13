package com.x.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.x.client.XClientService
import com.x.client.launch.LaunchProfile
import com.x.launcher.state.LaunchViewModel
import com.x.launcher.state.VersionListItem
import com.x.launcher.ui.screens.HomeScreen
import com.x.launcher.ui.screens.HomeUiState
import com.x.launcher.ui.screens.LaunchScreen
import com.x.launcher.ui.screens.ModManagerScreen
import com.x.launcher.ui.screens.VersionPickerScreen
import com.x.launcher.ui.theme.XTheme
import java.io.File

private enum class Screen { HOME, VERSION_PICKER, MODS, LAUNCH }

class MainActivity : ComponentActivity() {

    private var clientService: XClientService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as XClientService.LocalBinder
            clientService = binder.getService()
            bound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            clientService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, XClientService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            XTheme {
                var screen by remember { mutableStateOf(Screen.HOME) }
                var selectedVersion by remember { mutableStateOf<VersionListItem?>(null) }
                val launchViewModel: LaunchViewModel = viewModel()

                when (screen) {
                    Screen.HOME -> HomeScreen(
                        state = HomeUiState(
                            selectedVersionLabel = selectedVersion?.id ?: "Select a version",
                            selectedVersionNumber = selectedVersion?.type ?: "--"
                        ),
                        onOpenVersionPicker = { screen = Screen.VERSION_PICKER },
                        onOpenFiles = { screen = Screen.MODS },
                        onPlay = {
                            val version = selectedVersion
                            val service = clientService
                            if (version != null && service != null) {
                                val profile = LaunchProfile(
                                    playerName = "kiua",
                                    uuid = "00000000-0000-0000-0000-000000000000",
                                    accessToken = "0",
                                    versionId = version.id
                                )
                                screen = Screen.LAUNCH
                                launchViewModel.start(
                                    clientService = service,
                                    profile = profile,
                                    javaMajor = 17,
                                    rendererJvmArg = "-Dorg.lwjgl.opengl.libname=libgl4es.so"
                                )
                            }
                        }
                    )

                    Screen.VERSION_PICKER -> VersionPickerScreen(
                        onBack = { screen = Screen.HOME },
                        onVersionSelected = {
                            selectedVersion = it
                            screen = Screen.HOME
                        }
                    )

                    Screen.MODS -> {
                        val version = selectedVersion
                        if (version != null && clientService != null) {
                            val modsDir = File(clientService!!.getXRoot(), "versions/${version.id}/mods")
                            ModManagerScreen(
                                versionModsDir = modsDir,
                                onBack = { screen = Screen.HOME },
                                onEditMod = { /* wired in the file-editor part */ }
                            )
                        } else {
                            screen = Screen.HOME
                        }
                    }

                    Screen.LAUNCH -> LaunchScreen(
                        viewModel = launchViewModel,
                        onBack = { screen = Screen.HOME }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (bound) {
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }
}
