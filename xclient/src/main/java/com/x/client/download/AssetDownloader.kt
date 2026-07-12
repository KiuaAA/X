package com.x.client.download

import org.json.JSONObject
import java.io.File

/**
 * Downloads the actual sound/texture objects referenced by an assets index,
 * using Mojang's official CDN + hash-sharded object layout.
 */
class AssetDownloader(private val xRoot: File) {

    fun downloadObjects(assetIndexId: String, listener: VersionInstaller.ProgressListener) {
        val indexFile = File(xRoot, "assets/indexes/$assetIndexId.json")
        if (!indexFile.exists()) {
            listener.onError(
                "Missing asset index $assetIndexId",
                "Re-run install so the index file downloads before objects."
            )
            return
        }

        val json = JSONObject(indexFile.readText())
        val objects = json.getJSONObject("objects")
        val keys = objects.keys().asSequence().toList()
        val objectsDir = File(xRoot, "assets/objects")

        keys.forEachIndexed { i, key ->
            val obj = objects.getJSONObject(key)
            val hash = obj.getString("hash")
            val subDir = hash.substring(0, 2)
            val dest = File(objectsDir, "$subDir/$hash")
            val url = "https://resources.download.minecraft.net/$subDir/$hash"
            FileUtils.downloadFile(url, dest, hash)

            if (i % 25 == 0) {
                listener.onProgress((i * 100) / keys.size)
            }
        }
        listener.onStep("Assets ready (${keys.size} files)")
    }
}
