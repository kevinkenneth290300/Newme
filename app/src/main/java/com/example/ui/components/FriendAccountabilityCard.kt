package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.FriendConnectionEntity
import com.example.ui.theme.*

@Composable
fun FriendAccountabilityCard(
    friends: List<FriendConnectionEntity>,
    userCode: String,
    onAddFriend: (code: String, name: String) -> Unit,
    onRemoveFriend: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Surface(
        color = NewMeSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, NewMeBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("friend_accountability_card")
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = NewMeSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOCAL ACCOUNTABILITY NOTES",
                        color = NewMeSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                }

                TextButton(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("add_friend_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NewMeSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("+ ALLY", color = NewMeSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Stored only here. These entries do not connect or synchronize devices.",
                color = NewMeTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (friends.isEmpty()) {
                Surface(
                    color = NewMeSurfaceDarker,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NewMeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No local notes yet.",
                        color = NewMeTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    friends.forEach { friend ->
                        Surface(
                            color = NewMeSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NewMeBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = friend.friendName,
                                        color = NewMeTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Local label: ${friend.friendCode}",
                                        color = NewMeTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }

                                IconButton(
                                    onClick = { onRemoveFriend(friend.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove ally",
                                        tint = NewMeTextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFriendDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { code, name ->
                onAddFriend(code, name)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddFriendDialog(
    onDismiss: () -> Unit,
    onAdd: (code: String, name: String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NewMeSurface,
            border = BorderStroke(1.dp, NewMeBorder),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "CONNECT ACCOUNTABILITY ALLY",
                    color = NewMeSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ally's Name") },
                    placeholder = { Text("e.g., Alex") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("friend_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NewMeSecondary,
                        unfocusedBorderColor = NewMeBorder,
                        focusedTextColor = NewMeTextPrimary,
                        unfocusedTextColor = NewMeTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Ally's Peer Code") },
                    placeholder = { Text("e.g., NEWME-ALX-9182") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("friend_code_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NewMeSecondary,
                        unfocusedBorderColor = NewMeBorder,
                        focusedTextColor = NewMeTextPrimary,
                        unfocusedTextColor = NewMeTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (code.isNotBlank() && name.isNotBlank()) {
                            onAdd(code.trim(), name.trim())
                        }
                    },
                    enabled = code.isNotBlank() && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp).testTag("confirm_add_friend"),
                    colors = ButtonDefaults.buttonColors(containerColor = NewMeSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CONNECT ALLY", color = NewMePrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
