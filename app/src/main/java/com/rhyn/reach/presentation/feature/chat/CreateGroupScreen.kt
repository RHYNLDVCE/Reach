package com.rhyn.reach.presentation.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onGroupCreated: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf(setOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    val selectableUsers by viewModel.selectableUsers.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New Group", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (groupName.isNotBlank() && selectedMembers.isNotEmpty()) {
                                isCreating = true
                                viewModel.createGroupChat(
                                    groupName = groupName,
                                    memberIds = selectedMembers.toList(),
                                    onSuccess = {
                                        isCreating = false
                                        onGroupCreated()
                                    },
                                    onError = { 
                                        isCreating = false
                                    }
                                )
                            }
                        },
                        // Only enable if they typed a name and selected at least 1 person
                        enabled = groupName.isNotBlank() && selectedMembers.isNotEmpty() && !isCreating
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Create", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- 1. Group Name Input ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                TextField(
                    value = groupName,
                    onValueChange = { 
                        groupName = it
                        errorMessage = null 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape),
                    placeholder = { Text("Group Name...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // --- 2. Member Selection Header ---
            Text(
                text = "SELECT MEMBERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            // --- 3. Contacts List ---
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(selectableUsers) { user ->
                    val isSelected = selectedMembers.contains(user.userId)
                    
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newSet = selectedMembers.toMutableSet()
                                if (isSelected) newSet.remove(user.userId) else newSet.add(user.userId)
                                selectedMembers = newSet
                            },
                        leadingContent = { UserAvatar(user.username) },
                        headlineContent = { 
                            Text(user.username, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground) 
                        },
                        trailingContent = {
                            // Custom sleek checkmark instead of a clunky Checkbox
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = "Select",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
                
                if (selectableUsers.isEmpty()) {
                    item {
                        Text(
                            text = "No contacts found. Search for users to add them to your network first.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}