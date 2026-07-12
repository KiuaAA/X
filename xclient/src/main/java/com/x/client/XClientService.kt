package com.x.client

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import com.x.client.launch.GameLauncher
import com.x.client.launch.LaunchProfile
import java.io.File

/**
 * X Client — background worker.
 * Runs as a bound service so MainActivity can call launch()/cancel()
 * directly and get callbacks without polling.
 */
class XClientService : Service() {

    private val binder = LocalBinder()
    private lateinit var xRoot: File
    private lateinit var gameLauncher: GameLauncher

    inner class LocalBinder : Binder() {
        fun getService(): XClientService = this@XClientService
    }

    override fun onCreate() {
        super.onCreate()
        xRoot = File(getExternalFilesDir(null) ?: Environment.getExternalStorageDirectory(), "X")
        xRoot.mkdirs()
        gameLauncher = GameLauncher(xRoot)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    fun launchGame(profile: LaunchProfile, javaMajor: Int, rendererJvmArg: String, listener: GameLauncher.Listener) {
        Thread {
            gameLauncher.launch(profile, javaMajor, rendererJvmArg, listener)
        }.start()
    }

    fun getXRoot(): File = xRoot
}
