package com.x.client.launch

import com.x.client.crash.CrashLogParser
import com.x.client.crash.CrashReport
import com.x.client.crash.CrashWatcher
import com.x.client.download.DeviceAbi
import com.x.client.modloader.JavaProcessRunner
import java.io.File

/**
 * Orchestrates an actual launch: resolve version → build args →
 * find matching Java runtime → start process → watch for crashes.
 */
class GameLauncher(private val xRoot: File) {

    interface Listener {
        fun onLaunching(message: String)
        fun onGameOutput(line: String)
        fun onCrash(report: CrashReport)
        fun onExit(code: Int)
    }

    private val crashWatcher = CrashWatcher(xRoot)

    fun launch(
        profile: LaunchProfile,
        javaMajor: Int,
        rendererJvmArg: String,
        listener: Listener
    ) {
        listener.onLaunching("Resolving version ${profile.versionId}")
        val resolved = VersionResolver(xRoot).resolve(profile.versionId)

        val archTag = DeviceAbi.pojavArchTag()
        val nativesDir = File(xRoot, "libraries/lwjgl3-natives/$archTag")
        val javaBinary = JavaProcessRunner.findJavaBinary(xRoot, javaMajor, archTag)
            ?: run {
                listener.onCrash(
                    CrashReport(
                        title = "Java runtime not found",
                        reason = "No Java $javaMajor runtime is installed for this device architecture.",
                        solution = "Go to X → Settings → Java Runtime and install Java $javaMajor, then try launching again.",
                        sourceFile = null,
                        rawExcerpt = ""
                    )
                )
                return
            }

        listener.onLaunching("Building launch arguments")
        val args = LaunchArgumentBuilder(xRoot).build(resolved, profile, nativesDir, rendererJvmArg)

        listener.onLaunching("Starting crash watcher")
        crashWatcher.startWatching(profile.versionId, object : CrashWatcher.Listener {
            override fun onCrashDetected(report: CrashReport) = listener.onCrash(report)
        })

        listener.onLaunching("Launching Minecraft")
        val workingDir = File(xRoot, "versions/${profile.versionId}").apply { mkdirs() }

        try {
            val process = ProcessBuilder(listOf(javaBinary.absolutePath) + args)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()

            val outputBuffer = StringBuilder()
            val reader = process.inputStream.bufferedReader()
            reader.forEachLine { line ->
                listener.onGameOutput(line)
                outputBuffer.appendLine(line)
                if (outputBuffer.length > 20000) {
                    outputBuffer.delete(0, outputBuffer.length - 20000)
                }
            }

            val exitCode = process.waitFor()
            crashWatcher.stopWatching()

            if (exitCode != 0) {
                // No crash-report .txt was necessarily written (native crash / early exit) —
                // fall back to parsing captured stdout/stderr instead.
                val report = CrashLogParser.parse(outputBuffer.toString())
                listener.onCrash(report)
            }
            listener.onExit(exitCode)
        } catch (e: Exception) {
            crashWatcher.stopWatching()
            listener.onCrash(
                CrashReport(
                    title = "Failed to start game process",
                    reason = e.message ?: "Unknown error launching the JVM process.",
                    solution = "Check that the Java runtime binary has execute permission and matches your device's architecture.",
                    sourceFile = null,
                    rawExcerpt = e.stackTraceToString().take(1000)
                )
            )
        }
    }
}
