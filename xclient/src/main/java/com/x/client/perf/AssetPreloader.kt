package com.x.client.perf

import com.x.client.download.MinecraftVersionManifest
import java.io.File

/**
 * Runs quietly in the background (from XClientService) the moment the app opens —
 * fetches the version manifest and caches it to disk immediately, so opening the
 * version picker later feels instant instead of waiting on a network call.
 * This is exactly the kind of "feels smoother" work a background client should do.
 */
class AssetPreloader(private val xRoot: File) {

    private val manifestCache = File(xRoot, "cache/version_manifest.json")

    fun warmVersionManifestCache(): Boolean {
        return try {
            val versions = MinecraftVersionManifest.fetchAllVersions()
            manifestCache.parentFile?.mkdirs()
            val json = org.json.JSONArray()
            versions.forEach {
                json.put(org.json.JSONObject().apply {
                    put("id", it.id); put("type", it.type); put("url", it.url)
                })
            }
            manifestCache.writeText(json.toString())
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readCachedManifest(): List<MinecraftVersionManifest.VersionEntry>? {
        if (!manifestCache.exists()) return null
        return try {
            val arr = org.json.JSONArray(manifestCache.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MinecraftVersionManifest.VersionEntry(o.getString("id"), o.getString("type"), o.getString("url"))
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Cache age in minutes — used to decide "show cached instantly, refresh silently after". */
    fun cacheAgeMinutes(): Long {
        if (!manifestCache.exists()) return Long.MAX_VALUE
        return (System.currentTimeMillis() - manifestCache.lastModified()) / 60000
    }
}
