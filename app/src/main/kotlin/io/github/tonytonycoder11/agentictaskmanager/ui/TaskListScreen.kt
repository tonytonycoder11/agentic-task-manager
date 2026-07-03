package io.github.tonytonycoder11.agentictaskmanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Priority
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Recurrence
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus
import io.github.tonytonycoder11.agentictaskmanager.ui.theme.AtmTheme
import io.github.tonytonycoder11.agentictaskmanager.ui.theme.LocalStatusColors

/**
 * The whole UI: the task list plus three dialogs — add a task (with prerequisites), link two tasks
 * (a cycle is rejected), and delete a task (two-step confirmation).
 */
@Composable
fun TaskListScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingDeletion by viewModel.pendingDeletion.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messageFlow.collect { snackbarHostState.showSnackbar(it) }
    }

    var showAddTask by remember { mutableStateOf(false) }
    var showAddDependency by remember { mutableStateOf(false) }
    // null = all cadences; Recurrence.NONE = one-time tasks.
    var recurrenceFilter by remember { mutableStateOf<Recurrence?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTask = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add task")
            }
        },
    ) { padding ->
        // Partition the already-sorted rows into status sections; we split, never re-sort.
        val visibleRows = state.rows.filter { recurrenceFilter == null || it.recurrence == recurrenceFilter }
        val actionableRows = visibleRows.filter { it.status == TaskStatus.OPEN && it.isActionable }
        val blockedRows = visibleRows.filter { it.status == TaskStatus.OPEN && !it.isActionable }
        val completedRows = visibleRows.filter { it.status == TaskStatus.COMPLETED }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                BoardHeader(
                    actionableCount = actionableRows.size,
                    blockedCount = blockedRows.size,
                    doneCount = completedRows.size,
                    addDependencyEnabled = state.pickerOptions.size >= 2,
                    onAddDependency = { showAddDependency = true },
                )
            }
            item {
                RecurrenceFilterRow(selected = recurrenceFilter, onSelect = { recurrenceFilter = it })
            }

            if (visibleRows.isEmpty()) {
                item {
                    if (state.rows.isEmpty()) {
                        EmptyState()
                    } else {
                        EmptyState("Nothing here", "No tasks match this filter.")
                    }
                }
            } else {
                taskSection(
                    label = "Actionable",
                    rows = actionableRows,
                    onComplete = { viewModel.onCompleteTask(it) },
                    onDelete = { viewModel.onRequestDelete(it) },
                )
                taskSection(
                    label = "Blocked",
                    rows = blockedRows,
                    onComplete = { viewModel.onCompleteTask(it) },
                    onDelete = { viewModel.onRequestDelete(it) },
                )
                taskSection(
                    label = "Completed",
                    rows = completedRows,
                    dimmed = true,
                    onComplete = { viewModel.onCompleteTask(it) },
                    onDelete = { viewModel.onRequestDelete(it) },
                )
            }
        }
    }

    if (showAddTask) {
        AddTaskDialog(
            options = state.pickerOptions,
            onDismiss = { showAddTask = false },
            onConfirm = { title, dependsOn ->
                viewModel.onAddTask(title, dependsOn)
                showAddTask = false
            },
        )
    }

    if (showAddDependency) {
        AddDependencyDialog(
            options = state.pickerOptions,
            onDismiss = { showAddDependency = false },
            onConfirm = { dependent, prerequisite ->
                viewModel.onAddDependency(dependent, prerequisite)
                showAddDependency = false
            },
        )
    }

    pendingDeletion?.let { pending ->
        AlertDialog(
            onDismissRequest = { viewModel.onCancelDelete() },
            title = { Text("Delete task?") },
            text = { Text(pending.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.onConfirmDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onCancelDelete() }) { Text("Cancel") }
            },
        )
    }
}

/**
 * Adds one labelled section (header + task cards) to a [LazyColumn]; renders nothing, header
 * included, when [rows] is empty. [dimmed] fades the cards (used for the "Completed" group).
 */
private fun LazyListScope.taskSection(
    label: String,
    rows: List<TaskRowUi>,
    dimmed: Boolean = false,
    onComplete: (TaskId) -> Unit,
    onDelete: (TaskId) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "section-$label") {
        SectionHeader(label = label, count = rows.size)
    }
    items(rows, key = { it.id.value }) { row ->
        TaskCard(
            row = row,
            dimmed = dimmed,
            onComplete = { onComplete(row.id) },
            onDelete = { onDelete(row.id) },
        )
    }
}

/** Section divider: an uppercased label with its task count, e.g. "ACTIONABLE · 4". */
@Composable
private fun SectionHeader(label: String, count: Int) {
    Text(
        text = "${label.uppercase()} · $count",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 2.dp),
    )
}

@Composable
private fun BoardHeader(
    actionableCount: Int,
    blockedCount: Int,
    doneCount: Int,
    addDependencyEnabled: Boolean,
    onAddDependency: () -> Unit,
) {
    val status = LocalStatusColors.current
    Column(modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Agentic Task Manager",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAddDependency, enabled = addDependencyEnabled) {
                Icon(
                    Icons.Rounded.Link,
                    contentDescription = "Add dependency",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPill(actionableCount, "Actionable", status.actionableContainer, status.actionable)
            StatPill(
                blockedCount,
                "Blocked",
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatPill(
                doneCount,
                "Done",
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowScope.StatPill(value: Int, label: String, container: Color, content: Color) {
    Surface(
        color = container,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.weight(1f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = content,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = content.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun TaskCard(
    row: TaskRowUi,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    dimmed: Boolean = false,
) {
    val completed = row.status == TaskStatus.COMPLETED
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = if (dimmed) Modifier.alpha(0.6f) else Modifier,
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Leading priority accent bar.
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, top = 12.dp, bottom = 12.dp)
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(priorityColor(row.priority), RoundedCornerShape(2.dp)),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, top = 10.dp, bottom = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                IconButton(onClick = onComplete, enabled = !completed) {
                    Icon(
                        imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (completed) "Completed" else "Mark complete",
                        tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f).padding(top = 6.dp)) {
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (completed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                    )
                    Spacer(Modifier.height(4.dp))
                    MetaRow(row)
                    StatusSection(row, completed)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete ${row.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaRow(row: TaskRowUi) {
    val status = LocalStatusColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = row.priority.label(),
            style = MaterialTheme.typography.labelMedium,
            color = priorityColor(row.priority),
        )
        if (row.dueLabel != null) {
            Dot()
            if (row.isOverdue) {
                Icon(
                    Icons.Rounded.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = status.overdue,
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = if (row.isOverdue) "${row.dueLabel} · Overdue" else row.dueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (row.isOverdue) status.overdue else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusSection(row: TaskRowUi, completed: Boolean) {
    val status = LocalStatusColors.current
    if (!completed) {
        Spacer(Modifier.height(8.dp))
        if (row.isActionable) {
            SoftPill(
                text = "Actionable",
                container = status.actionableContainer,
                content = status.actionable,
                leading = Icons.Rounded.CheckCircle,
            )
        } else if (row.blockedByTitles.isNotEmpty()) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).padding(top = 2.dp),
                    tint = status.blocked,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Blocked by ${row.blockedByTitles.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = status.blocked,
                )
            }
        }
    }

    val showRecurrence = row.recurrence != Recurrence.NONE
    val showBlocks = row.blocksCount > 0 && !completed
    if (showRecurrence || showBlocks) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showRecurrence) {
                FaintChip(text = row.recurrence.label(), leading = Icons.Rounded.Repeat)
            }
            if (showBlocks) {
                Text(
                    text = "Blocks ${row.blocksCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SoftPill(text: String, container: Color, content: Color, leading: ImageVector?) {
    Surface(color = container, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Icon(leading, contentDescription = null, modifier = Modifier.size(14.dp), tint = content)
                Spacer(Modifier.width(4.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}

@Composable
private fun FaintChip(text: String, leading: ImageVector) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                leading,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .size(3.dp)
            .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
    )
}

@Composable
private fun EmptyState(
    title: String = "No tasks yet",
    subtitle: String = "Tap + to add your first task.",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Scrollable chip row to filter the list by recurrence cadence; "All" clears the filter. */
@Composable
private fun RecurrenceFilterRow(selected: Recurrence?, onSelect: (Recurrence?) -> Unit) {
    val options: List<Pair<String, Recurrence?>> = listOf(
        "All" to null,
        "One-time" to Recurrence.NONE,
        "Daily" to Recurrence.DAILY,
        "Weekly" to Recurrence.WEEKLY,
        "Monthly" to Recurrence.MONTHLY,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (label, value) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
                shape = MaterialTheme.shapes.small,
            )
        }
    }
}

private fun Priority.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun Recurrence.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun priorityColor(priority: Priority): Color {
    val s = LocalStatusColors.current
    return when (priority) {
        Priority.URGENT -> s.priorityUrgent
        Priority.HIGH -> s.priorityHigh
        Priority.MEDIUM -> s.priorityMedium
        Priority.LOW -> s.priorityLow
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskCardPreview() {
    AtmTheme {
        TaskCard(
            row = TaskRowUi(
                id = TaskId("preview"),
                title = "Prepare slides",
                description = null,
                priority = Priority.HIGH,
                status = TaskStatus.OPEN,
                isActionable = true,
                dueLabel = "Jul 1, 09:00",
                isOverdue = false,
                blockedByTitles = emptyList(),
                blocksCount = 1,
                recurrence = Recurrence.WEEKLY,
            ),
            onComplete = {},
            onDelete = {},
        )
    }
}

@Composable
private fun AddTaskDialog(
    options: List<TaskOptionUi>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dependsOn: List<TaskId>) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    val selected = remember { mutableStateOf(emptySet<TaskId>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (options.isNotEmpty()) {
                    Text(
                        "Depends on (optional):",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Column(
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        options.forEach { option ->
                            val isChecked = option.id in selected.value
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = isChecked,
                                        onValueChange = { checked ->
                                            selected.value = if (checked) {
                                                selected.value + option.id
                                            } else {
                                                selected.value - option.id
                                            }
                                        },
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = isChecked, onCheckedChange = null)
                                Text(option.title, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, selected.value.toList()) },
                enabled = title.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDependencyDialog(
    options: List<TaskOptionUi>,
    onDismiss: () -> Unit,
    onConfirm: (dependent: TaskId, prerequisite: TaskId) -> Unit,
) {
    var dependent by remember { mutableStateOf<TaskOptionUi?>(null) }
    var prerequisite by remember { mutableStateOf<TaskOptionUi?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add dependency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Pick the task that should be blocked, then the task it must wait for. " +
                        "A link that would create a cycle is rejected.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TaskPicker("This task…", options, dependent) { dependent = it }
                TaskPicker("…depends on", options, prerequisite) { prerequisite = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val d = dependent
                    val p = prerequisite
                    if (d != null && p != null) onConfirm(d.id, p.id)
                },
                enabled = dependent != null && prerequisite != null && dependent?.id != prerequisite?.id,
            ) { Text("Link") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskPicker(
    label: String,
    options: List<TaskOptionUi>,
    selected: TaskOptionUi?,
    onSelect: (TaskOptionUi) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected?.title.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
