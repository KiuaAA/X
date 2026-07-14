package com.x.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.x.client.download.Renderer
import com.x.launcher.state.SettingsViewModel
import com.x.launcher.ui.theme.XCardBlack
import com.x.launcher.ui.theme.XCream
import java.io.File

@Composable
fun SettingsScreen(
    xRoot: File,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel { SettingsViewModel(xRoot) }
) {
    val settings = viewModel.settings
    var ramMaxSlider by remember(settings.ramMaxMb) { mutableStateOf(settings.ramMaxMb.toFloat()) }
    var ramMinSlider by remember(settings.ramMinMb) { mutableStateOf(settings.ramMinMb.toFloat()) }
    var extraArgsText by remember(settings.extraJvmArgs) { mutableStateOf(settings.extraJvmArgs) }

    val recommendedMax = remember { viewModel.deviceRecommendedMaxRamMb() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }

        Spacer(Modifier.height(20.dp))

        SectionCard(title = "Memory allocation") {
            Text(
                "Recommended max for this device: ~${recommendedMax} MB",
                color = Color.LightGray, fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))

            Text("Minimum: ${ramMinSlider.toInt()} MB", color = Color.White, fontSize = 13.sp)
            Slider(
                value = ramMinSlider,
                onValueChange = { ramMinSlider = it },
                onValueChangeFinished = {
                    viewModel.updateRam(ramMinSlider.toInt(), ramMaxSlider.toInt())
                },
                valueRange = 512f..4096f,
                colors = SliderDefaults.colors(thumbColor = XCream, activeTrackColor = XCream)
            )

            Spacer(Modifier.height(8.dp))

            Text("Maximum: ${ramMaxSlider.toInt()} MB", color = Color.White, fontSize = 13.sp)
            Slider(
                value = ramMaxSlider,
                onValueChange = { ramMaxSlider = it },
                onValueChangeFinished = {
                    viewModel.updateRam(ramMinSlider.toInt(), ramMaxSlider.toInt())
                },
                valueRange = 1024f..8192f,
                colors = SliderDefaults.colors(thumbColor = XCream, activeTrackColor = XCream)
            )
        }

        Spacer(Modifier.height(16.dp))

        SectionCard(title = "Renderer") {
            Text(
                "Switch this if the game fails to start or graphics look wrong",
                color = Color.LightGray, fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Renderer.values().forEach { renderer ->
                RendererRow(
                    renderer = renderer,
                    selected = settings.renderer == renderer,
                    onSelect = { viewModel.updateRenderer(renderer) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionCard(title = "Java runtime") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(8, 17, 21).forEach { major ->
                    JavaChip(
                        label = "Java $major",
                        selected = settings.javaMajor == major,
                        onClick = { viewModel.updateJavaMajor(major) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Older versions need Java 8, modern ones need 17 or 21 — check the version's install notes",
                color = Color.LightGray, fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        SectionCard(title = "Advanced: extra JVM arguments") {
            OutlinedTextField(
                value = extraArgsText,
                onValueChange = {
                    extraArgsText = it
                    viewModel.updateExtraJvmArgs(it)
                },
                placeholder = { Text("e.g. -XX:+UseG1GC") },
                singleLine = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = XCream,
                    unfocusedBorderColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(40.dp))
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
private fun RendererRow(renderer: Renderer, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) XCream else Color(0xFF1E1E1E))
            .then(Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = if (selected) Color.Black else XCream)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                renderer.name.replace("_", " "),
                color = if (selected) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                renderer.description,
                color = if (selected) Color(0xFF333333) else Color.LightGray,
                fontSize = 11.sp
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun JavaChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) XCream else Color(0xFF1E1E1E),
            labelColor = if (selected) Color.Black else Color.White
        )
    )
}
