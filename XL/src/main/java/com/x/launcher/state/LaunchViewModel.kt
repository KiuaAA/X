package com.x.launcher.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.x.client.XClientService
import com.x.client.crash.CrashReport
import com.x.client.launch.GameLauncher
import com.x.client.launch.LaunchProfile

enum class LaunchPhase { IDLE, LAUNCHING, RUNNING, CRASHED, EXITED }

class LaunchViewModel : ViewModel() {

    var phase by mutableStateOf(LaunchPhase.IDLE)
        private set
    var statusMessage by mutableStateOf("")
        private set
    var crashReport by mutableStateOf<CrashReport?>(null)
        private set
    var exitCode by mutableStateOf<Int?>(null)
        private set

    private val logBuffer = StringBuilder()
    var visibleLog by mutableStateOf("")
        private set

    fun start(
        clientService: XClientService,
        profile: LaunchProfile,
        javaMajor: Int,
        rendererJvmArg: String
    ) {
        phase = LaunchPhase.LAUNCHING
        crashReport = null
        exitCode = null
        logBuffer.clear()
        visibleLog = ""

        clientService.launchGame(profile, javaMajor, rendererJvmArg, object : GameLauncher.Listener {
            override fun onLaunching(message: String) {
                statusMessage = message
            }

            override fun onGameOutput(line: String) {
                if (phase == LaunchPhase.LAUNCHING) phase = LaunchPhase.RUNNING
                logBuffer.appendLine(line)
                if (logBuffer.length > 8000) logBuffer.delete(0, logBuffer.length - 8000)
                visibleLog = logBuffer.toString()
            }

            override fun onCrash(report: CrashReport) {
                phase = LaunchPhase.CRASHED
                crashReport = report
            }

            override fun onExit(code: Int) {
                exitCode = code
                if (phase != LaunchPhase.CRASHED) phase = LaunchPhase.EXITED
            }
        })
    }

    fun dismissCrash() {
        phase = LaunchPhase.IDLE
        crashReport = null
    }
}
