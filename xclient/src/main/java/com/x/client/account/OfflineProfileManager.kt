package com.x.client.account

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * Offline mode done properly, with things most Android launchers skip:
 * - Deterministic UUID matching vanilla's own offline algorithm (so your
 *   world saves / player data stay consistent across reinstalls, and
 *   servers that whitelist by offline-UUID keep working)
 * - Multiple saved offline profiles with instant switching
 * - Local custom skin/cape assignment per profile (many offline modes
 *   only ever show Steve/Alex — this actually renders your chosen skin
 *   file locally instead)
 * - Per-profile "last used version" memory, so switching profiles also
 *   restores what you were playing last
 */
class OfflineProfileManager(private val xRoot: File) {

    private val storeFile = File(xRoot, "accounts/offline_profiles.json")

    data class OfflineProfile(
        val name: String,
        val uuid: String,
        val localSkinPath: String?,
        val localCapePath: String?,
        val lastVersionId: String?
    )

    fun listProfiles(): List<OfflineProfile> {
        if (!storeFile.exists()) return emptyList()
        val arr = JSONArray(storeFile.readText())
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            OfflineProfile(
                name = o.getString("name"),
                uuid = o.getString("uuid"),
                localSkinPath = o.optString("localSkinPath", null),
                localCapePath = o.optString("localCapePath", null),
                lastVersionId = o.optString("lastVersionId", null)
            )
        }
    }

    fun createOrGetProfile(name: String): OfflineProfile {
        val existing = listProfiles().firstOrNull { it.name == name }
        if (existing != null) return existing

        val profile = OfflineProfile(
            name = name,
            uuid = offlineUuidFor(name),
            localSkinPath = null,
            localCapePath = null,
            lastVersionId = null
        )
        save(listProfiles() + profile)
        return profile
    }

    fun setLocalSkin(name: String, skinFilePath: String?) {
        updateProfile(name) { it.copy(localSkinPath = skinFilePath) }
    }

    fun setLastVersion(name: String, versionId: String) {
        updateProfile(name) { it.copy(lastVersionId = versionId) }
    }

    fun deleteProfile(name: String) {
        save(listProfiles().filterNot { it.name == name })
    }

    fun toAccountProfile(offline: OfflineProfile): AccountProfile {
        return AccountProfile(
            playerName = offline.name,
            uuid = offline.uuid,
            accessToken = "0",
            authType = AuthType.OFFLINE,
            skinUrl = offline.localSkinPath,
            capeUrl = offline.localCapePath
        )
    }

    /** Matches vanilla Minecraft's own offline-mode UUID algorithm exactly:
     *  UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(UTF_8)) */
    fun offlineUuidFor(name: String): String {
        val bytes = "OfflinePlayer:$name".toByteArray(Charsets.UTF_8)
        val md5 = MessageDigest.getInstance("MD5").digest(bytes)
        // Set version 3 UUID bits, exactly as java.util.UUID.nameUUIDFromBytes does
        md5[6] = (md5[6].toInt() and 0x0f or 0x30).toByte()
        md5[8] = (md5[8].toInt() and 0x3f or 0x80).toByte()
        val msb = (0 until 8).fold(0L) { acc, i -> (acc shl 8) or (md5[i].toLong() and 0xff) }
        val lsb = (8 until 16).fold(0L) { acc, i -> (acc shl 8) or (md5[i].toLong() and 0xff) }
        return UUID(msb, lsb).toString()
    }

    private fun updateProfile(name: String, transform: (OfflineProfile) -> OfflineProfile) {
        val updated = listProfiles().map { if (it.name == name) transform(it) else it }
        save(updated)
    }

    private fun save(profiles: List<OfflineProfile>) {
        storeFile.parentFile?.mkdirs()
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(JSONObject().apply {
                put("name", p.name)
                put("uuid", p.uuid)
                put("localSkinPath", p.localSkinPath)
                put("localCapePath", p.localCapePath)
                put("lastVersionId", p.lastVersionId)
            })
        }
        storeFile.writeText(arr.toString())
    }
}
