package com.x.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.x.client.XClientService
import android.content.Intent

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Starts the X client background service that keeps
        // downloads, mod indexing, and JVM prep running smoothly.
        startService(Intent(this, XClientService::class.java))
    }
}
