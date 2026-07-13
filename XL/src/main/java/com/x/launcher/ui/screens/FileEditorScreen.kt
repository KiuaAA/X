package com.x.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.x.launcher.state.FileEditMode
import com.x.launcher.state.FileEditorViewModel
import com.x.launcher.ui.theme.XCardBlack
import com.x.launcher.ui.theme.XCream
import java.io.File

@Composable
fun FileEditorScreen(
    targetFile: File,
    onBack: () -> Unit,
    viewModel: FileEditorViewModel = viewModel()
) {
    LaunchedEffect(targetFile) { viewModel.open(targetFile) }

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
            Column(Modifier.weight(1f)) {
                Text(targetFile.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(targetFile.parentFile?.name ?: "", color = Color.LightGray, fontSize = 11.sp)
            }
            if (viewModel.mode == FileEditMode.TEXT) {
                IconButton(onClick = { viewModel.save() }) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Save",
                        tint = if (viewModel.isDirty) XCream else Color.DarkGray
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        viewModel.saveError?.let {
            Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (viewModel.saveSuccess) {
            Text("Saved", color = Color(0xFF4CD964), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        when {
            viewModel.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = XCream)
                }
            }

            viewModel.mode == FileEditMode.TEXT -> {
                OutlinedTextField(
                    value = viewModel.textContent,
                    onValueChange = { viewModel.updateText(it) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = XCardBlack,
                        unfocusedContainerColor = XCardBlack,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = XCream,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
            }

            viewModel.mode == FileEditMode.HEX -> {
                Column {
                    Text(
                        "Binary file — hex preview only (jar/zip files can't be edited as text)",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(XCardBlack)
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            viewModel.hexPreview,
                            color = Color(0xFF9FE870),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This file type can't be previewed", color = Color.LightGray)
                }
            }
        }
    }
}
