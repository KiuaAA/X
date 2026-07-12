package com.x.client.modloader

enum class ModLoaderType { VANILLA, FABRIC, QUILT, FORGE, NEOFORGE }

data class LoaderVersion(
    val loaderType: ModLoaderType,
    val minecraftVersion: String,
    val loaderVersion: String,
    val stable: Boolean
)

interface ModLoaderInstaller {
    fun listAvailableVersions(minecraftVersion: String): List<LoaderVersion>
    fun install(
        minecraftVersion: String,
        loaderVersion: String,
        listener: com.x.client.download.VersionInstaller.ProgressListener
    )
}
