package com.x.launcher.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.x.client.mods.ModInfo
import com.x.launcher.state.ModManagerViewModel
import com.x.launcher.ui.theme.XCardBlack
import com.x.launcher.ui.theme.XCream
import java.io.File

@Composable
fun ModManagerScreen(
    versionModsDir: File,
    onBack: () -> Unit,
    onEditMod: (File) -> Unit
) {
    val context = LocalContext.current
    val viewModel: ModManagerViewModel = viewModel { ModManagerViewModel(versionModsDir) }

    var pendingImportFile by remember { mutableStateOf<File?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.jar")
            context.contentResolver.openInputStream(it)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            pendingImportFile = tempFile
        }
    }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(pendingImportFile) {
        pendingImportFile?.let {
            viewModel.addMod(it)
            pendingImportFile = null
        }
    }

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
            Text("Mods", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { filePicker.launch("*/*") }) {
                Icon(Icons.Default.Add, contentDescription = "Add mod", tint = XCream)
            }
        }

        Spacer(Modifier.height(16.dp))

        viewModel.lastError?.let { err ->
            Text(err, color = Color(0xFFFF6B6B), modifier = Modifier.padding(bottom = 12.dp))
        }

        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = XCream)
            }
        } else if (viewModel.mods.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No mods yet — tap + to add one", color = Color.LightGray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(viewModel.mods) { mod ->
                    ModRow(
                        mod = mod,
                        onToggle = { viewModel.toggle(mod) },
                        onRemove = { viewModel.remove(mod) },
                        onEdit = { onEditMod(viewModel.getModFile(mod)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModRow(
    mod: ModInfo,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(XCardBlack)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (mod.enabled) Color(0xFF4CD964) else Color(0xFF666666))
        )
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(mod.displayName, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                "${mod.loader}${mod.version?.let { " · $it" } ?: ""}",
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }

        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit files", tint = Color.White)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFFF6B6B))
        }
        Switch(
            checked = mod.enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = XCream, checkedTrackColor = Color(0xFF5A5637))
        )
    }
}
