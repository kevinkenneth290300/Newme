package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.EnvironmentTaskEntity
import com.example.ui.theme.*

@Composable
fun EnvironmentMaintenanceSection(
    tasks: List<EnvironmentTaskEntity>,
    onToggleTask: (EnvironmentTaskEntity) -> Unit,
    onAddTask: (title: String, category: String) -> Unit,
    onDeleteTask: (EnvironmentTaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val completedCount = tasks.count { it.isCompleted }

    Surface(
        color = NewMeSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NewMeBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("environment_maintenance_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LIVING SPACE & ROUTINES",
                            color = NewMePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = if (completedCount == tasks.size && tasks.isNotEmpty()) NewMeTertiary.copy(alpha = 0.2f) else NewMeSurfaceDarker,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$completedCount/${tasks.size} DONE",
                                color = if (completedCount == tasks.size && tasks.isNotEmpty()) NewMeTertiary else NewMeTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "External order creates internal calm.",
                        color = NewMeTextMuted,
                        fontSize = 11.sp
                    )
                }

                TextButton(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_environment_task_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NewMePrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("+ TASK", color = NewMePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tasks.isEmpty()) {
                Text(
                    text = "No environment tasks scheduled for today. Tap '+ TASK' to add your living space standards.",
                    color = NewMeTextMuted,
                    fontSize = 11.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tasks.forEach { task ->
                        EnvironmentTaskRow(
                            task = task,
                            onToggle = { onToggleTask(task) },
                            onDelete = { onDeleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEnvironmentTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, category ->
                onAddTask(title, category)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun EnvironmentTaskRow(
    task: EnvironmentTaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = if (task.isCompleted) NewMeSurfaceDarker else NewMeSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (task.isCompleted) NewMeBorder.copy(alpha = 0.5f) else NewMeBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("env_task_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Checkbox Box
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (task.isCompleted) NewMeTertiary else Color.Transparent,
                    border = BorderStroke(1.5.dp, if (task.isCompleted) NewMeTertiary else NewMeBorderStrong),
                    modifier = Modifier.size(20.dp)
                ) {
                    if (task.isCompleted) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = NewMePrimaryDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = task.title,
                        color = if (task.isCompleted) NewMeTextMuted else NewMeTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.category.replace("_", " "),
                            color = NewMePrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        task.completionTime?.let { time ->
                            Text(
                                text = " • $time",
                                color = NewMeTextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = NewMeTextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun AddEnvironmentTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("MORNING") }

    val categories = listOf("MORNING", "LEAVING_HOME", "AFTER_WORK", "BEFORE_SLEEP", "WEEKLY")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NewMeSurface,
            border = BorderStroke(1.dp, NewMeBorder),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "ADD ENVIRONMENT ROUTINE",
                    color = NewMePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Description") },
                    placeholder = { Text("e.g., Wipe kitchen counter, declutter desk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("env_task_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NewMePrimary,
                        unfocusedBorderColor = NewMeBorder,
                        focusedTextColor = NewMeTextPrimary,
                        unfocusedTextColor = NewMeTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("CATEGORY", color = NewMeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) NewMePrimary.copy(alpha = 0.2f) else NewMeSurfaceDarker,
                            border = BorderStroke(1.dp, if (isSelected) NewMePrimary else NewMeBorder),
                            modifier = Modifier.weight(1f).clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat.replace("_", " "),
                                color = if (isSelected) NewMePrimary else NewMeTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.drop(3).forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) NewMePrimary.copy(alpha = 0.2f) else NewMeSurfaceDarker,
                            border = BorderStroke(1.dp, if (isSelected) NewMePrimary else NewMeBorder),
                            modifier = Modifier.weight(1f).clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat.replace("_", " "),
                                color = if (isSelected) NewMePrimary else NewMeTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAdd(title.trim(), selectedCategory)
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("confirm_add_env_task"),
                    colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ADD TASK", color = NewMePrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
