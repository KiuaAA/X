package com.x.client.perf

import java.io.File

/**
 * Prevents the exact thing that makes launchers feel "weak" over time: an
 * ever-growing /tmp and stale partial downloads silently eating storage and
 * slowing every disk scan. Runs a lightweight sweep in the background.
 */
class DiskCacheGuard(private val xRoot: File) {

    fun sweep(maxTmpAgeHours: Long = 24): CleanupResult {
        val tmpDir = File(xRoot, "tmp")
        var deletedFiles = 0
        var freedBytes = 0L

        if (tmpDir.exists()) {
            val cutoff = System.currentTimeMillis() - maxTmpAgeHours * 3600_000
            tmpDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) {
                    freedBytes += file.length()
                    if (file.delete()) deletedFiles++
                }
            }
        }

        // Also clear zero-byte / corrupted leftovers anywhere under libraries/versions
        listOf("libraries", "versions").forEach { sub ->
            File(xRoot, sub).walkTopDown().forEach { f ->
                if (f.isFile && f.length() == 0L) {
                    freedBytes += 0
                    if (f.delete()) deletedFiles++
                }
            }
        }

        return CleanupResult(deletedFiles, freedBytes)
    }

    data class CleanupResult(val filesDeleted: Int, val bytesFreed: Long)
}
