package com.x.client.perf

import com.x.client.download.FileUtils
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

/**
 * The single biggest real speed win for a launcher: Mojang/Fabric/Forge downloads
 * are hundreds of small files (libraries, assets). Downloading them one at a time
 * (what Part 2-5's code does by default) is the main reason installs feel slow.
 * This runs N downloads in parallel with a bounded thread pool.
 */
class ParallelDownloadQueue(private val maxConcurrent: Int = 8) {

    data class Job(val url: String, val dest: File, val sha1: String? = null)

    interface Listener {
        fun onFileDone(completed: Int, total: Int)
        fun onFileFailed(job: Job, error: Exception)
    }

    fun runAll(jobs: List<Job>, listener: Listener) {
        if (jobs.isEmpty()) return
        val executor = Executors.newFixedThreadPool(maxConcurrent)
        val semaphore = Semaphore(maxConcurrent)
        val completed = AtomicInteger(0)
        val latch = java.util.concurrent.CountDownLatch(jobs.size)

        for (job in jobs) {
            executor.submit {
                semaphore.acquire()
                try {
                    FileUtils.downloadFile(job.url, job.dest, job.sha1)
                    listener.onFileDone(completed.incrementAndGet(), jobs.size)
                } catch (e: Exception) {
                    listener.onFileFailed(job, e)
                    completed.incrementAndGet()
                } finally {
                    semaphore.release()
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()
    }
}
