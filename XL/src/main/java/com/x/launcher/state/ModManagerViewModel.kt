package com.x.launcher.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.x.client.mods.ModInfo
import com.x.client.mods.ModManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ModManagerViewModel(private val versionModsDir: File) : ViewModel() {

    private val modManager = ModManager(versionModsDir)

    var mods by mutableStateOf<List<ModInfo>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    fun refresh() {
        isLoading = true
        viewModelScope.launch {
            mods = withContext(Dispatchers.IO) { modManager.listMods() }
            isLoading = false
        }
    }

    fun toggle(mod: ModInfo) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                if (mod.enabled) modManager.disableMod(mod) else modManager.enableMod(mod)
            }
            if (ok) refresh() else lastError = "Couldn't toggle ${mod.displayName}"
        }
    }

    fun remove(mod: ModInfo) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { modManager.removeMod(mod) }
            if (ok) refresh() else lastError = "Couldn't remove ${mod.displayName}"
        }
    }

    fun addMod(sourceFile: File) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { modManager.addMod(sourceFile) }
            if (result == null) lastError = "Couldn't add ${sourceFile.name}"
            refresh()
        }
    }

    fun getModFile(mod: ModInfo): File = modManager.getModFile(mod)

    fun clearError() { lastError = null }
}
