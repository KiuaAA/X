package com.x.client.download

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to Mojang's official public version manifest.
 * No copyrighted files are bundled with the app — everything
 * is pulled live from Mojang's own CDN, same as the official launcher.
 */
object MinecraftVersionManifest {

    private const val MANIFEST_URL =
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    data class VersionEntry(
        val id: String,
        val type: String,
        val url: String
    )

    fun fetchAllVersions(): List<VersionEntry> {
        val json = httpGetJson(MANIFEST_URL)
        val arr: JSONArray = json.getJSONArray("versions")
        val list = mutableListOf<VersionEntry>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                VersionEntry(
                    id = obj.getString("id"),
                    type = obj.getString("type"),
                    url = obj.getString("url")
                )
            )
        }
        return list
    }

    fun fetchVersionJson(versionUrl: String): JSONObject {
        return httpGetJson(versionUrl)
    }

    private fun httpGetJson(urlStr: String): JSONObject {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.inputStream.bufferedReader().use { reader ->
            val text = reader.readText()
            return JSONObject(text)
        }
    }
}
