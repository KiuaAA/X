package com.x.client.perf

import com.x.client.download.DeviceAbi
import com.x.client.modloader.JavaProcessRunner
import java.io.File

/**
 * Real fix for "Java takes a while to spin up" — most of that delay on Android
 * is the JVM's own class-loading/JIT warmup, not your code. We can't literally
 * keep a JVM process alive between launches (Android kills backgrounded
 * processes), but we CAN pre-verify the Java binary is present, executable,
 * and pre-touch its class data ahead of time, so the actual "Play" tap doesn't
 * pay that discovery cost on top of JVM startup.
 */
class WarmJvmPrewarmer(private val xRoot: File) {

    fun prewarm(javaMajor: Int): Boolean {
        val archTag = DeviceAbi.pojavArchTag()
        val javaBinary = JavaProcessRunner.findJavaBinary(xRoot, javaMajor, archTag) ?: return false

        return try {
            // Runs `java -version` once in the background right after app open —
            // forces the OS to page in the JVM's shared libraries and warms the
            // filesystem cache, so the real launch later starts from warm disk cache.
            val result = JavaProcessRunner.run(
                javaBinary,
                listOf("-version"),
                workingDir = xRoot
            )
            result.exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
}
