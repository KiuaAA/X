package com.x.client.download

import java.io.File

class RuntimeManager(private val xRoot: File) {

    private val owner = "PojavLauncherTeam"
    private val repo = "android-openjdk-build-multiarch"

    /**
     * component: e.g. "jre-legacy" (Java 8), "java-runtime-gamma" (Java 17),
     * "java-runtime-delta" (Java 21) — as declared in the Mojang version JSON.
     */
    fun ensureRuntime(component: String, listener: VersionInstaller.ProgressListener) {
        val javaMajor = mapComponentToJavaMajor(component)
        val archTag = DeviceAbi.pojavArchTag()
        val runtimeDir = File(xRoot, "runtime/jre$javaMajor-$archTag")

        if (runtimeDir.exists() && runtimeDir.listFiles()?.isNotEmpty() == true) {
            listener.onStep("Runtime jre$javaMajor ($archTag) already installed")
            return
        }

        listener.onStep("Looking up jre$javaMajor build for $archTag")
        val assets = GitHubReleaseFetcher.latestReleaseAssets(owner, repo)

        // Expected naming pattern from this repo: jre<major>_<arch>.tar.xz
        val match = assets.firstOrNull {
            it.name.contains("jre$javaMajor", ignoreCase = true) &&
            it.name.contains(archTag, ignoreCase = true) &&
            it.name.endsWith(".tar.xz")
        } ?: run {
            listener.onError(
                "No runtime build found for jre$javaMajor / $archTag",
                "This device architecture ($archTag) may not have a prebuilt Java $javaMajor. " +
                "Check PojavLauncherTeam/android-openjdk-build-multiarch releases manually."
            )
            return
        }

        listener.onStep("Downloading ${match.name} (${match.size / 1_000_000} MB)")
        val tempFile = File(xRoot, "tmp/${match.name}")
        FileUtils.downloadFile(match.downloadUrl, tempFile)

        listener.onStep("Extracting runtime")
        TarXzExtractor.extract(tempFile, runtimeDir)
        tempFile.delete()

        listener.onStep("Runtime jre$javaMajor ready")
    }

    private fun mapComponentToJavaMajor(component: String): Int = when (component) {
        "jre-legacy" -> 8
        "java-runtime-alpha" -> 8
        "java-runtime-beta" -> 16
        "java-runtime-gamma", "java-runtime-gamma-snapshot" -> 17
        "java-runtime-delta" -> 21
        else -> 17
    }
}
