package com.x.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x.launcher.ui.components.PlayerHead
import com.x.launcher.ui.components.XLogo
import com.x.launcher.ui.theme.XCardBlack
import com.x.launcher.ui.theme.XCream
import com.x.launcher.ui.theme.XTextOnCream

data class HomeUiState(
    val playerName: String = "Guest",
    val playerUuid: String = "00000000-0000-0000-0000-000000000000",
    val githubLinked: Boolean = false,
    val selectedVersionLabel: String = "Select a version",
    val selectedVersionNumber: String = "--",
    val hasAccount: Boolean = false,
    val hasVersion: Boolean = false
)

@Composable
fun HomeScreen(
    state: HomeUiState = HomeUiState(),
    onEditProfile: () -> Unit = {},
    onOpenVersionPicker: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenFiles: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onPlay: () -> Unit = {}
) {
    var blockedMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(28.dp))
                .background(XCream)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            PlayerHead(uuid = state.playerUuid, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(state.playerName, color = XTextOnCream, fontWeight = FontWeight.Bold)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            IconButton(onClick = onOpenHistory) {
                Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
            }
            IconButton(onClick = onOpenFiles) {
                Icon(Icons.Default.Folder, contentDescription = "Files", tint = Color.White)
            }
            IconButton(onClick = onOpenDownloads) {
                Icon(Icons.Default.Download, contentDescription = "Downloads", tint = Color.White)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Tune, contentDescription = "Settings", tint = Color.White)
            }
        }

        XLogo(modifier = Modifier.align(Alignment.Center))

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = XCream),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(340.dp)
                .fillMaxHeight(0.82f)
                .padding(top = 60.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlayerHead(uuid = state.playerUuid, size = 96.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    state.playerName.uppercase(),
                    color = XTextOnCream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = XTextOnCream,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.githubLinked) "GITHUB account" else "Not linked",
                        color = XTextOnCream,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onEditProfile,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = XCardBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile", color = Color.White)
                }

                Spacer(Modifier.weight(1f))

                blockedMessage?.let {
                    Text(it, color = Color(0xFFB00020), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(XCardBlack)
                        .clickableRow(onOpenVersionPicker)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    PlayerHead(uuid = state.playerUuid, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(state.selectedVersionNumber, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(state.selectedVersionLabel, color = Color.LightGray, fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Version", color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        blockedMessage = when {
                            !state.hasAccount -> "Tap Edit Profile to sign in or pick an offline profile first"
                            !state.hasVersion -> "Tap Version to pick a Minecraft version first"
                            else -> null
                        }
                        if (blockedMessage == null) onPlay()
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = XCardBlack),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
