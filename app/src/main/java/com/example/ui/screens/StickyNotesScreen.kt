package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaType
import com.example.data.model.QuickTemplateEntity
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TemplateTargetKind
import com.example.ui.components.ChecklistInteractiveSection
import com.example.ui.components.MediaPreviewCard
import com.example.ui.components.QuickTemplateStrip
import com.example.ui.theme.stickyBorderColor
import com.example.ui.theme.stickySurfaceColor

@Composable
fun StickyNotesScreen(
    notes: List<StickyNoteEntity>,
    templates: List<QuickTemplateEntity> = emptyList(),
    canOverlay: Boolean,
    overlayMode: com.example.data.local.OverlayDisplayMode,
    isTwoPane: Boolean,
    onOverlayModeChange: (com.example.data.local.OverlayDisplayMode) -> Unit,
    onAddNote: () -> Unit,
    onApplyTemplate: (QuickTemplateEntity) -> Unit = {},
    onDeleteTemplate: (Long) -> Unit = {},
    onToggleChecklistItem: (Long, String) -> Unit = { _, _ -> },
    onEditNote: (StickyNoteEntity) -> Unit,
    onTogglePinOverlay: (StickyNoteEntity) -> Unit,
    onUnpinAll: () -> Unit,
    onDeleteNote: (StickyNoteEntity) -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val pinnedCount = notes.count { it.isPinnedOverlay }
    val noteTemplates = remember(templates) {
        templates.filter { it.targetKind == TemplateTargetKind.NOTE }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTwoPane) 24.dp else 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sticky Notes Board", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Pin any note to float over other apps on screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onAddNote,
                modifier = Modifier.height(48.dp).testTag("add_sticky_note_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New Note")
            }
        }

        // Toggle option for Full Overlay or Transparent Overlay screen
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Overlay Screen Style", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (overlayMode == com.example.data.local.OverlayDisplayMode.FULL) {
                            "Full Overlay: full-screen solid focus card"
                        } else {
                            "Transparent Overlay: see-through floating card"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = overlayMode == com.example.data.local.OverlayDisplayMode.TRANSPARENT,
                        onClick = { onOverlayModeChange(com.example.data.local.OverlayDisplayMode.TRANSPARENT) },
                        label = { Text("Transparent") },
                        modifier = Modifier.testTag("notes_overlay_transparent_chip")
                    )
                    androidx.compose.material3.FilterChip(
                        selected = overlayMode == com.example.data.local.OverlayDisplayMode.FULL,
                        onClick = { onOverlayModeChange(com.example.data.local.OverlayDisplayMode.FULL) },
                        label = { Text("Full") },
                        modifier = Modifier.testTag("notes_overlay_full_chip")
                    )
                }
            }
        }

        if (!canOverlay) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Allow Overlay once to float sticky notes over other apps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    FilledTonalButton(onClick = onRequestOverlayPermission) {
                        Text("Allow")
                    }
                }
            }
        } else if (pinnedCount > 0) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$pinnedCount sticky note(s) pinned on screen",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    TextButton(onClick = onUnpinAll) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Hide All")
                    }
                }
            }
        }

        QuickTemplateStrip(
            title = "Quick Note Templates",
            templates = noteTemplates,
            onApplyTemplate = onApplyTemplate,
            onDeleteCustomTemplate = { tpl -> onDeleteTemplate(tpl.id) }
        )

        if (notes.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.StickyNote2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("No sticky notes yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Create quick visual reminders and pin them directly onto your tablet screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = onAddNote) {
                        Text("Write First Sticky Note")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isTwoPane) 260.dp else 210.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notes, key = { it.id }) { note ->
                    StickyNoteGridCard(
                        note = note,
                        canOverlay = canOverlay,
                        onEdit = { onEditNote(note) },
                        onToggleChecklistItem = { itemId -> onToggleChecklistItem(note.id, itemId) },
                        onTogglePin = {
                            if (canOverlay) onTogglePinOverlay(note) else onRequestOverlayPermission()
                        },
                        onDelete = { onDeleteNote(note) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyNoteGridCard(
    note: StickyNoteEntity,
    canOverlay: Boolean,
    onEdit: () -> Unit,
    onToggleChecklistItem: (String) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = stickySurfaceColor(note.color.name)
    val border = stickyBorderColor(note.color.name)

    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (note.isPinnedOverlay) 2.5.dp else 1.5.dp,
                color = if (note.isPinnedOverlay) MaterialTheme.colorScheme.primary else border,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onEdit)
            .testTag("sticky_card_${note.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Sticky Note" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("sticky_pin_btn_${note.id}")
                ) {
                    Icon(
                        imageVector = if (note.isPinnedOverlay) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (note.isPinnedOverlay) "Unpin sticky note" else "Show sticky note on screen",
                        tint = if (note.isPinnedOverlay) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!note.imageUri.isNullOrBlank()) {
                MediaPreviewCard(
                    mediaType = MediaType.IMAGE,
                    mediaUri = note.imageUri,
                    mediaDisplayName = note.imageDisplayName,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!note.musicUri.isNullOrBlank()) {
                MediaPreviewCard(
                    mediaType = MediaType.MUSIC,
                    mediaUri = note.musicUri,
                    mediaDisplayName = note.musicDisplayName,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = note.content.ifBlank { "Tap to add note details..." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                minLines = if (note.checklistItems().isEmpty() && !note.hasAnyMedia()) 3 else 1,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            val noteChecklist = remember(note.checklistJson) { note.checklistItems() }
            if (noteChecklist.isNotEmpty()) {
                ChecklistInteractiveSection(
                    items = noteChecklist,
                    compact = true,
                    maxVisible = 4,
                    onToggleItem = onToggleChecklistItem
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onTogglePin,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        when {
                            note.isPinnedOverlay -> "Pinned on Screen"
                            canOverlay -> "Show on Screen"
                            else -> "Allow Overlay"
                        }
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit sticky note")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete sticky note")
                    }
                }
            }
        }
    }
}
