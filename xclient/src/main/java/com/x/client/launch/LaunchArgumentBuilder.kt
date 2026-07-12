package com.x.client.launch

import org.json.JSONObject
import java.io.File

data class LaunchProfile(
    val playerName: String,
    val uuid: String,
    val accessToken: String,    // "0" for offline/cracked-style local play
    val versionId: String,
    val ramMinMb: Int = 1024,
    val ramMaxMb: Int = 2048,
    val extraJvmArgs: List<String> = emptyList()
)

class LaunchArgumentBuilder(private val xRoot: File) {

    fun build(
        resolved: VersionResolver.ResolvedVersion,
        profile: LaunchProfile,
        nativesDir: File,
        rendererJvmArg: String
    ): List<String> {
        val classpath = (resolved.libraries.map { it.absolutePath } + resolved.clientJar.absolutePath)
            .joinToString(":")

        val substitutions = mapOf(
            "auth_player_name" to profile.playerName,
            "version_name" to profile.versionId,
            "game_directory" to File(xRoot, "versions/${profile.versionId}").absolutePath,
            "assets_root" to File(xRoot, "assets").absolutePath,
            "assets_index_name" to resolved.assetIndexId,
            "auth_uuid" to profile.uuid,
            "auth_access_token" to profile.accessToken,
            "user_type" to "msa",
            "version_type" to "release",
            "natives_directory" to nativesDir.absolutePath,
            "launcher_name" to "X",
            "launcher_version" to "1.0",
            "classpath" to classpath
        )

        val args = mutableListOf<String>()

        // JVM args
        args += "-Xms${profile.ramMinMb}M"
        args += "-Xmx${profile.ramMaxMb}M"
        args += "-Djava.library.path=${nativesDir.absolutePath}"
        args += rendererJvmArg
        args += profile.extraJvmArgs
        args += resolveArgList(resolved.jvmArgs, substitutions)

        args += "-cp"
        args += classpath
        args += resolved.mainClass

        // Game args
        args += resolveArgList(resolved.gameArgs, substitutions)

        return args
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveArgList(rawArgs: List<Any>, substitutions: Map<String, String>): List<String> {
        val out = mutableListOf<String>()
        for (item in rawArgs) {
            when (item) {
                is String -> out += substitute(item, substitutions)
                is JSONObject -> {
                    // Conditional rule entry (os/feature match) — kept permissive here;
                    // strict OS/feature rule filtering can be tightened later if a
                    // specific mod needs it.
                    val value = item.opt("value")
                    when (value) {
                        is String -> out += substitute(value, substitutions)
                        else -> {
                            val arr = item.optJSONArray("value")
                            if (arr != null) {
                                for (i in 0 until arr.length()) {
                                    out += substitute(arr.getString(i), substitutions)
                                }
                            }
                        }
                    }
                }
            }
        }
        return out
    }

    private fun substitute(raw: String, substitutions: Map<String, String>): String {
        var result = raw
        for ((key, value) in substitutions) {
            result = result.replace("\${$key}", value)
        }
        return result
    }
}
