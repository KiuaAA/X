package com.x.client.download

import com.x.client.crash.CrashLogParser

object CrashSolutions {
    fun suggest(message: String): String {
        return CrashLogParser.parse(message).solution
    }
}
