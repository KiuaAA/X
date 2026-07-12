package com.x.client.crash

import android.os.FileObserver
import java.io.File

/**
 * Watches a version's crash-reports folder and logs folder for new files,
 * parses them immediately, and reports back through the listener so the
 * launcher UI can show "reason + solution" the moment a crash happens.
 */
class CrashWatcher(private val xRoot: File) {

    interface Listener {
        fun onCrashDetected(report: CrashReport)
    }

    private var observer: FileObserver? = null

    fun startWatching(versionId: String, listener: Listener) {
        val crashDir = File(xRoot, "versions/$versionId/crash-reports")
        crashDir.mkdirs()

        observer = object : FileObserver(crashDir.absolutePath, CREATE or CLOSE_WRITE) {
            override fun onEvent(event: Int, path: String?) {
                if (path == null || !path.endsWith(".txt")) return
                val file = File(crashDir, path)
                // Small delay-free re-read guard: file might still be mid-write on CREATE,
                // CLOSE_WRITE is the reliable "fully written" signal we act on.
                if (event and CLOSE_WRITE != 0 && file.exists()) {
                    val report = CrashLogParser.parseFile(file)
                    listener.onCrashDetected(report)
                }
            }
        }
        observer?.startWatching()
    }

    fun stopWatching() {
        observer?.stopWatching()
        observer = null
    }

    /** For a launch that fails before a crash-report file is even written
     *  (native crash, immediate JVM exit) — parse whatever was captured
     *  from the process's stdout/stderr instead. */
    fun parseProcessOutput(output: String): CrashReport {
        return CrashLogParser.parse(output, sourceFile = null)
    }
}
