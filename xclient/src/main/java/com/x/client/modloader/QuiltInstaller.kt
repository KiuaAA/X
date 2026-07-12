package com.x.client.modloader

import com.x.client.download.FileUtils
import com.x.client.download.VersionInstaller
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class QuiltInstaller(private val xRoot: File) : ModLoaderInstaller {

    private val metaBase = "https://meta.quiltmc.org/v3"

    override fun listAvailableVersions(minecraftVersion: String): List<LoaderVersion> {
        val json = httpGetJsonArray("$metaBase/versions/loader/$minecraftVersion")
        val list = mutableListOf<LoaderVersion>()
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            val loader = obj.getJSONObject("loader")
            list.add(
                LoaderVersion(
                    loaderType = ModLoaderType.QUILT,
                    minecraftVersion = minecraftVersion,
                    loaderVersion = loader.getString("version"),
                    stable = !loader.getString("version").contains("beta", ignoreCase = true)
                )
            )
        }
        return list
    }

    override fun install(
        minecraftVersion: String,
        loaderVersion: String,
        listener: VersionInstaller.ProgressListener
    ) {
        listener.onStep("Fetching Quilt profile ($loaderVersion)")
        val profileUrl = "$metaBase/versions/loader/$minecraftVersion/$loaderVersion/profile/json"
        val profileJson = httpGetJsonObject(profileUrl)

        val profileId = profileJson.getString("id")
        val versionDir = File(xRoot, "versions/$profileId")
        File(versionDir, "$profileId.json").apply {
            parentFile?.mkdirs()
            writeText(profileJson.toString())
        }

        listener.onStep("Downloading Quilt loader libraries")
        val libraries = profileJson.getJSONArray("libraries")
        val libRoot = File(xRoot, "libraries")
        for (i in 0 until libraries.length()) {
            val lib = libraries.getJSONObject(i)
            val name = lib.getString("name")
            val url = lib.getString("url")
            val path = mavenNameToPath(name)
            val dest = File(libRoot, path)
            FileUtils.downloadFile(url.trimEnd('/') + "/" + path, dest)
            listener.onProgress((i * 100) / libraries.length())
        }

        listener.onStep("Quilt $loaderVersion installed for $minecraftVersion")
        listener.onComplete()
    }

    private fun mavenNameToPath(name: String): String {
        val (group, artifact, version) = name.split(":")
        val groupPath = group.replace(".", "/")
        return "$groupPath/$artifact/$version/$artifact-$version.jar"
    }

    private fun httpGetJsonArray(urlStr: String): JSONArray = JSONArray(httpGet(urlStr))
    private fun httpGetJsonObject(urlStr: String): JSONObject = JSONObject(httpGet(urlStr))

    private fun httpGet(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.inputStream.bufferedReader().use { return it.readText() }
    }
}
