package com.x.client.download

import org.json.JSONObject
import java.io.File

/**
 * Installs a chosen Minecraft version into the X directory layout:
 *
 * /X/versions/<id>/<id>.jar
 * /X/versions/<id>/<id>.json
 * /X/libraries/...
 * /X/libraries/lwjgl3-natives/<arch>/...
 * /X/assets/...
 * /X/runtime/<javaVersion>/...
 */
class VersionInstaller(private val xRoot: File) {

    interface ProgressListener {
        fun onStep(message: String)
        fun onProgress(percent: Int)
        fun onError(message: String, solution: String)
        fun onComplete()
    }

    fun install(versionId: String, listener: ProgressListener) {
        try {
            listener.onStep("Fetching version manifest")
            val entry = MinecraftVersionManifest.fetchAllVersions()
                .firstOrNull { it.id == versionId }
                ?: run {
                    listener.onError(
                        "Version $versionId not found",
                        "Check the version ID matches Mojang's manifest exactly."
                    )
                    return
                }

            listener.onStep("Fetching version metadata")
            val versionJson = MinecraftVersionManifest.fetchVersionJson(entry.url)

            // 1. Client jar
            listener.onStep("Downloading client jar")
            val clientDl = versionJson.getJSONObject("downloads").getJSONObject("client")
            val versionDir = File(xRoot, "versions/$versionId")
            val jarFile = File(versionDir, "$versionId.jar")
            FileUtils.downloadFile(clientDl.getString("url"), jarFile, clientDl.getString("sha1"))
            File(versionDir, "$versionId.json").writeText(versionJson.toString())

            // 2. Libraries (Mojang libs only — LWJGL natives are handled separately below,
            //    since Android needs a custom-built LWJGL fork, not stock desktop natives)
            listener.onStep("Downloading libraries")
            downloadLibraries(versionJson, listener)

            // 2b. LWJGL natives for this device's architecture
            listener.onStep("Downloading LWJGL natives")
            LWJGLManager(xRoot).ensureNatives(listener)

            // 3. Java runtime
            listener.onStep("Downloading Java runtime")
            downloadJavaRuntime(versionJson, listener)

            // 4. Asset index
            listener.onStep("Downloading assets index")
            downloadAssetIndex(versionJson, listener)

            // 4b. Actual asset objects (sounds, textures) referenced by the index
            listener.onStep("Downloading asset objects")
            val assetIndexId = versionJson.getJSONObject("assetIndex").getString("id")
            AssetDownloader(xRoot).downloadObjects(assetIndexId, listener)

            listener.onComplete()
        } catch (e: Exception) {
            listener.onError(
                "Install failed: ${e.javaClass.simpleName}",
                CrashSolutions.suggest(e.message ?: "")
            )
        }
    }

    private fun downloadLibraries(versionJson: JSONObject, listener: ProgressListener) {
        val libs = versionJson.getJSONArray("libraries")
        val libRoot = File(xRoot, "libraries")
        for (i in 0 until libs.length()) {
            val lib = libs.getJSONObject(i)
            if (!appliesToAndroidArm(lib)) continue

            val downloads = lib.optJSONObject("downloads") ?: continue

            downloads.optJSONObject("artifact")?.let { artifact ->
                val path = artifact.getString("path")
                val dest = File(libRoot, path)
                FileUtils.downloadFile(artifact.getString("url"), dest, artifact.optString("sha1", null))
            }

            listener.onProgress((i * 100) / libs.length())
        }
    }

    private fun appliesToAndroidArm(lib: JSONObject): Boolean {
        val rules = lib.optJSONArray("rules") ?: return true
        // Simplified rule evaluation — refined once full OS/arch rule
        // matching is wired in during the mod-loader part.
        return true
    }

    private fun downloadJavaRuntime(versionJson: JSONObject, listener: ProgressListener) {
        val javaVersion = versionJson.optJSONObject("javaVersion")
        val component = javaVersion?.optString("component") ?: "jre-legacy"
        RuntimeManager(xRoot).ensureRuntime(component, listener)
    }

    private fun downloadAssetIndex(versionJson: JSONObject, listener: ProgressListener) {
        val assetIndex = versionJson.getJSONObject("assetIndex")
        val dest = File(xRoot, "assets/indexes/${assetIndex.getString("id")}.json")
        FileUtils.downloadFile(assetIndex.getString("url"), dest, assetIndex.optString("sha1", null))
    }
}
