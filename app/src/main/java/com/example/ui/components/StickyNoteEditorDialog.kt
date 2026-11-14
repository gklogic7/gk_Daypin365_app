package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChecklistCodec
import com.example.data.model.MediaType
import com.example.data.model.QuickTemplateEntity
import com.example.data.model.StickyColor
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TemplateTargetKind
import com.example.data.repository.CachedMediaResult
import com.example.ui.theme.stickyBorderColor
import com.example.ui.theme.stickySurfaceColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StickyNoteEditorDialog(
    initialNote: StickyNoteEntity?,
    canOverlay: Boolean,
    initialTemplate: QuickTemplateEntity? = null,
    templates: List<QuickTemplateEntity> = emptyList(),
    onSaveAsTemplate: (QuickTemplateEntity) -> Unit = {},
    onDeleteTemplate: (Long) -> Unit = {},
    onPersistMedia: suspend (Uri, String) -> CachedMediaResult = { uri, _ ->
        CachedMediaResult(uri.toString(), uri.lastPathSegment ?: "media")
    },
    onRequestOverlayPermission: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (StickyNoteEntity, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember(initialNote, initialTemplate) {
        mutableStateOf(initialNote?.title ?: initialTemplate?.defaultTitle ?: "")
    }
    var content by remember(initialNote, initialTemplate) {
        mutableStateOf(initialNote?.content ?: initialTemplate?.defaultBody ?: "")
    }
    var checklistItems by remember(initialNote, initialTemplate) {
        mutableStateOf(initialNote?.checklistItems() ?: initialTemplate?.checklistItems() ?: emptyList())
    }
    var color by remember(initialNote, initialTemplate) {
        mutableStateOf(initialNote?.color ?: initialTemplate?.stickyColor ?: StickyColor.BUTTERCUP)
    }
    var musicUri by remember(initialNote) { mutableStateOf(initialNote?.musicUri) }
    var musicName by remember(initialNote) { mutableStateOf(initialNote?.musicDisplayName) }
    var imageUri by remember(initialNote) { mutableStateOf(initialNote?.imageUri) }
    var imageName by remember(initialNote) { mutableStateOf(initialNote?.imageDisplayName) }
    var videoUri by remember(initialNote) { mutableStateOf(initialNote?.videoUri) }
    var videoName by remember(initialNote) { mutableStateOf(initialNote?.videoDisplayName) }
    var pinOnScreen by remember(initialNote) { mutableStateOf(initialNote?.isPinnedOverlay ?: false) }
    var templateSavedNotice by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val cached = onPersistMedia(uri, "note_img")
                imageUri = cached.uriString
                imageName = cached.displayName
            }
        }
    }

    val gifWebpPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val cached = onPersistMedia(uri, "note_anim")
                imageUri = cached.uriString
                imageName = cached.displayName
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val cached = onPersistMedia(uri, "note_vid")
                videoUri = cached.uriString
                videoName = cached.displayName
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val cached = onPersistMedia(uri, "note_aud")
                musicUri = cached.uriString
                musicName = cached.displayName
            }
        }
    }

    fun applyTemplate(tpl: QuickTemplateEntity) {
        title = tpl.defaultTitle
        content = tpl.defaultBody
        color = tpl.stickyColor
        checklistItems = tpl.checklistItems()
        templateSavedNotice = false
    }

    val canSave = title.isNotBlank() || content.isNotBlank() || checklistItems.isNotEmpty() ||
        !imageUri.isNullOrBlank() || !videoUri.isNullOrBlank() || !musicUri.isNullOrBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(18.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialNote == null) "New Sticky Note" else "Edit Sticky Note",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("sticky_cancel_button")
                        ) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            enabled = canSave,
                            onClick = {
                                val entity = (initialNote ?: StickyNoteEntity(title = "", content = "")).copy(
                                    title = title.trim().ifBlank { "Quick Note" },
                                    content = content.trim(),
                                    color = color,
                                    isPinnedOverlay = pinOnScreen && canOverlay,
                                    checklistJson = ChecklistCodec.encode(checklistItems),
                                    musicUri = musicUri,
                                    musicDisplayName = musicName,
                                    imageUri = imageUri,
                                    imageDisplayName = imageName,
                                    videoUri = videoUri,
                                    videoDisplayName = videoName
                                )
                                onSave(entity, pinOnScreen && canOverlay)
                            },
                            modifier = Modifier.testTag("sticky_save_button")
                        ) {
                            Text("Save Note")
                        }
                    }
                }
                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val noteTemplates = remember(templates) {
                        templates.filter { it.targetKind == TemplateTargetKind.NOTE }
                    }
                    QuickTemplateStrip(
                        title = "Quick Note Templates",
                        templates = noteTemplates,
                        onApplyTemplate = ::applyTemplate,
                        onDeleteCustomTemplate = { tpl -> onDeleteTemplate(tpl.id) }
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Note title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sticky_title_input")
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Note text") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sticky_content_input")
                    )

                    ChecklistEditorSection(
                        items = checklistItems,
                        onItemsChange = { checklistItems = it }
                    )

                    Text(
                        text = "Overlay Media (Split & Gradient Panels)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = !imageUri.isNullOrBlank(),
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(imageName?.take(14) ?: "Photo") },
                            modifier = Modifier.testTag("sticky_media_photo_chip")
                        )
                        FilterChip(
                            selected = false,
                            onClick = { gifWebpPickerLauncher.launch(arrayOf("image/gif", "image/webp")) },
                            leadingIcon = { Icon(Icons.Filled.Animation, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("GIF / WebP") },
                            modifier = Modifier.testTag("sticky_media_gif_chip")
                        )
                        FilterChip(
                            selected = !videoUri.isNullOrBlank(),
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            leadingIcon = { Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(videoName?.take(14) ?: "Video") },
                            modifier = Modifier.testTag("sticky_media_video_chip")
                        )
                        FilterChip(
                            selected = !musicUri.isNullOrBlank(),
                            onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                            leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(musicName?.take(14) ?: "Audio") },
                            modifier = Modifier.testTag("sticky_media_audio_chip")
                        )
                    }

                    if (!imageUri.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MediaPreviewCard(
                                mediaType = MediaType.IMAGE,
                                mediaUri = imageUri,
                                mediaDisplayName = imageName,
                                compact = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { imageUri = null; imageName = null }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Remove image")
                            }
                        }
                    }

                    if (!videoUri.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MediaPreviewCard(
                                mediaType = MediaType.VIDEO,
                                mediaUri = videoUri,
                                mediaDisplayName = videoName,
                                compact = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { videoUri = null; videoName = null }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Remove video")
                            }
                        }
                    }

                    if (!musicUri.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MediaPreviewCard(
                                mediaType = MediaType.MUSIC,
                                mediaUri = musicUri,
                                mediaDisplayName = musicName,
                                compact = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { musicUri = null; musicName = null }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Remove audio")
                            }
                        }
                    }

                    Text(
                        text = "Paper Color",
                        style = MaterialTheme.typography.labelLarge
                    )
                    StickyColorSelectorRow(
                        selectedColor = color,
                        onSelect = { color = it }
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Show floating on screen", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = if (canOverlay) "Stays visible over other apps" else "Needs Overlay permission once",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = pinOnScreen && canOverlay,
                                onCheckedChange = { checked ->
                                    if (checked && !canOverlay) {
                                        onRequestOverlayPermission()
                                    } else {
                                        pinOnScreen = checked
                                    }
                                },
                                modifier = Modifier.testTag("sticky_pin_switch")
                            )
                        }
                    }

                    OutlinedButton(
                        enabled = canSave,
                        onClick = {
                            val resolvedTitle = title.trim().ifBlank { "Quick Note" }
                            onSaveAsTemplate(
                                QuickTemplateEntity(
                                    name = resolvedTitle.take(24),
                                    targetKind = TemplateTargetKind.NOTE,
                                    defaultTitle = resolvedTitle,
                                    defaultBody = content.trim(),
                                    stickyColor = color,
                                    checklistJson = ChecklistCodec.encode(checklistItems),
                                    isBuiltIn = false
                                )
                            )
                            templateSavedNotice = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sticky_save_template_btn")
                    ) {
                        Text(if (templateSavedNotice) "Saved as Reusable Note Template" else "Save as Quick Template")
                    }
                }
            }
        }
    }
}

@Composable
fun StickyColorSelectorRow(
    selectedColor: StickyColor,
    onSelect: (StickyColor) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StickyColor.entries.forEach { swatch ->
            val selected = swatch == selectedColor
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(stickySurfaceColor(swatch.name))
                    .border(
                        width = if (selected) 3.dp else 1.5.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else stickyBorderColor(swatch.name),
                        shape = CircleShape
                    )
                    .clickable { onSelect(swatch) }
                    .testTag("swatch_${swatch.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = swatch.label,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
