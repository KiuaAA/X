package com.x.launcher.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class FileEditMode { TEXT, HEX, UNSUPPORTED }

private val TEXT_EXTENSIONS = setOf(
    "txt", "toml", "json", "cfg", "properties", "yml", "yaml", "mcmeta", "log", "md"
)

class FileEditorViewModel : ViewModel() {

    var file by mutableStateOf<File?>(null)
        private set
    var mode by mutableStateOf(FileEditMode.UNSUPPORTED)
        private set
    var textContent by mutableStateOf("")
        private set
    var hexPreview by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isDirty by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set
    var saveSuccess by mutableStateOf(false)
        private set

    fun open(target: File) {
        file = target
        isLoading = true
        saveError = null
        saveSuccess = false
        isDirty = false

        viewModelScope.launch {
            val ext = target.extension.lowercase()
            val isJar = ext == "jar" || ext == "zip"

            if (!isJar && (ext in TEXT_EXTENSIONS || looksLikeText(target))) {
                mode = FileEditMode.TEXT
                textContent = withContext(Dispatchers.IO) {
                    try {
                        target.readText()
                    } catch (e: Exception) {
                        saveError = "Couldn't read file: ${e.message}"
                        ""
                    }
                }
            } else {
                mode = FileEditMode.HEX
                hexPreview = withContext(Dispatchers.IO) { buildHexPreview(target) }
            }
            isLoading = false
        }
    }

    fun updateText(newText: String) {
        textContent = newText
        isDirty = true
    }

    fun save() {
        val target = file ?: return
        if (mode != FileEditMode.TEXT) return

        viewModelScope.launch {
            saveError = null
            val ok = withContext(Dispatchers.IO) {
                try {
                    // Write to a temp file first, then atomically replace — protects
                    // against a half-written file if the app gets killed mid-save.
                    val tempFile = File(target.parentFile, "${target.name}.xtmp")
                    tempFile.writeText(textContent)
                    tempFile.renameTo(target)
                } catch (e: Exception) {
                    saveError = e.message ?: "Unknown save error"
                    false
                }
            }
            if (ok) {
                isDirty = false
                saveSuccess = true
            }
        }
    }

    private fun looksLikeText(target: File): Boolean {
        return try {
            val bytes = target.inputStream().use { it.readNBytes(512) }
            // Heuristic: if more than a few bytes are non-printable/non-UTF8-safe, treat as binary
            val nonPrintable = bytes.count { b -> b < 0x09 || (b in 0x0E..0x1F) }
            nonPrintable.toDouble() / bytes.size.coerceAtLeast(1) < 0.05
        } catch (e: Exception) {
            false
        }
    }

    private fun buildHexPreview(target: File, maxBytes: Int = 4096): String {
        return try {
            val bytes = target.inputStream().use { it.readNBytes(maxBytes) }
            val sb = StringBuilder()
            bytes.toList().chunked(16).forEachIndexed { rowIndex, row ->
                val offset = (rowIndex * 16).toString(16).padStart(8, '0')
                val hex = row.joinToString(" ") { "%02x".format(it) }
                val ascii = row.joinToString("") { b ->
                    val c = b.toInt().toChar()
                    if (c.code in 32..126) c.toString() else "."
                }
                sb.append("$offset  ${hex.padEnd(47)}  $ascii\n")
            }
            if (target.length() > maxBytes) sb.append("\n… truncated (${target.length()} bytes total)")
            sb.toString()
        } catch (e: Exception) {
            "Couldn't read file: ${e.message}"
        }
    }
}
