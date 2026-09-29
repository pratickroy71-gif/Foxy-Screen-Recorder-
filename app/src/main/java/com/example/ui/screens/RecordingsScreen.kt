package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StorageHelper
import com.example.model.RecordingEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.FoxyAccentCyan
import com.example.ui.theme.FoxyAccentPink
import com.example.ui.theme.FoxyBackground
import com.example.ui.theme.FoxyCardElevated
import com.example.ui.theme.FoxyPrimary
import com.example.ui.theme.FoxyPrimaryGlow
import com.example.ui.theme.FoxyRecordRed
import com.example.ui.theme.FoxySurface
import com.example.ui.theme.FoxySurfaceVariant
import com.example.ui.theme.FoxyTextPrimary
import com.example.ui.theme.FoxyTextSecondary
import com.example.ui.theme.FoxyTextTertiary
import com.example.util.TimeUtils

enum class RecordingFilter {
    ALL,
    FAVORITES,
    RECENT
}

@Composable
fun RecordingsScreen(
    recordings: List<RecordingEntity>,
    onPlayRecording: (RecordingEntity) -> Unit,
    onShareRecording: (RecordingEntity) -> Unit,
    onDeleteRecording: (RecordingEntity) -> Unit,
    onRenameRecording: (RecordingEntity, String) -> Unit,
    onToggleFavorite: (RecordingEntity) -> Unit,
    onTrimRecording: (RecordingEntity) -> Unit,
    onCompressRecording: (RecordingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(RecordingFilter.ALL) }
    var isGridView by remember { mutableStateOf(false) }

    var recordingToRename by remember { mutableStateOf<RecordingEntity?>(null) }
    var newRenameTitle by remember { mutableStateOf("") }

    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }

    val filteredList = recordings.filter { rec ->
        val matchesQuery = rec.title.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            RecordingFilter.ALL -> true
            RecordingFilter.FAVORITES -> rec.isFavorite
            RecordingFilter.RECENT -> {
                val oneDayMs = 24 * 60 * 60 * 1000L
                (System.currentTimeMillis() - rec.timestamp) <= oneDayMs
            }
        }
        matchesQuery && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FoxyBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recordings",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = FoxyTextPrimary
                )
                Text(
                    text = "${filteredList.size} items available",
                    style = MaterialTheme.typography.bodySmall,
                    color = FoxyTextTertiary
                )
            }

            IconButton(onClick = { isGridView = !isGridView }) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle Grid/List",
                    tint = FoxyPrimaryGlow
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search recordings...", color = FoxyTextTertiary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = FoxyTextSecondary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = FoxyTextSecondary)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_recordings_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FoxySurfaceVariant,
                unfocusedContainerColor = FoxySurfaceVariant,
                focusedBorderColor = FoxyPrimary,
                unfocusedBorderColor = Color(0x338C52FF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecordingFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = when (filter) {
                                RecordingFilter.ALL -> "All (${recordings.size})"
                                RecordingFilter.FAVORITES -> "Favorites (${recordings.count { it.isFavorite }})"
                                RecordingFilter.RECENT -> "Last 24h"
                            }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FoxyPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = FoxySurfaceVariant,
                        labelColor = FoxyTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0x338C52FF),
                        selectedBorderColor = FoxyPrimaryGlow
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = FoxyTextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matches found" else "No recordings found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = FoxyTextSecondary
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try changing your search term" else "Record your screen from Home tab",
                        style = MaterialTheme.typography.bodySmall,
                        color = FoxyTextTertiary
                    )
                }
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredList, key = { it.id }) { rec ->
                    RecordingGridCard(
                        recording = rec,
                        onPlay = { onPlayRecording(rec) },
                        onShare = { onShareRecording(rec) },
                        onTrim = { onTrimRecording(rec) },
                        onCompress = { onCompressRecording(rec) },
                        onToggleFavorite = { onToggleFavorite(rec) },
                        onRename = {
                            recordingToRename = rec
                            newRenameTitle = rec.title
                        },
                        onDelete = { recordingToDelete = rec }
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredList, key = { it.id }) { rec ->
                    RecordingListCard(
                        recording = rec,
                        onPlay = { onPlayRecording(rec) },
                        onShare = { onShareRecording(rec) },
                        onTrim = { onTrimRecording(rec) },
                        onCompress = { onCompressRecording(rec) },
                        onToggleFavorite = { onToggleFavorite(rec) },
                        onRename = {
                            recordingToRename = rec
                            newRenameTitle = rec.title
                        },
                        onDelete = { recordingToDelete = rec }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Rename Dialog
    if (recordingToRename != null) {
        AlertDialog(
            onDismissRequest = { recordingToRename = null },
            title = { Text("Rename Recording", color = FoxyTextPrimary) },
            text = {
                OutlinedTextField(
                    value = newRenameTitle,
                    onValueChange = { newRenameTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = recordingToRename
                        if (target != null && newRenameTitle.isNotBlank()) {
                            onRenameRecording(target, newRenameTitle.trim())
                        }
                        recordingToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FoxyPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToRename = null }) {
                    Text("Cancel", color = FoxyTextSecondary)
                }
            },
            containerColor = FoxySurface
        )
    }

    // Delete Confirmation Dialog
    if (recordingToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Delete Recording?", color = FoxyTextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete '${recordingToDelete?.title}'? This action cannot be undone.",
                    color = FoxyTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        recordingToDelete?.let { onDeleteRecording(it) }
                        recordingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FoxyRecordRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text("Cancel", color = FoxyTextSecondary)
                }
            },
            containerColor = FoxySurface
        )
    }
}

@Composable
private fun RecordingListCard(
    recording: RecordingEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onTrim: () -> Unit,
    onCompress: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(18.dp),
        color = FoxySurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Icon box
            Box(
                modifier = Modifier
                    .size(72.dp, 52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF432874), Color(0xFF1F173B))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = TimeUtils.formatDuration(recording.durationMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FoxyTextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${recording.resolution} • ${recording.fps} FPS",
                    style = MaterialTheme.typography.bodySmall,
                    color = FoxyAccentCyan
                )
                Text(
                    text = "${StorageHelper.formatBytes(recording.fileSizeBytes)} • ${TimeUtils.formatDate(recording.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FoxyTextTertiary
                )
            }

            // Favorite Button
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (recording.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (recording.isFavorite) FoxyAccentPink else FoxyTextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // More Options Dropdown
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = FoxyTextSecondary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(FoxyCardElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = FoxyPrimaryGlow) },
                        onClick = {
                            showMenu = false
                            onPlay()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Trim Video", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.ContentCut, null, tint = FoxyAccentCyan) },
                        onClick = {
                            showMenu = false
                            onTrim()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Compress & Export", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Compress, null, tint = FoxyPrimaryGlow) },
                        onClick = {
                            showMenu = false
                            onCompress()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = FoxyRecordRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = FoxyRecordRed) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingGridCard(
    recording: RecordingEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onTrim: () -> Unit,
    onCompress: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(16.dp),
        color = FoxySurfaceVariant
    ) {
        Column {
            // Header Image Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF381E72), Color(0xFF1D1736))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )

                // Top right favorite icon
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (recording.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (recording.isFavorite) FoxyAccentPink else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = TimeUtils.formatDuration(recording.durationMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            // Info
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FoxyTextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${recording.resolution} • ${StorageHelper.formatBytes(recording.fileSizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FoxyTextTertiary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onCompress, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Compress, null, tint = FoxyPrimaryGlow, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, null, tint = FoxyTextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onTrim, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCut, null, tint = FoxyAccentCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
