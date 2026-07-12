package com.x.client.download

import java.io.File

class RuntimeManager(private val xRoot: File) {
    fun ensureRuntime(component: String, listener: VersionInstaller.ProgressListener) {
        listener.onStep("Runtime '$component' will be fetched in Part 3 (JRE ARM64 build)")
    }
}
