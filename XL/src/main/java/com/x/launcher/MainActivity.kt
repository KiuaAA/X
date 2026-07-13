package com.x.launcher

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.x.client.XClientService
import com.x.client.launch.LaunchProfile
import com.x.launcher.state.AccountViewModel
import com.x.launcher.state.LaunchViewModel
import com.x.launcher.state.VersionListItem
import com.x.launcher.ui.screens.AccountScreen
import com.x.launcher.ui.screens.HomeScreen
import com.x.launcher.ui.screens.HomeUiState
import com.x.launcher.ui.screens.LaunchScreen
import com.x.launcher.ui.screens.ModManagerScreen
import com.x.launcher.ui.screens.VersionPickerScreen
import com.x.launcher.ui.theme.XTheme
import java.io.File

private enum class Screen { HOME, VERSION_PICKER, MODS, LAUNCH, ACCOUNT }

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

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored — service still runs without it, just silently */ }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        val serviceIntent = Intent(this, XClientService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            XTheme {
                var screen by remember { mutableStateOf(Screen.HOME) }
                var selectedVersion by remember { mutableStateOf<VersionListItem?>(null) }
                var serviceReady by remember { mutableStateOf(false) }

                LaunchedEffect(bound) {
                    if (bound) serviceReady = true
                }

                if (!serviceReady || clientService == null) {
                    return@XTheme
                }

                val xRoot = remember { clientService!!.getXRoot() }
                val accountViewModel: AccountViewModel = viewModel { AccountViewModel(xRoot) }
                val launchViewModel: LaunchViewModel = viewModel()

                LaunchedEffect(Unit) { accountViewModel.loadOfflineProfiles() }

                when (screen) {
                    Screen.HOME -> {
                        val active = accountViewModel.activeProfile
                        HomeScreen(
                            state = HomeUiState(
                                playerName = active?.playerName ?: "Guest",
                                playerUuid = active?.uuid ?: "00000000-0000-0000-0000-000000000000",
                                githubLinked = accountViewModel.githubUsername != null,
                                selectedVersionLabel = selectedVersion?.id ?: "Select a version",
                                selectedVersionNumber = selectedVersion?.type ?: "--",
                                hasAccount = active != null,
                                hasVersion = selectedVersion != null
                            ),
                            onOpenVersionPicker = { screen = Screen.VERSION_PICKER },
                            onOpenFiles = { screen = Screen.MODS },
                            onEditProfile = { screen = Screen.ACCOUNT },
                            onPlay = {
                                val version = selectedVersion
                                val service = clientService
                                val account = active
                                if (version != null && service != null && account != null) {
                                    val profile = LaunchProfile(
                                        playerName = account.playerName,
                                        uuid = account.uuid,
                                        accessToken = account.accessToken,
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
                    }

                    Screen.VERSION_PICKER -> VersionPickerScreen(
                        onBack = { screen = Screen.HOME },
                        onVersionSelected = {
                            selectedVersion = it
                            screen = Screen.HOME
                        }
                    )

                    Screen.MODS -> {
                        val version = selectedVersion
                        if (version != null) {
                            val modsDir = File(xRoot, "versions/${version.id}/mods")
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

                    Screen.ACCOUNT -> AccountScreen(
                        xRoot = xRoot,
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
