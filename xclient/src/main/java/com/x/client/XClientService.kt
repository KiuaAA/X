package com.x.client

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * X Client — background worker.
 * Later parts will add: download queue, mod indexing,
 * cache warm-up, crash-log watcher.
 */
class XClientService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Part 3 will wire in the actual background work here
        return START_STICKY
    }
}
