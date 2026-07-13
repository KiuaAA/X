package com.x.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.x.client.launch.GameLauncher
import com.x.client.launch.LaunchProfile
import com.x.client.perf.AssetPreloader
import com.x.client.perf.DiskCacheGuard
import com.x.client.perf.WarmJvmPrewarmer
import java.io.File

/**
 * X Client — background worker. Runs as a proper foreground service with a
 * persistent notification so Android doesn't kill it mid-download or mid-launch
 * when the app isn't in the foreground. This is required for background work
 * to actually be reliable on real devices, not just in a quick test session.
 */
class XClientService : Service() {

    private val binder = LocalBinder()
    private lateinit var xRoot: File
    private lateinit var gameLauncher: GameLauncher
    private lateinit var preloader: AssetPreloader
    private lateinit var cacheGuard: DiskCacheGuard

    private var currentStatusText = "Idle"

    companion object {
        const val CHANNEL_ID = "x_client_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_UPDATE_STATUS = "com.x.client.UPDATE_STATUS"
        const val EXTRA_STATUS_TEXT = "status_text"
    }

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

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(currentStatusText))

        Thread {
            updateStatus("Cleaning up temp files")
            cacheGuard.sweep()
            updateStatus("Refreshing version list")
            preloader.warmVersionManifestCache()
            updateStatus("Warming Java runtime")
            WarmJvmPrewarmer(xRoot).prewarm(javaMajor = 17)
            updateStatus("Ready")
        }.start()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_STATUS) {
            intent.getStringExtra(EXTRA_STATUS_TEXT)?.let { updateStatus(it) }
        }
        return START_STICKY
    }

    fun launchGame(profile: LaunchProfile, javaMajor: Int, rendererJvmArg: String, listener: GameLauncher.Listener) {
        updateStatus("Launching ${profile.versionId}")
        Thread {
            gameLauncher.launch(profile, javaMajor, rendererJvmArg, object : GameLauncher.Listener {
                override fun onLaunching(message: String) {
                    updateStatus(message)
                    listener.onLaunching(message)
                }
                override fun onGameOutput(line: String) {
                    listener.onGameOutput(line)
                }
                override fun onCrash(report: com.x.client.crash.CrashReport) {
                    updateStatus("Crashed: ${report.title}")
                    listener.onCrash(report)
                }
                override fun onExit(code: Int) {
                    updateStatus("Idle")
                    listener.onExit(code)
                }
            })
        }.start()
    }

    fun getXRoot(): File = xRoot
    fun getAssetPreloader(): AssetPreloader = preloader

    /** Called by any background operation (install, download queue, etc.) to keep
     *  the notification text meaningful instead of stuck on a stale message. */
    fun updateStatus(text: String) {
        currentStatusText = text
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("X Client")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "X Client background service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps downloads, installs, and game launches running smoothly in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
