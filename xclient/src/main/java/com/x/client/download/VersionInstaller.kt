package com.x.client.download

import org.json.JSONObject
import java.io.File

/**
 * Installs a chosen Minecraft version into the X directory layout:
 *
 * /X/versions/<id>/<id>.jar
 * /X/versions/<id>/<id>.json
 * /X/libraries/...
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

            // 2. Libraries (includes LWJGL natives for the right platform)
            listener.onStep("Downloading libraries + LWJGL natives")
            downloadLibraries(versionJson, listener)

            // 3. Java runtime
            listener.onStep("Downloading Java runtime")
            downloadJavaRuntime(versionJson, listener)

            // 4. Assets (sounds, textures index)
            listener.onStep("Downloading assets index")
            downloadAssetIndex(versionJson, listener)

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

            downloads.optJSONObject("classifiers")?.let { classifiers ->
                // natives-linux / native ARM LWJGL builds go here
                val nativesKey = classifiers.keys().asSequence().firstOrNull { it.contains("linux") || it.contains("arm") }
                nativesKey?.let { key ->
                    val native = classifiers.getJSONObject(key)
                    val path = native.getString("path")
                    val dest = File(libRoot, path)
                    FileUtils.downloadFile(native.getString("url"), dest, native.optString("sha1", null))
                }
            }
            listener.onProgress((i * 100) / libs.length())
        }
    }

    private fun appliesToAndroidArm(lib: JSONObject): Boolean {
        val rules = lib.optJSONArray("rules") ?: return true
        // Simplified rule evaluation — refined later once we wire in
        // the actual ARM/Android native-selection logic in Part 3.
        return true
    }

    private fun downloadJavaRuntime(versionJson: JSONObject, listener: ProgressListener) {
        // Java runtime component name (e.g. java-runtime-gamma)
        val javaVersion = versionJson.optJSONObject("javaVersion")
        val component = javaVersion?.optString("component") ?: "jre-legacy"
        // Full ARM64 JRE fetching is handled by RuntimeManager in Part 3 —
        // placeholder call kept here so install() has one entry point.
        RuntimeManager(xRoot).ensureRuntime(component, listener)
    }

    private fun downloadAssetIndex(versionJson: JSONObject, listener: ProgressListener) {
        val assetIndex = versionJson.getJSONObject("assetIndex")
        val dest = File(xRoot, "assets/indexes/${assetIndex.getString("id")}.json")
        FileUtils.downloadFile(assetIndex.getString("url"), dest, assetIndex.optString("sha1", null))
        // Full per-object asset download (sounds/textures) added in Part 3
        // to keep this part focused on the core install flow.
    }
}
