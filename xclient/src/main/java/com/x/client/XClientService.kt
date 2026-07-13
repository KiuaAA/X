package com.x.client

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import com.x.client.launch.GameLauncher
import com.x.client.launch.LaunchProfile
import com.x.client.perf.AssetPreloader
import com.x.client.perf.DiskCacheGuard
import com.x.client.perf.WarmJvmPrewarmer
import java.io.File

/**
 * X Client — background worker. On top of launch orchestration, this now
 * does the "smooth/fast/strong" work in the background the moment the app
 * opens: warms the version-list cache, sweeps stale temp files, and
 * pre-touches the Java runtime so Play feels snappier.
 */
class XClientService : Service() {

    private val binder = LocalBinder()
    private lateinit var xRoot: File
    private lateinit var gameLauncher: GameLauncher
    private lateinit var preloader: AssetPreloader
    private lateinit var cacheGuard: DiskCacheGuard

    inner class LocalBinder : Binder() {
        fun getService(): XClientService = this@XClientService
    }

    override fun onCreate() {
        super.onCreate()
        xRoot = File(getExternalFilesDir(null) ?: Environment.getExternalStorageDirectory(), "X")
        xRoot.mkdirs()
        gameLauncher = GameLauncher(xRoot)
        preloader = AssetPreloader(xRoot)
        cacheGuard = DiskCacheGuard(xRoot)

        // All background smoothness work runs off the main thread, fire-and-forget,
        // so app startup itself is never blocked by any of this.
        Thread {
            cacheGuard.sweep()
            preloader.warmVersionManifestCache()
            WarmJvmPrewarmer(xRoot).prewarm(javaMajor = 17)
        }.start()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    fun launchGame(profile: LaunchProfile, javaMajor: Int, rendererJvmArg: String, listener: GameLauncher.Listener) {
        Thread {
            gameLauncher.launch(profile, javaMajor, rendererJvmArg, listener)
        }.start()
    }

    fun getXRoot(): File = xRoot
    fun getAssetPreloader(): AssetPreloader = preloader
}
