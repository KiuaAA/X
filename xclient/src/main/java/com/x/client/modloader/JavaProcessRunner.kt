package com.x.client.modloader

import java.io.File

/**
 * Runs a jar using the JRE X Client already downloaded into /X/runtime/.
 * Needed for Forge/NeoForge, whose installers are real Java programs
 * that patch the client jar — they can't just be "downloaded", they
 * have to actually execute.
 */
object JavaProcessRunner {

    fun findJavaBinary(xRoot: File, javaMajor: Int, archTag: String): File? {
        val runtimeDir = File(xRoot, "runtime/jre$javaMajor-$archTag")
        if (!runtimeDir.exists()) return null
        val candidate = runtimeDir.walkTopDown().firstOrNull { it.name == "java" && it.canExecute() }
        return candidate
    }

    data class RunResult(val exitCode: Int, val output: String)

    fun run(javaBinary: File, args: List<String>, workingDir: File): RunResult {
        val cmd = mutableListOf(javaBinary.absolutePath)
        cmd.addAll(args)

        val process = ProcessBuilder(cmd)
            .directory(workingDir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return RunResult(exitCode, output)
    }
}
