package com.x.launcher.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.x.client.account.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class MsLoginState { IDLE, WAITING_FOR_USER, SUCCESS, ERROR }

class AccountViewModel(private val xRoot: File) : ViewModel() {

    private val msAuth = MicrosoftAuthManager(clientId = "YOUR_AZURE_CLIENT_ID")
    private val ghAuth = GitHubAccountManager(clientId = "YOUR_GITHUB_CLIENT_ID")
    private val offlineManager = OfflineProfileManager(xRoot)

    var activeProfile by mutableStateOf<AccountProfile?>(null)
        private set

    var githubUsername by mutableStateOf<String?>(null)
        private set

    var msLoginState by mutableStateOf(MsLoginState.IDLE)
        private set
    var msUserCode by mutableStateOf<String?>(null)
        private set
    var msVerificationUri by mutableStateOf<String?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var offlineProfiles by mutableStateOf<List<OfflineProfileManager.OfflineProfile>>(emptyList())
        private set

    fun loadOfflineProfiles() {
        offlineProfiles = offlineManager.listProfiles()
    }

    fun startMicrosoftLogin() {
        errorMessage = null
        msLoginState = MsLoginState.WAITING_FOR_USER
        viewModelScope.launch {
            try {
                val device = withContext(Dispatchers.IO) { msAuth.requestDeviceCode() }
                msUserCode = device.userCode
                msVerificationUri = device.verificationUri

                var msToken: String? = null
                val deadline = System.currentTimeMillis() + 15 * 60 * 1000
                while (msToken == null && System.currentTimeMillis() < deadline) {
                    delay(device.intervalSeconds * 1000L)
                    msToken = withContext(Dispatchers.IO) { msAuth.pollForMsToken(device.deviceCode) }
                }

                if (msToken == null) {
                    msLoginState = MsLoginState.ERROR
                    errorMessage = "Login timed out — try again."
                    return@launch
                }

                val profile = withContext(Dispatchers.IO) { msAuth.completeMinecraftLogin(msToken) }
                activeProfile = profile
                msLoginState = MsLoginState.SUCCESS
            } catch (e: Exception) {
                msLoginState = MsLoginState.ERROR
                errorMessage = "Login failed: ${e.message ?: "unknown error"}. " +
                    "If you don't own Minecraft Java Edition on this Microsoft account, " +
                    "that's why — a real purchased account is required for online-mode servers."
            }
        }
    }

    fun cancelMicrosoftLogin() {
        msLoginState = MsLoginState.IDLE
        msUserCode = null
    }

    fun connectGitHub(onCodeReady: (userCode: String, uri: String) -> Unit) {
        viewModelScope.launch {
            try {
                val device = withContext(Dispatchers.IO) { ghAuth.requestDeviceCode() }
                onCodeReady(device.userCode, device.verificationUri)

                var token: String? = null
                val deadline = System.currentTimeMillis() + device.expiresInSeconds * 1000L
                while (token == null && System.currentTimeMillis() < deadline) {
                    delay(device.intervalSeconds * 1000L)
                    token = withContext(Dispatchers.IO) { ghAuth.pollForToken(device.deviceCode) }
                }

                token?.let {
                    githubUsername = withContext(Dispatchers.IO) { ghAuth.fetchUsername(it) }
                }
            } catch (e: Exception) {
                errorMessage = "GitHub connect failed: ${e.message}"
            }
        }
    }

    fun createOfflineProfile(name: String) {
        viewModelScope.launch {
            val profile = withContext(Dispatchers.IO) { offlineManager.createOrGetProfile(name) }
            loadOfflineProfiles()
            activeProfile = offlineManager.toAccountProfile(profile)
        }
    }

    fun useOfflineProfile(profile: OfflineProfileManager.OfflineProfile) {
        activeProfile = offlineManager.toAccountProfile(profile)
    }

    fun deleteOfflineProfile(name: String) {
        offlineManager.deleteProfile(name)
        loadOfflineProfiles()
    }

    fun setOfflineSkin(name: String, skinPath: String) {
        offlineManager.setLocalSkin(name, skinPath)
        loadOfflineProfiles()
    }
}
