package com.x.client.crash

import java.io.File

object CrashLogParser {

    fun parse(logText: String, sourceFile: String? = null): CrashReport {
        val match = CrashPatternDatabase.match(logText)
        val excerpt = extractExcerpt(logText, match?.regex)

        return if (match != null) {
            CrashReport(
                title = match.title,
                reason = match.reason,
                solution = match.solution,
                sourceFile = sourceFile,
                rawExcerpt = excerpt
            )
        } else {
            CrashReport(
                title = "Unrecognized error",
                reason = "This crash doesn't match a known pattern yet.",
                solution = "Check the full log below for the first 'Caused by:' line — that usually names the real source (a specific mod or file).",
                sourceFile = sourceFile,
                rawExcerpt = excerpt
            )
        }
    }

    fun parseFile(file: File): CrashReport {
        val text = if (file.exists()) file.readText() else ""
        return parse(text, file.absolutePath)
    }

    private fun extractExcerpt(logText: String, matchedRegex: Regex?): String {
        val lines = logText.lines()
        val idx = matchedRegex?.let { regex ->
            lines.indexOfFirst { regex.containsMatchIn(it) }
        } ?: -1

        return if (idx >= 0) {
            val start = maxOf(0, idx - 2)
            val end = minOf(lines.size, idx + 5)
            lines.subList(start, end).joinToString("\n")
        } else {
            lines.takeLast(15).joinToString("\n")
        }
    }
}
