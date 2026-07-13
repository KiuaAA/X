package com.x.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x.launcher.state.LaunchPhase
import com.x.launcher.state.LaunchViewModel
import com.x.launcher.ui.theme.XCardBlack
import com.x.launcher.ui.theme.XCream
import com.x.launcher.ui.theme.XTextOnCream

@Composable
fun LaunchScreen(
    viewModel: LaunchViewModel,
    onBack: () -> Unit
) {
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
            Text("Launch", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }

        Spacer(Modifier.height(20.dp))

        when (viewModel.phase) {
            LaunchPhase.IDLE -> {
                Text("Not launched yet", color = Color.LightGray)
            }

            LaunchPhase.LAUNCHING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = XCream, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(viewModel.statusMessage, color = Color.White)
                }
            }

            LaunchPhase.RUNNING -> {
                Text("Running", color = Color(0xFF4CD964), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LogBox(viewModel.visibleLog)
            }

            LaunchPhase.EXITED -> {
                Text("Game closed (exit code ${viewModel.exitCode})", color = Color.LightGray)
                Spacer(Modifier.height(12.dp))
                LogBox(viewModel.visibleLog)
            }

            LaunchPhase.CRASHED -> {
                CrashCard(
                    title = viewModel.crashReport?.title ?: "Crash",
                    reason = viewModel.crashReport?.reason ?: "",
                    solution = viewModel.crashReport?.solution ?: "",
                    onDismiss = { viewModel.dismissCrash() }
                )
                Spacer(Modifier.height(12.dp))
                LogBox(viewModel.crashReport?.rawExcerpt ?: "")
            }
        }
    }
}

@Composable
private fun CrashCard(title: String, reason: String, solution: String, onDismiss: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = XCream),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = XTextOnCream)
                Spacer(Modifier.width(8.dp))
                Text(title, color = XTextOnCream, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Reason", color = XTextOnCream, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(reason, color = XTextOnCream, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Text("Solution", color = XTextOnCream, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(solution, color = XTextOnCream, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = XCardBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("OK", color = Color.White)
            }
        }
    }
}

@Composable
private fun LogBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .background(XCardBlack, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text.ifBlank { "(no output yet)" },
            color = Color(0xFF9FE870),
            fontSize = 12.sp,
            modifier = Modifier.verticalScroll(rememberScrollState())
        )
    }
}
