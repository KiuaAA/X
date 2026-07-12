package com.x.client.download

import java.io.File

/**
 * LWJGL natives don't come from stock Mojang libraries on Android —
 * they need a custom-built fork (patched for GLES/Zink/VirGL).
 * We pull the prebuilt .so bundle from ZalithLauncher's lwjgl3 fork,
 * same lineage PojavLauncher/ZL2 both ship.
 */
class LWJGLManager(private val xRoot: File) {

    private val owner = "ZalithLauncher"
    private val repo = "lwjgl3"

    fun ensureNatives(listener: VersionInstaller.ProgressListener) {
        val archTag = DeviceAbi.pojavArchTag()
        val nativesDir = File(xRoot, "libraries/lwjgl3-natives/$archTag")

        if (nativesDir.exists() && nativesDir.listFiles()?.isNotEmpty() == true) {
            listener.onStep("LWJGL natives already installed ($archTag)")
            return
        }

        listener.onStep("Looking up LWJGL natives for $archTag")
        val assets = GitHubReleaseFetcher.latestReleaseAssets(owner, repo)

        val match = assets.firstOrNull {
            it.name.contains(archTag, ignoreCase = true) &&
            (it.name.endsWith(".zip") || it.name.endsWith(".tar.xz"))
        } ?: run {
            listener.onError(
                "No LWJGL native build found for $archTag",
                "Check ZalithLauncher/lwjgl3 releases manually — asset naming may " +
                "have changed; adjust the match filter in LWJGLManager."
            )
            return
        }

        listener.onStep("Downloading LWJGL natives (${match.size / 1_000_000} MB)")
        val temp = File(xRoot, "tmp/${match.name}")
        FileUtils.downloadFile(match.downloadUrl, temp)

        listener.onStep("Extracting LWJGL natives")
        if (match.name.endsWith(".tar.xz")) {
            TarXzExtractor.extract(temp, nativesDir)
        } else {
            ZipExtractor.extract(temp, nativesDir)
        }
        temp.delete()

        listener.onStep("LWJGL natives ready")
    }
}
