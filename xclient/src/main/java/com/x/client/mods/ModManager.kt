package com.x.client.mods

import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

data class ModInfo(
    val fileName: String,
    val displayName: String,
    val version: String?,
    val loader: String,
    val enabled: Boolean,
    val filePath: String
)

/**
 * Full control over a version's /mods folder: list, enable/disable
 * (by renaming .jar <-> .jar.disabled, the same trick every launcher uses
 * since mod loaders just skip non-.jar files), add, remove, and expose
 * the raw file for editing/inspection.
 */
class ModManager(private val versionModsDir: File) {

    init {
        versionModsDir.mkdirs()
    }

    fun listMods(): List<ModInfo> {
        val files = versionModsDir.listFiles { f ->
            f.name.endsWith(".jar") || f.name.endsWith(".jar.disabled")
        } ?: emptyArray()

        return files.mapNotNull { file -> readModInfo(file) }
    }

    fun enableMod(mod: ModInfo): Boolean {
        if (mod.enabled) return true
        val current = File(mod.filePath)
        val target = File(versionModsDir, current.name.removeSuffix(".disabled"))
        return current.renameTo(target)
    }

    fun disableMod(mod: ModInfo): Boolean {
        if (!mod.enabled) return true
        val current = File(mod.filePath)
        val target = File(versionModsDir, current.name + ".disabled")
        return current.renameTo(target)
    }

    fun removeMod(mod: ModInfo): Boolean {
        return File(mod.filePath).delete()
    }

    fun addMod(sourceFile: File): ModInfo? {
        val dest = File(versionModsDir, sourceFile.name)
        sourceFile.copyTo(dest, overwrite = true)
        return readModInfo(dest)
    }

    /** Exposes the raw file for a file-editor UI to open/edit directly. */
    fun getModFile(mod: ModInfo): File = File(mod.filePath)

    private fun readModInfo(file: File): ModInfo? {
        val enabled = !file.name.endsWith(".disabled")
        val jarFile = if (enabled) file else File(file.parentFile, file.name.removeSuffix(".disabled"))

        return try {
            ZipFile(file).use { zip ->
                // Try Fabric/Quilt metadata first
                zip.getEntry("fabric.mod.json")?.let { entry ->
                    val json = JSONObject(zip.getInputStream(entry).bufferedReader().readText())
                    return ModInfo(
                        fileName = file.name,
                        displayName = json.optString("name", jarFile.nameWithoutExtension),
                        version = json.optString("version", null),
                        loader = "Fabric/Quilt",
                        enabled = enabled,
                        filePath = file.absolutePath
                    )
                }
                // Forge/NeoForge use mods.toml — TOML parsing kept minimal on purpose
                zip.getEntry("META-INF/mods.toml")?.let {
                    return ModInfo(
                        fileName = file.name,
                        displayName = jarFile.nameWithoutExtension,
                        version = null,
                        loader = "Forge/NeoForge",
                        enabled = enabled,
                        filePath = file.absolutePath
                    )
                }
                // Unknown/legacy mod — still list it, just without rich metadata
                ModInfo(
                    fileName = file.name,
                    displayName = jarFile.nameWithoutExtension,
                    version = null,
                    loader = "Unknown",
                    enabled = enabled,
                    filePath = file.absolutePath
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
