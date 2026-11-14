package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.QuickTemplateEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskProofEntity
import com.example.data.model.TaskStatus
import com.example.data.model.TemplateTargetKind
import com.example.data.repository.CachedMediaResult
import com.example.ui.DateCalendarUtils
import com.example.ui.components.ChecklistInteractiveSection
import com.example.ui.components.QuickTemplateStrip
import com.example.ui.components.TaskProofCaptureDialog
import com.example.ui.theme.JetBrainsClockMono
import com.example.ui.theme.stickyBorderColor
import com.example.ui.theme.stickySurfaceColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    proofs: List<TaskProofEntity>,
    templates: List<QuickTemplateEntity> = emptyList(),
    selectedTaskId: Long?,
    defaultSnoozeMinutes: Int,
    isTwoPane: Boolean,
    onSelectTask: (Long?) -> Unit,
    onAddTask: () -> Unit,
    onApplyTemplate: (QuickTemplateEntity) -> Unit = {},
    onDeleteTemplate: (Long) -> Unit = {},
    onToggleChecklistItem: (Long, String) -> Unit = { _, _ -> },
    onEditTask: (TaskEntity) -> Unit,
    onToggleTaskEnabled: (TaskEntity, Boolean) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onTestTaskAlarm: (TaskEntity) -> Unit,
    onPersistProofImage: suspend (Uri) -> CachedMediaResult,
    onSubmitProof: (Long, TaskStatus, String, String, String) -> Unit,
    onDeferLater: (Long, Int) -> Unit
) {
    var filterStatus by remember { mutableStateOf<TaskStatus?>(null) }
    var proofDialogRequest by remember { mutableStateOf<Pair<TaskEntity, TaskStatus>?>(null) }
    var laterPickerTask by remember { mutableStateOf<TaskEntity?>(null) }

    val taskTemplates = remember(templates) {
        templates.filter { it.targetKind == TemplateTargetKind.TASK }
    }
    val filteredTasks = remember(tasks, filterStatus) {
        if (filterStatus == null) tasks else tasks.filter { it.status == filterStatus }
    }
    val proofsByTask = remember(proofs) { proofs.groupBy { it.taskId } }
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId } ?: filteredTasks.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTwoPane) 24.dp else 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tasks & Proof Alarms", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Works like an alarm: mark Started, Working, Done (with image proof) or Later",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onAddTask,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_task_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New Task")
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filterStatus == null,
                onClick = { filterStatus = null },
                label = { Text("All (${tasks.size})") }
            )
            TaskStatus.entries.forEach { st ->
                val count = tasks.count { it.status == st }
                FilterChip(
                    selected = filterStatus == st,
                    onClick = { filterStatus = if (filterStatus == st) null else st },
                    label = { Text("${st.label} ($count)") }
                )
            }
        }

        QuickTemplateStrip(
            title = "Quick Task Templates",
            templates = taskTemplates,
            onApplyTemplate = onApplyTemplate,
            onDeleteCustomTemplate = { tpl -> onDeleteTemplate(tpl.id) }
        )

        if (filteredTasks.isEmpty()) {
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
                        imageVector = Icons.Filled.TaskAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("No tasks yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Schedule a Task alarm that prompts you for Started, Working, or Done image proof, or lets you tap Later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onAddTask) {
                        Text("Create First Task")
                    }
                }
            }
        } else if (isTwoPane) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        val taskProofs = proofsByTask[task.id].orEmpty()
                        TaskCard(
                            task = task,
                            proofCount = taskProofs.size,
                            proofs = taskProofs,
                            isSelected = selectedTask?.id == task.id,
                            showInlineProofs = false,
                            onClick = { onSelectTask(task.id) },
                            onToggleEnabled = { onToggleTaskEnabled(task, it) },
                            onToggleChecklistItem = { itemId -> onToggleChecklistItem(task.id, itemId) },
                            onRequestStatusProof = { st -> proofDialogRequest = task to st },
                            onRequestLater = { laterPickerTask = task },
                            onTestAlarm = { onTestTaskAlarm(task) },
                            onEdit = { onEditTask(task) },
                            onDelete = { onDeleteTask(task) }
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                ) {
                    if (selectedTask != null) {
                        val taskProofs = proofsByTask[selectedTask.id].orEmpty()
                        TaskInspectorPane(
                            task = selectedTask,
                            proofs = taskProofs,
                            onToggleChecklistItem = { itemId -> onToggleChecklistItem(selectedTask.id, itemId) },
                            onRequestStatusProof = { st -> proofDialogRequest = selectedTask to st },
                            onRequestLater = { laterPickerTask = selectedTask },
                            onTestAlarm = { onTestTaskAlarm(selectedTask) },
                            onEdit = { onEditTask(selectedTask) }
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select a task to view details and saved image proofs")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    val taskProofs = proofsByTask[task.id].orEmpty()
                    TaskCard(
                        task = task,
                        proofCount = taskProofs.size,
                        proofs = taskProofs,
                        isSelected = selectedTaskId == task.id,
                        showInlineProofs = selectedTaskId == task.id,
                        onClick = {
                            onSelectTask(if (selectedTaskId == task.id) null else task.id)
                        },
                        onToggleEnabled = { onToggleTaskEnabled(task, it) },
                        onToggleChecklistItem = { itemId -> onToggleChecklistItem(task.id, itemId) },
                        onRequestStatusProof = { st -> proofDialogRequest = task to st },
                        onRequestLater = { laterPickerTask = task },
                        onTestAlarm = { onTestTaskAlarm(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }
        }
    }

    proofDialogRequest?.let { (task, targetStatus) ->
        TaskProofCaptureDialog(
            taskTitle = task.title,
            targetStatus = targetStatus,
            onPersistProofImage = onPersistProofImage,
            onDismiss = { proofDialogRequest = null },
            onSaveProof = { uri, name, note ->
                onSubmitProof(task.id, targetStatus, uri, name, note)
                proofDialogRequest = null
            }
        )
    }

    laterPickerTask?.let { task ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { laterPickerTask = null },
            title = { Text("Defer \"${task.title}\"") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pick when to ring this task alarm again:")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(defaultSnoozeMinutes, 15, 30, 60, 180).distinct().forEach { mins ->
                            FilledTonalButton(
                                onClick = {
                                    onDeferLater(task.id, mins)
                                    laterPickerTask = null
                                }
                            ) {
                                Text(if (mins >= 60) "${mins / 60} hr" else "$mins min")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { laterPickerTask = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskCard(
    task: TaskEntity,
    proofCount: Int,
    proofs: List<TaskProofEntity>,
    isSelected: Boolean,
    showInlineProofs: Boolean,
    onClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleChecklistItem: (String) -> Unit,
    onRequestStatusProof: (TaskStatus) -> Unit,
    onRequestLater: () -> Unit,
    onTestAlarm: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = stickySurfaceColor(task.stickyColor.name)
    val border = stickyBorderColor(task.stickyColor.name)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else border,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("task_card_${task.id}")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
                        Text(
                            text = DateCalendarUtils.formatTime(task.triggerAtMillis),
                            fontFamily = JetBrainsClockMono,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = taskStatusContainerColor(task.status)
                    ) {
                        Text(
                            text = task.status.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Switch(
                    checked = task.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${DateCalendarUtils.formatDateShort(task.triggerAtMillis)} • ${task.recurrenceSummaryLabel()} • ${task.mediaSummaryLabel()} • $proofCount proof(s)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val taskChecklist = remember(task.checklistJson) { task.checklistItems() }
            if (taskChecklist.isNotEmpty()) {
                ChecklistInteractiveSection(
                    items = taskChecklist,
                    compact = true,
                    maxVisible = 3,
                    onToggleItem = onToggleChecklistItem
                )
            }

            // Quick Task Status & Proof Buttons
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(
                    onClick = { onRequestStatusProof(TaskStatus.STARTED) },
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Started")
                }

                FilledTonalButton(
                    onClick = { onRequestStatusProof(TaskStatus.WORKING) },
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Working")
                }

                Button(
                    onClick = { onRequestStatusProof(TaskStatus.DONE) },
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Done")
                }

                OutlinedButton(
                    onClick = onRequestLater,
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Later")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onTestAlarm) {
                    Text("Trigger Task Alarm")
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit task")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete task")
                    }
                }
            }

            if (showInlineProofs && proofs.isNotEmpty()) {
                HorizontalDivider()
                Text("Saved Proof History", style = MaterialTheme.typography.titleSmall)
                proofs.forEach { proof ->
                    ProofHistoryRow(proof = proof)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskInspectorPane(
    task: TaskEntity,
    proofs: List<TaskProofEntity>,
    onToggleChecklistItem: (String) -> Unit,
    onRequestStatusProof: (TaskStatus) -> Unit,
    onRequestLater: () -> Unit,
    onTestAlarm: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Edit")
            }
        }

        if (task.description.isNotBlank()) {
            Text(task.description, style = MaterialTheme.typography.bodyLarge)
        }

        val inspectorChecklist = remember(task.checklistJson) { task.checklistItems() }
        if (inspectorChecklist.isNotEmpty()) {
            ChecklistInteractiveSection(
                items = inspectorChecklist,
                compact = false,
                onToggleItem = onToggleChecklistItem
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onRequestStatusProof(TaskStatus.DONE) }) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Mark Done + Proof")
            }
            FilledTonalButton(onClick = { onRequestStatusProof(TaskStatus.WORKING) }) {
                Text("Working + Proof")
            }
            FilledTonalButton(onClick = { onRequestStatusProof(TaskStatus.STARTED) }) {
                Text("Started + Proof")
            }
            OutlinedButton(onClick = onRequestLater) {
                Text("Later")
            }
            OutlinedButton(onClick = onTestAlarm) {
                Text("Test Alarm")
            }
        }

        HorizontalDivider()

        Text(
            text = "Saved Image Proofs (${proofs.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        if (proofs.isEmpty()) {
            Text(
                text = "No image proofs saved yet. Tapping Started, Working, or Done asks for a photo proof and stores it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            proofs.forEach { proof ->
                ProofHistoryRow(proof = proof)
            }
        }
    }
}

@Composable
private fun ProofHistoryRow(proof: TaskProofEntity) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = Uri.parse(proof.imageUri),
                contentDescription = "Proof image for ${proof.statusAtProof.label}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = taskStatusContainerColor(proof.statusAtProof)
                ) {
                    Text(
                        text = proof.statusAtProof.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (proof.note.isNotBlank()) {
                    Text(proof.note, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = "${DateCalendarUtils.formatDateShort(proof.capturedAtMillis)} • ${DateCalendarUtils.formatTime(proof.capturedAtMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun taskStatusContainerColor(status: TaskStatus) = when (status) {
    TaskStatus.PENDING -> MaterialTheme.colorScheme.surfaceContainerHigh
    TaskStatus.STARTED -> MaterialTheme.colorScheme.secondaryContainer
    TaskStatus.WORKING -> MaterialTheme.colorScheme.tertiaryContainer
    TaskStatus.LATER -> MaterialTheme.colorScheme.surfaceVariant
    TaskStatus.DONE -> MaterialTheme.colorScheme.primaryContainer
}
