package com.x.launcher.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.x.client.download.MinecraftVersionManifest
import com.x.client.modloader.ModLoaderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VersionFilter { ALL, RELEASE, SNAPSHOT, OLD }

data class VersionListItem(
    val id: String,
    val type: String,
    val loaderType: ModLoaderType = ModLoaderType.VANILLA
)

class VersionListViewModel : ViewModel() {

    var allVersions by mutableStateOf<List<VersionListItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var filter by mutableStateOf(VersionFilter.RELEASE)
    var searchQuery by mutableStateOf("")

    fun loadVersions() {
        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                val versions = withContext(Dispatchers.IO) {
                    MinecraftVersionManifest.fetchAllVersions()
                }
                allVersions = versions.map { VersionListItem(it.id, it.type) }
            } catch (e: Exception) {
                error = "Couldn't load version list: ${e.message ?: "network error"}"
            } finally {
                isLoading = false
            }
        }
    }

    fun filteredVersions(): List<VersionListItem> {
        val byType = when (filter) {
            VersionFilter.ALL -> allVersions
            VersionFilter.RELEASE -> allVersions.filter { it.type == "release" }
            VersionFilter.SNAPSHOT -> allVersions.filter { it.type == "snapshot" }
            VersionFilter.OLD -> allVersions.filter { it.type.startsWith("old_") }
        }
        return if (searchQuery.isBlank()) byType
        else byType.filter { it.id.contains(searchQuery, ignoreCase = true) }
    }
}
