package com.x.client.modloader

import com.x.client.download.DeviceAbi
import com.x.client.download.FileUtils
import com.x.client.download.VersionInstaller
import java.io.File

/**
 * Same headless-installer approach as Forge, pointed at NeoForge's maven.
 */
class NeoForgeInstaller(private val xRoot: File) : ModLoaderInstaller {

    private val mavenBase = "https://maven.neoforged.net/releases/net/neoforged/neoforge"

    override fun listAvailableVersions(minecraftVersion: String): List<LoaderVersion> {
        // Same note as ForgeInstaller — full maven-metadata.xml parsing added
        // in the mod-browser UI part.
        return emptyList()
    }

    override fun install(
        minecraftVersion: String,
        loaderVersion: String,
        listener: VersionInstaller.ProgressListener
    ) {
        val installerUrl = "$mavenBase/$loaderVersion/neoforge-$loaderVersion-installer.jar"
        val installerFile = File(xRoot, "tmp/neoforge-$loaderVersion-installer.jar")

        listener.onStep("Downloading NeoForge installer $loaderVersion")
        val ok = FileUtils.downloadFile(installerUrl, installerFile)
        if (!ok) {
            listener.onError(
                "NeoForge installer not found for $loaderVersion",
                "Double check the build number exists on maven.neoforged.net."
            )
            return
        }

        listener.onStep("Locating Java runtime to run the installer")
        val archTag = DeviceAbi.pojavArchTag()
        val javaBinary = JavaProcessRunner.findJavaBinary(xRoot, 21, archTag)
            ?: run {
                listener.onError(
                    "No Java runtime available to run NeoForge installer",
                    "NeoForge needs Java 21 — install that runtime first."
                )
                return
            }

        listener.onStep("Running NeoForge installer (this can take a minute)")
        val result = JavaProcessRunner.run(
            javaBinary,
            listOf("-jar", installerFile.absolutePath, "--installClient", xRoot.absolutePath),
            workingDir = xRoot
        )

        if (result.exitCode != 0) {
            listener.onError(
                "NeoForge installer exited with code ${result.exitCode}",
                "Check the installer log — usually a Java version mismatch or missing vanilla base install."
            )
            return
        }

        installerFile.delete()
        listener.onStep("NeoForge $loaderVersion installed for $minecraftVersion")
        listener.onComplete()
    }
}
