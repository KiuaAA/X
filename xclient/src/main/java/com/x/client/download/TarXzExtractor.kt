package com.x.client.download

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileOutputStream

object TarXzExtractor {

    fun extract(tarXzFile: File, destDir: File) {
        destDir.mkdirs()
        XZInputStream(tarXzFile.inputStream().buffered()).use { xz ->
            TarArchiveInputStream(xz).use { tar ->
                var entry: TarArchiveEntry? = tar.nextTarEntry
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { out -> tar.copyTo(out) }
                        if (entry.mode and 0b001000000 != 0) {
                            outFile.setExecutable(true)
                        }
                    }
                    entry = tar.nextTarEntry
                }
            }
        }
    }
}
