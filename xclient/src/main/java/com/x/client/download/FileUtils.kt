package com.x.client.download

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object FileUtils {

    fun downloadFile(url: String, dest: File, expectedSha1: String? = null): Boolean {
        dest.parentFile?.mkdirs()

        if (dest.exists() && expectedSha1 != null && sha1(dest) == expectedSha1) {
            return true // already correct, skip re-downloading
        }

        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.inputStream.use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }

        if (expectedSha1 != null) {
            val actual = sha1(dest)
            if (!actual.equals(expectedSha1, ignoreCase = true)) {
                dest.delete()
                return false
            }
        }
        return true
    }

    fun sha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
