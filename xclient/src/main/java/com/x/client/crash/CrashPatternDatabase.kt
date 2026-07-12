package com.x.client.crash

/**
 * Known crash signatures mapped to a plain-language reason + fix.
 * Matched in order — first hit wins, so put more specific patterns first.
 */
object CrashPatternDatabase {

    data class Pattern(val regex: Regex, val title: String, val reason: String, val solution: String)

    val patterns: List<Pattern> = listOf(
        Pattern(
            regex = Regex("java\\.lang\\.OutOfMemoryError"),
            title = "Out of memory",
            reason = "Minecraft ran out of allocated RAM, usually from a heavy modpack or high render distance.",
            solution = "Increase the allocated RAM in X → Settings → Java, or lower render distance / remove some mods."
        ),
        Pattern(
            regex = Regex("UnsatisfiedLinkError.*lwjgl|UnsatisfiedLinkError.*gl4es|UnsatisfiedLinkError.*virgl"),
            title = "Graphics native library failed to load",
            reason = "The LWJGL/renderer native library for your device's CPU architecture is missing or corrupted.",
            solution = "Reinstall the LWJGL natives from X Client settings, or switch renderer (GL4ES/VirGL/Vulkan Zink) in Settings → Renderer."
        ),
        Pattern(
            regex = Regex("UnsatisfiedLinkError"),
            title = "Native library failed to load",
            reason = "A required native (.so) library is missing for your device's architecture.",
            solution = "Re-run the version install so X Client re-downloads the correct native libraries for your device."
        ),
        Pattern(
            regex = Regex("NoClassDefFoundError|ClassNotFoundException"),
            title = "Missing class at runtime",
            reason = "A mod or library expected a class that isn't present — usually a mod loader/version mismatch.",
            solution = "Check the mod is built for your exact Minecraft + loader version (Fabric/Forge/Quilt/NeoForge), and that all its required dependency mods are installed."
        ),
        Pattern(
            regex = Regex("mixin\\.injection|MixinTransformerError|MixinApplyError"),
            title = "Mod conflict (Mixin error)",
            reason = "Two or more mods are trying to modify the same game code and conflict with each other.",
            solution = "Disable mods one at a time from X → Mods to find the conflicting pair, or check the mod's page for known incompatibilities."
        ),
        Pattern(
            regex = Regex("java\\.util\\.zip\\.ZipException|invalid CEN header|zip END header not found"),
            title = "Corrupted file",
            reason = "A jar or zip file (client, library, or mod) was only partially downloaded or is corrupted.",
            solution = "Delete the affected file and let X Client re-download it — usually the version jar or a mod file."
        ),
        Pattern(
            regex = Regex("UnsupportedClassVersionError"),
            title = "Wrong Java version",
            reason = "This Minecraft version needs a newer (or older) Java runtime than the one currently selected.",
            solution = "Go to X → Settings → Java Runtime and switch to the version required by this Minecraft/mod-loader version (shown in the version's install notes)."
        ),
        Pattern(
            regex = Regex("Failed to find Minecraft installation|version json.*not found"),
            title = "Incomplete install",
            reason = "The version's files weren't fully downloaded before launch.",
            solution = "Re-run install for this version from X → Versions, then launch again."
        ),
        Pattern(
            regex = Regex("EGL_BAD_(ALLOC|CONFIG|MATCH)|GLFW error|Failed to create.*context"),
            title = "Graphics context failed to initialize",
            reason = "The selected renderer isn't supported by your device's GPU driver.",
            solution = "Switch renderer in Settings → Renderer — try GL4ES first, it has the widest device compatibility."
        ),
        Pattern(
            regex = Regex("Address already in use|BindException"),
            title = "Port already in use",
            reason = "Another running instance or app is using the same local port Minecraft needs.",
            solution = "Close other running Minecraft/game instances and launch again."
        ),
        Pattern(
            regex = Regex("No space left on device"),
            title = "Storage full",
            reason = "Your device ran out of free storage while writing game files.",
            solution = "Free up storage space, then relaunch — X will resume downloading/writing where it left off."
        )
    )

    fun match(logText: String): Pattern? = patterns.firstOrNull { it.regex.containsMatchIn(logText) }
}
