package com.x.client.account

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub OAuth Device Flow — used ONLY for the launcher's own identity
 * (profile badge, cloud settings/mod-sync). This never touches Minecraft
 * authentication; it just tells X "this is kiua's launcher install".
 *
 * Register your own OAuth App at github.com/settings/developers to get
 * a client_id — placeholder below, swap in your real one.
 */
class GitHubAccountManager(private val clientId: String) {

    data class DeviceCodeResponse(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val intervalSeconds: Int,
        val expiresInSeconds: Int
    )

    fun requestDeviceCode(): DeviceCodeResponse {
        val body = "client_id=$clientId&scope=read:user"
        val json = post("https://github.com/login/device/code", body)
        return DeviceCodeResponse(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUri = json.getString("verification_uri"),
            intervalSeconds = json.optInt("interval", 5),
            expiresInSeconds = json.optInt("expires_in", 900)
        )
    }

    /** Poll after the user enters the code at verificationUri. Returns null while pending. */
    fun pollForToken(deviceCode: String): String? {
        val body = "client_id=$clientId&device_code=$deviceCode&grant_type=urn:ietf:params:oauth:grant-type:device_code"
        val json = post("https://github.com/login/oauth/access_token", body)
        return json.optString("access_token", null).takeUnless { it.isNullOrBlank() }
    }

    fun fetchUsername(accessToken: String): String {
        val conn = URL("https://api.github.com/user").openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.inputStream.bufferedReader().use {
            return JSONObject(it.readText()).getString("login")
        }
    }

    private fun post(urlStr: String, body: String): JSONObject {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.outputStream.use { it.write(body.toByteArray()) }
        conn.inputStream.bufferedReader().use { return JSONObject(it.readText()) }
    }
}
