package com.x.launcher.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.x.client.account.OfflineProfileManager
import com.x.launcher.state.AccountViewModel
import com.x.launcher.state.MsLoginState
import com.x.launcher.ui.components.PlayerHead
import com.x.launcher.ui.theme.XCardBlack
import com.x.launcher.ui.theme.XCream
import com.x.launcher.ui.theme.XTextOnCream
import java.io.File

@Composable
fun AccountScreen(
    xRoot: File,
    onBack: () -> Unit,
    viewModel: AccountViewModel = viewModel { AccountViewModel(xRoot) }
) {
    val context = LocalContext.current

    var githubCode by remember { mutableStateOf<String?>(null) }
    var githubUri by remember { mutableStateOf<String?>(null) }
    var newProfileName by remember { mutableStateOf("") }
    var selectedProfileForSkin by remember { mutableStateOf<String?>(null) }

    val skinPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { pickedUri ->
            val name = selectedProfileForSkin ?: return@let
            val destFile = File(context.filesDir, "skins/$name.png")
            destFile.parentFile?.mkdirs()
            context.contentResolver.openInputStream(pickedUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            viewModel.setOfflineSkin(name, destFile.absolutePath)
        }
    }

    LaunchedEffect(Unit) { viewModel.loadOfflineProfiles() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }

        Spacer(Modifier.height(20.dp))

        viewModel.activeProfile?.let { profile ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = XCream),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    PlayerHead(uuid = profile.uuid, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(profile.playerName, color = XTextOnCream, fontWeight = FontWeight.Bold)
                        Text(
                            if (profile.authType == com.x.client.account.AuthType.MICROSOFT)
                                "Online — real Minecraft account" else "Offline — local only",
                            color = XTextOnCream,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        } ?: run {
            Text(
                "No account selected yet — sign in or pick an offline profile below.",
                color = Color.LightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        SectionCard(title = "Launcher identity") {
            if (viewModel.githubUsername != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("GitHub: ${viewModel.githubUsername}", color = Color.White)
                }
            } else if (githubCode != null) {
                Text("Enter this code at $githubUri:", color = Color.LightGray, fontSize = 13.sp)
                Text(githubCode ?: "", color = XCream, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            } else {
                XButton("Connect GitHub") {
                    viewModel.connectGitHub { code, uri ->
                        githubCode = code
                        githubUri = uri
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionCard(title = "Play on real servers (online-mode)") {
            when (viewModel.msLoginState) {
                MsLoginState.IDLE -> {
                    Text(
                        "Needs a real Microsoft account that owns Minecraft Java Edition.",
                        color = Color.LightGray, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    XButton("Sign in with Microsoft") { viewModel.startMicrosoftLogin() }
                }
                MsLoginState.WAITING_FOR_USER -> {
                    if (viewModel.msUserCode != null) {
                        Text("Go to ${viewModel.msVerificationUri} and enter:", color = Color.LightGray, fontSize = 13.sp)
                        Text(viewModel.msUserCode ?: "", color = XCream, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.cancelMicrosoftLogin() }) {
                            Text("Cancel", color = Color.LightGray)
                        }
                    } else {
                        CircularProgressIndicator(color = XCream, modifier = Modifier.size(20.dp))
                    }
                }
                MsLoginState.SUCCESS -> {
                    Text("Signed in", color = Color(0xFF4CD964), fontWeight = FontWeight.Bold)
                }
                MsLoginState.ERROR -> {
                    Text(viewModel.errorMessage ?: "Login failed", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    XButton("Try again") { viewModel.startMicrosoftLogin() }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionCard(title = "Offline profiles (LAN / cracked-friendly servers)") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    placeholder = { Text("New profile name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    if (newProfileName.isNotBlank()) {
                        viewModel.createOfflineProfile(newProfileName)
                        newProfileName = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add profile", tint = XCream)
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.offlineProfiles) { profile ->
                    OfflineProfileChip(
                        profile = profile,
                        onUse = { viewModel.useOfflineProfile(profile) },
                        onChangeSkin = {
                            selectedProfileForSkin = profile.name
                            skinPicker.launch("image/png")
                        },
                        onDelete = { viewModel.deleteOfflineProfile(profile.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(XCardBlack)
            .padding(16.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun XButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = XCream)
    ) {
        Text(label, color = XTextOnCream, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OfflineProfileChip(
    profile: OfflineProfileManager.OfflineProfile,
    onUse: () -> Unit,
    onChangeSkin: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E1E1E))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A))
        )
        Spacer(Modifier.height(6.dp))
        Text(profile.name, color = Color.White, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row {
            IconButton(onClick = onUse, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Check, contentDescription = "Use", tint = Color(0xFF4CD964))
            }
            IconButton(onClick = onChangeSkin, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Image, contentDescription = "Skin", tint = XCream)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF6B6B))
            }
        }
    }
}
