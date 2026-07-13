package com.x.client.account

enum class AuthType { MICROSOFT, OFFLINE }

data class AccountProfile(
    val playerName: String,
    val uuid: String,
    val accessToken: String,      // real MC session token, or "0" for offline
    val authType: AuthType,
    val skinUrl: String? = null,
    val capeUrl: String? = null,
    val githubUsername: String? = null   // launcher identity, unrelated to MC auth
)
