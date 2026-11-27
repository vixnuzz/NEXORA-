package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ProjectsScreen(
    projects: List<Project>,
    tasks: List<ProjectTask>,
    searchQuery: String,
    canEdit: Boolean,
    canDelete: Boolean,
    canExport: Boolean,
    onUpsertProject: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onUpsertTask: (ProjectTask) -> Unit,
    onDeleteTask: (ProjectTask) -> Unit,
    onExportCsv: (String, List<String>, List<List<String>>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var kanbanCol by remember { mutableStateOf(KanbanColumn.TODO) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var showProjectDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<ProjectTask?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }

    val filteredProjects = remember(projects, searchQuery) {
        projects.filter { p ->
            searchQuery.isBlank() || p.name.contains(searchQuery, true) || p.memberNames.contains(searchQuery, true)
        }
    }
    val filteredTasks = remember(tasks, searchQuery, kanbanCol) {
        tasks.filter { t ->
            t.columnEnum() == kanbanCol &&
                (searchQuery.isBlank() || t.title.contains(searchQuery, true) || t.projectName.contains(searchQuery, true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModuleHeaderBar(
            title = "Projects & Kanban",
            subtitle = "${projects.size} projects • ${tasks.size} tasks",
            canCreate = canEdit,
            canExport = canExport,
            addLabel = if (selectedTab == 0) "New Task" else "New Project",
            addTag = "projects_add_button",
            onAddClick = {
                if (selectedTab == 0) {
                    editingTask = null; showTaskDialog = true
                } else {
                    editingProject = null; showProjectDialog = true
                }
            },
            onExportCsv = {
                if (selectedTab == 0) {
                    onExportCsv(
                        "kanban_tasks",
                        listOf("Title", "Project", "Stage", "Priority", "Assignee", "DueDate", "Subtasks"),
                        tasks.map { listOf(it.title, it.projectName, it.kanbanColumn, it.priority, it.assignee, it.dueDate, it.subtasksSummary) }
                    )
                } else {
                    onExportCsv(
                        "projects",
                        listOf("Name", "Status", "Progress%", "Deadline", "Members"),
                        projects.map { listOf(it.name, it.status, it.progressPercent.toString(), it.deadline, it.memberNames) }
                    )
                }
            }
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Kanban Board (${tasks.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Projects (${projects.size})") })
        }

        if (selectedTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KanbanColumn.entries.forEach { col ->
                    val count = tasks.count { it.columnEnum() == col }
                    FilterChip(
                        selected = kanbanCol == col,
                        onClick = { kanbanCol = col },
                        label = { Text("${col.label} ($count)") }
                    )
                }
            }

            if (filteredTasks.isEmpty()) {
                EmptyModuleCard(
                    title = "No Tasks in ${kanbanCol.label}",
                    description = "Create tasks with subtasks, deadlines, and reminders to manage sprint delivery.",
                    icon = Icons.Filled.ViewKanban,
                    actionLabel = if (canEdit) "Add Task" else null,
                    onAction = if (canEdit) { { editingTask = null; showTaskDialog = true } } else null
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredTasks, key = { it.id }) { task ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(task.projectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                                    }
                                    StatusPill(
                                        task.priorityEnum().label,
                                        when (task.priorityEnum()) {
                                            TaskPriority.URGENT -> StatusError
                                            TaskPriority.HIGH -> StatusWarning
                                            TaskPriority.MEDIUM -> NexoraCyan
                                            TaskPriority.LOW -> NexoraEmerald
                                        }
                                    )
                                }
                                if (task.subtasksSummary.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("Subtasks: ${task.subtasksSummary}", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Assignee: ${task.assignee.ifBlank { "Unassigned" }} • Due ${task.dueDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (canEdit) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val nextCol = when (task.columnEnum()) {
                                            KanbanColumn.TODO -> KanbanColumn.IN_PROGRESS
                                            KanbanColumn.IN_PROGRESS -> KanbanColumn.REVIEW
                                            KanbanColumn.REVIEW -> KanbanColumn.DONE
                                            KanbanColumn.DONE -> null
                                        }
                                        if (nextCol != null) {
                                            FilledTonalButton(onClick = { onUpsertTask(task.copy(kanbanColumn = nextCol.name)) }) {
                                                Text("Move -> ${nextCol.label}")
                                            }
                                        }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = { editingTask = task; showTaskDialog = true }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit task")
                                        }
                                        if (canDelete) {
                                            IconButton(onClick = { onDeleteTask(task) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = StatusError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredProjects.isEmpty()) {
                EmptyModuleCard(
                    title = "No Projects Created",
                    description = "Organize team members, project milestones, and progress percentages.",
                    icon = Icons.Filled.FolderCopy,
                    actionLabel = if (canEdit) "Create Project" else null,
                    onAction = if (canEdit) { { editingProject = null; showProjectDialog = true } } else null
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredProjects, key = { it.id }) { proj ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(proj.name, style = MaterialTheme.typography.titleMedium)
                                        Text("Deadline: ${proj.deadline}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(
                                        proj.statusEnum().label,
                                        when (proj.statusEnum()) {
                                            ProjectStatus.ACTIVE -> NexoraEmerald
                                            ProjectStatus.COMPLETED -> NexoraCyan
                                            ProjectStatus.ON_HOLD -> StatusWarning
                                            ProjectStatus.PLANNING -> StatusInfo
                                        }
                                    )
                                }
                                if (proj.description.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(proj.description, style = MaterialTheme.typography.bodySmall)
                                }
                                if (proj.memberNames.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Team: ${proj.memberNames}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Progress", style = MaterialTheme.typography.labelSmall)
                                    Text("${proj.progressPercent}%", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (proj.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                )
                                if (canEdit) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        IconButton(onClick = { editingProject = proj; showProjectDialog = true }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit project")
                                        }
                                        if (canDelete) {
                                            IconButton(onClick = { onDeleteProject(proj) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete project", tint = StatusError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProjectDialog) {
        ProjectFormDialog(editingProject, { showProjectDialog = false }, onUpsertProject)
    }
    if (showTaskDialog) {
        TaskFormDialog(editingTask, projects, { showTaskDialog = false }, onUpsertTask)
    }
}
