package com.x.launcher.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.x.client.download.Renderer
import org.json.JSONObject
import java.io.File

data class XSettings(
    val ramMinMb: Int = 1024,
    val ramMaxMb: Int = 2048,
    val javaMajor: Int = 17,
    val renderer: Renderer = Renderer.GL4ES,
    val extraJvmArgs: String = ""
)

/**
 * Persists settings to disk under /X/settings.json so they survive app restarts —
 * plain JSON, no database needed for a handful of fields.
 */
class SettingsViewModel(private val xRoot: File) : ViewModel() {

    private val settingsFile = File(xRoot, "settings.json")

    var settings by mutableStateOf(load())
        private set

    fun updateRam(minMb: Int, maxMb: Int) {
        settings = settings.copy(ramMinMb = minMb, ramMaxMb = maxMb)
        save()
    }

    fun updateJavaMajor(major: Int) {
        settings = settings.copy(javaMajor = major)
        save()
    }

    fun updateRenderer(renderer: Renderer) {
        settings = settings.copy(renderer = renderer)
        save()
    }

    fun updateExtraJvmArgs(args: String) {
        settings = settings.copy(extraJvmArgs = args)
        save()
    }

    fun deviceRecommendedMaxRamMb(): Int {
        val runtime = Runtime.getRuntime()
        val totalDeviceRamMb = (runtime.maxMemory() / (1024 * 1024)).toInt().coerceAtLeast(1024)
        // Conservative recommendation: never suggest more than ~60% of what the
        // JVM heap ceiling reports, since Android also needs headroom for itself.
        return (totalDeviceRamMb * 0.6).toInt().coerceIn(512, 8192)
    }

    private fun load(): XSettings {
        if (!settingsFile.exists()) return XSettings()
        return try {
            val json = JSONObject(settingsFile.readText())
            XSettings(
                ramMinMb = json.optInt("ramMinMb", 1024),
                ramMaxMb = json.optInt("ramMaxMb", 2048),
                javaMajor = json.optInt("javaMajor", 17),
                renderer = Renderer.valueOf(json.optString("renderer", Renderer.GL4ES.name)),
                extraJvmArgs = json.optString("extraJvmArgs", "")
            )
        } catch (e: Exception) {
            XSettings()
        }
    }

    private fun save() {
        settingsFile.parentFile?.mkdirs()
        val json = JSONObject().apply {
            put("ramMinMb", settings.ramMinMb)
            put("ramMaxMb", settings.ramMaxMb)
            put("javaMajor", settings.javaMajor)
            put("renderer", settings.renderer.name)
            put("extraJvmArgs", settings.extraJvmArgs)
        }
        settingsFile.writeText(json.toString())
    }
}
