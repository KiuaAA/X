package com.x.client.download

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pulls public release assets from GitHub — no auth needed,
 * since we use the Releases API (not CI Actions artifacts,
 * which do require a logged-in GitHub session).
 */
object GitHubReleaseFetcher {

    data class Asset(val name: String, val downloadUrl: String, val size: Long)

    fun latestReleaseAssets(owner: String, repo: String): List<Asset> {
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val json = httpGetJson(url)
        val assets: JSONArray = json.getJSONArray("assets")
        val list = mutableListOf<Asset>()
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            list.add(
                Asset(
                    name = a.getString("name"),
                    downloadUrl = a.getString("browser_download_url"),
                    size = a.getLong("size")
                )
            )
        }
        return list
    }

    private fun httpGetJson(urlStr: String): JSONObject {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "X-Launcher")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.inputStream.bufferedReader().use { return JSONObject(it.readText()) }
    }
}
