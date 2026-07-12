package com.x.client.modloader

import com.x.client.download.DeviceAbi
import com.x.client.download.FileUtils
import com.x.client.download.VersionInstaller
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Forge doesn't have a clean "just download the jar" flow — its installer
 * is a real Java program that patches the vanilla client. We download the
 * official installer jar from Forge's maven and execute it headlessly
 * with the JRE X Client already fetched, same as PojavLauncher/ZL2 do.
 */
class ForgeInstaller(private val xRoot: File) : ModLoaderInstaller {

    private val mavenBase = "https://maven.minecraftforge.net/net/minecraftforge/forge"

    override fun listAvailableVersions(minecraftVersion: String): List<LoaderVersion> {
        // Forge versions are named "<mcVersion>-<forgeVersion>" in their maven-metadata.xml.
        // Full XML parsing wired in the mod-browser UI part; this returns a
        // manual-entry placeholder so install() below can still be used directly.
        return emptyList()
    }

    override fun install(
        minecraftVersion: String,
        loaderVersion: String,
        listener: VersionInstaller.ProgressListener
    ) {
        // loaderVersion here is expected as "<minecraftVersion>-<forgeBuild>",
        // e.g. "1.20.1-47.2.20"
        val installerUrl = "$mavenBase/$loaderVersion/forge-$loaderVersion-installer.jar"
        val installerFile = File(xRoot, "tmp/forge-$loaderVersion-installer.jar")

        listener.onStep("Downloading Forge installer $loaderVersion")
        val ok = FileUtils.downloadFile(installerUrl, installerFile)
        if (!ok) {
            listener.onError(
                "Forge installer not found for $loaderVersion",
                "Double check the Forge build number exists for $minecraftVersion on maven.minecraftforge.net."
            )
            return
        }

        listener.onStep("Locating Java runtime to run the installer")
        val archTag = DeviceAbi.pojavArchTag()
        val javaBinary = JavaProcessRunner.findJavaBinary(xRoot, 17, archTag)
            ?: run {
                listener.onError(
                    "No Java runtime available to run Forge installer",
                    "Install a Java 17 runtime first (Vanilla install does this automatically)."
                )
                return
            }

        listener.onStep("Running Forge installer (this can take a minute)")
        val minecraftRoot = xRoot // installer writes into versions/, libraries/ under here
        val result = JavaProcessRunner.run(
            javaBinary,
            listOf("-jar", installerFile.absolutePath, "--installClient", minecraftRoot.absolutePath),
            workingDir = xRoot
        )

        if (result.exitCode != 0) {
            listener.onError(
                "Forge installer exited with code ${result.exitCode}",
                "Check the installer log — common causes: incompatible Java version, " +
                "missing vanilla version installed first, or corrupted installer download."
            )
            return
        }

        installerFile.delete()
        listener.onStep("Forge $loaderVersion installed for $minecraftVersion")
        listener.onComplete()
    }
}
