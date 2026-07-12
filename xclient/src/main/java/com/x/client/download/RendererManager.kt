package com.x.client.download

/**
 * X Client renderer selection. Each backend trades off compatibility vs speed —
 * having all four (like ZL2) is what gives "works on weak AND strong devices".
 */
enum class Renderer(val jvmArg: String, val description: String) {
    GL4ES("-Dorg.lwjgl.opengl.libname=libgl4es.so", "Best compatibility, works on almost any GPU"),
    VIRGL("-Dorg.lwjgl.opengl.libname=libvirglrenderer.so", "Good middle ground, needs virgl-capable driver"),
    VULKAN_ZINK("-Dorg.lwjgl.opengl.libname=libzink.so", "Fastest, needs Vulkan 1.1+ GPU"),
    FREEDRENO("-Dorg.lwjgl.opengl.libname=libgallium_freedreno.so", "Adreno-optimized native path");

    companion object {
        fun recommended(supportsVulkan: Boolean, isHighEnd: Boolean): Renderer = when {
            supportsVulkan && isHighEnd -> VULKAN_ZINK
            isHighEnd -> VIRGL
            else -> GL4ES
        }
    }
}
