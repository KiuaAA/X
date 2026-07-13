package com.x.client.account

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Real Minecraft auth chain: Microsoft device code -> Xbox Live -> XSTS -> Minecraft services.
 * This is the only legitimate way to get a token a real online-mode server will accept —
 * same chain used by the official launcher and by PojavLauncher/ZL2.
 *
 * clientId below is a placeholder — register an Azure AD app to get your own.
 */
class MicrosoftAuthManager(private val clientId: String) {

    data class DeviceCodeResponse(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val intervalSeconds: Int
    )

    fun requestDeviceCode(): DeviceCodeResponse {
        val body = "client_id=$clientId&scope=XboxLive.signin%20offline_access"
        val json = post("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode", body)
        return DeviceCodeResponse(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUri = json.getString("verification_uri"),
            intervalSeconds = json.optInt("interval", 5)
        )
    }

    fun pollForMsToken(deviceCode: String): String? {
        val body = "client_id=$clientId&device_code=$deviceCode&grant_type=urn:ietf:params:oauth:grant-type:device_code"
        val json = post("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", body)
        return json.optString("access_token", null).takeUnless { it.isNullOrBlank() }
    }

    /** Full chain: MS token -> Xbox Live token -> XSTS token -> Minecraft profile. */
    fun completeMinecraftLogin(msAccessToken: String): AccountProfile {
        val xblToken = xboxLiveAuth(msAccessToken)
        val xstsResult = xstsAuth(xblToken)
        val mcAccessToken = minecraftAuth(xstsResult.first, xstsResult.second)
        val profile = fetchMinecraftProfile(mcAccessToken)
        return profile.copy(accessToken = mcAccessToken)
    }

    private fun xboxLiveAuth(msAccessToken: String): String {
        val payload = JSONObject().apply {
            put("Properties", JSONObject().apply {
                put("AuthMethod", "RPS")
                put("SiteName", "user.auth.xboxlive.com")
                put("RpsTicket", "d=$msAccessToken")
            })
            put("RelyingParty", "http://auth.xboxlive.com")
            put("TokenType", "JWT")
        }
        val json = postJson("https://user.auth.xboxlive.com/user/authenticate", payload)
        return json.getString("Token")
    }

    /** Returns (xstsToken, userHash) */
    private fun xstsAuth(xblToken: String): Pair<String, String> {
        val payload = JSONObject().apply {
            put("Properties", JSONObject().apply {
                put("SandboxId", "RETAIL")
                put("UserTokens", org.json.JSONArray().put(xblToken))
            })
            put("RelyingParty", "rp://api.minecraftservices.com/")
            put("TokenType", "JWT")
        }
        val json = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", payload)
        val userHash = json.getJSONObject("DisplayClaims")
            .getJSONArray("xui").getJSONObject(0).getString("uhs")
        return json.getString("Token") to userHash
    }

    private fun minecraftAuth(xstsToken: String, userHash: String): String {
        val payload = JSONObject().apply {
            put("identityToken", "XBL3.0 x=$userHash;$xstsToken")
        }
        val json = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", payload)
        return json.getString("access_token")
    }

    private fun fetchMinecraftProfile(mcAccessToken: String): AccountProfile {
        val conn = URL("https://api.minecraftservices.com/minecraft/profile").openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $mcAccessToken")
        conn.inputStream.bufferedReader().use {
            val json = JSONObject(it.readText())
            val skins = json.optJSONArray("skins")
            val skinUrl = skins?.let { arr ->
                if (arr.length() > 0) arr.getJSONObject(0).optString("url", null) else null
            }
            return AccountProfile(
                playerName = json.getString("name"),
                uuid = json.getString("id"),
                accessToken = "", // filled by caller
                authType = AuthType.MICROSOFT,
                skinUrl = skinUrl
            )
        }
    }

    private fun post(urlStr: String, body: String): JSONObject {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.outputStream.use { it.write(body.toByteArray()) }
        conn.inputStream.bufferedReader().use { return JSONObject(it.readText()) }
    }

    private fun postJson(urlStr: String, payload: JSONObject): JSONObject {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.outputStream.use { it.write(payload.toString().toByteArray()) }
        conn.inputStream.bufferedReader().use { return JSONObject(it.readText()) }
    }
}
