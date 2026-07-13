package com.x.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.x.launcher.state.VersionFilter
import com.x.launcher.state.VersionListItem
import com.x.launcher.state.VersionListViewModel
import com.x.launcher.ui.theme.XCardBlack
import com.x.launcher.ui.theme.XCream
import com.x.launcher.ui.theme.XTextOnCream

@Composable
fun VersionPickerScreen(
    onBack: () -> Unit,
    onVersionSelected: (VersionListItem) -> Unit,
    viewModel: VersionListViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        if (viewModel.allVersions.isEmpty()) viewModel.loadVersions()
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
            Text("Select version", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }

        Spacer(Modifier.height(16.dp))

        // Search bar
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            placeholder = { Text("Search versions") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = XCardBlack,
                unfocusedContainerColor = XCardBlack,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = XCream,
                unfocusedBorderColor = Color.DarkGray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChipItem("Release", viewModel.filter == VersionFilter.RELEASE) { viewModel.filter = VersionFilter.RELEASE }
            FilterChipItem("Snapshot", viewModel.filter == VersionFilter.SNAPSHOT) { viewModel.filter = VersionFilter.SNAPSHOT }
            FilterChipItem("Old", viewModel.filter == VersionFilter.OLD) { viewModel.filter = VersionFilter.OLD }
            FilterChipItem("All", viewModel.filter == VersionFilter.ALL) { viewModel.filter = VersionFilter.ALL }
        }

        Spacer(Modifier.height(16.dp))

        when {
            viewModel.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = XCream)
                }
            }
            viewModel.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(viewModel.error ?: "", color = Color.LightGray)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadVersions() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(viewModel.filteredVersions()) { version ->
                        VersionRow(version = version, onClick = { onVersionSelected(version) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) XCream else XCardBlack,
            labelColor = if (selected) XTextOnCream else Color.White
        )
    )
}

@Composable
private fun VersionRow(version: VersionListItem, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(XCardBlack)
            .clickable(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(version.id, color = Color.White, fontWeight = FontWeight.Bold)
            Text(version.type, color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
