package com.x.client.download

import android.os.Build

object DeviceAbi {
    /** Maps the device's real ABI to PojavLauncher's runtime naming. */
    fun pojavArchTag(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when {
            abi.contains("arm64") -> "arm64"
            abi.contains("armeabi") -> "arm32"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") -> "x86"
            else -> "arm64"
        }
    }
}
