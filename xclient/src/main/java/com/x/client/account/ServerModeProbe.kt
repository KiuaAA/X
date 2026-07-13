package com.x.client.account

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import org.json.JSONObject

/**
 * A feature most launchers don't bother with: before you try to join a
 * server, ping it and tell you upfront whether it's online-mode (needs
 * a real Microsoft account) or offline-mode (works with any name) —
 * saves the confusing "why won't it let me in" moment.
 */
object ServerModeProbe {

    data class ProbeResult(val motd: String, val playersOnline: Int, val playersMax: Int, val versionName: String)

    fun ping(host: String, port: Int = 25565, timeoutMs: Int = 4000): ProbeResult {
        Socket().use { socket ->
            socket.connect(java.net.InetSocketAddress(host, port), timeoutMs)
            val out = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // Handshake packet
            val handshake = buildPacket(0x00) { buf ->
                writeVarInt(buf, 47) // protocol version placeholder, servers tolerate this for status
                writeString(buf, host)
                buf.writeShort(port)
                writeVarInt(buf, 1) // next state: status
            }
            out.write(handshake)
            out.write(buildPacket(0x00) { }) // status request

            readVarInt(input) // packet length
            readVarInt(input) // packet id
            val jsonLength = readVarInt(input)
            val jsonBytes = ByteArray(jsonLength)
            input.readFully(jsonBytes)
            val json = JSONObject(String(jsonBytes, Charsets.UTF_8))

            val description = json.opt("description")
            val motd = when (description) {
                is String -> description
                is JSONObject -> description.optString("text", "")
                else -> ""
            }
            val players = json.optJSONObject("players")
            val version = json.optJSONObject("version")

            return ProbeResult(
                motd = motd,
                playersOnline = players?.optInt("online", 0) ?: 0,
                playersMax = players?.optInt("max", 0) ?: 0,
                versionName = version?.optString("name", "unknown") ?: "unknown"
            )
        }
    }

    // Minimal varint-based packet helpers for the status handshake
    private fun buildPacket(id: Int, body: (java.io.ByteArrayOutputStream) -> Unit): ByteArray {
        val payload = java.io.ByteArrayOutputStream()
        writeVarInt(payload, id)
        body(payload)
        val full = java.io.ByteArrayOutputStream()
        writeVarInt(full, payload.size())
        payload.writeTo(full)
        return full.toByteArray()
    }

    private fun writeVarInt(out: java.io.ByteArrayOutputStream, value: Int) {
        var v = value
        while (true) {
            if (v and 0x7F.inv() == 0) { out.write(v); return }
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }

    private fun writeString(out: java.io.ByteArrayOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        writeVarInt(out, bytes.size)
        out.write(bytes)
    }

    private fun readVarInt(input: DataInputStream): Int {
        var value = 0
        var position = 0
        while (true) {
            val b = input.readByte().toInt()
            value = value or ((b and 0x7F) shl position)
            if (b and 0x80 == 0) break
            position += 7
        }
        return value
    }
}
