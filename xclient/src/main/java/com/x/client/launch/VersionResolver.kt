package com.x.client.launch

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Merges a version's JSON with its parent chain (inheritsFrom) — this is how
 * Fabric/Quilt/Forge profiles work: they only store what's different from
 * vanilla, and expect the launcher to merge the rest in.
 */
class VersionResolver(private val xRoot: File) {

    data class ResolvedVersion(
        val mainClass: String,
        val libraries: List<File>,
        val clientJar: File,
        val jvmArgs: List<Any>,   // strings or conditional-rule objects
        val gameArgs: List<Any>,
        val assetIndexId: String
    )

    fun resolve(versionId: String): ResolvedVersion {
        val chain = mutableListOf<JSONObject>()
        var currentId: String? = versionId
        val visited = mutableSetOf<String>()

        while (currentId != null && visited.add(currentId)) {
            val versionFile = File(xRoot, "versions/$currentId/$currentId.json")
            require(versionFile.exists()) { "Missing version json for $currentId" }
            val json = JSONObject(versionFile.readText())
            chain.add(json)
            currentId = json.optString("inheritsFrom", "").ifEmpty { null }
        }
        // chain[0] = most specific (e.g. Fabric profile), last = vanilla base

        val mainClass = chain.first().optString("mainClass").ifEmpty {
            chain.mapNotNull { it.optString("mainClass", null) }.firstOrNull()
                ?: "net.minecraft.client.main.Main"
        }

        val libraries = mutableListOf<File>()
        val libRoot = File(xRoot, "libraries")
        for (versionJson in chain) {
            val libs = versionJson.optJSONArray("libraries") ?: continue
            for (i in 0 until libs.length()) {
                val lib = libs.getJSONObject(i)
                val artifact = lib.optJSONObject("downloads")?.optJSONObject("artifact")
                val path = artifact?.optString("path")
                    ?: lib.optString("name", null)?.let { mavenNameToPath(it) }
                if (path != null) {
                    libraries.add(File(libRoot, path))
                }
            }
        }

        // Base vanilla entry always has the client jar under its own id
        val baseId = chain.last().optString("id", versionId)
        val clientJar = File(xRoot, "versions/$baseId/$baseId.jar")

        val jvmArgs = mutableListOf<Any>()
        val gameArgs = mutableListOf<Any>()
        // Specific profile's arguments take priority order (Fabric first, vanilla appended)
        for (versionJson in chain) {
            val args = versionJson.optJSONObject("arguments")
            args?.optJSONArray("jvm")?.let { arr -> jvmArgs.addAll(jsonArrayToList(arr)) }
            args?.optJSONArray("game")?.let { arr -> gameArgs.addAll(jsonArrayToList(arr)) }
            // Very old versions use "minecraftArguments" (a plain string) instead
            versionJson.optString("minecraftArguments", "").ifEmpty { null }?.let {
                gameArgs.addAll(it.split(" "))
            }
        }

        val assetIndexId = chain.mapNotNull { it.optJSONObject("assetIndex")?.optString("id") }
            .firstOrNull() ?: "legacy"

        return ResolvedVersion(mainClass, libraries, clientJar, jvmArgs, gameArgs, assetIndexId)
    }

    private fun jsonArrayToList(arr: JSONArray): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until arr.length()) {
            list.add(arr.get(i)) // could be String or JSONObject (conditional rule)
        }
        return list
    }

    private fun mavenNameToPath(name: String): String {
        val parts = name.split(":")
        if (parts.size < 3) return name
        val (group, artifact, version) = parts
        val groupPath = group.replace(".", "/")
        return "$groupPath/$artifact/$version/$artifact-$version.jar"
    }
}
