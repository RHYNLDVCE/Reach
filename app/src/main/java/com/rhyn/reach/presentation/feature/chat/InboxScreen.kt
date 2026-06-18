package com.rhyn.reach.presentation.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhyn.reach.data.local.LocalMessageEntity
import com.rhyn.reach.data.local.MessageType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: ChatViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val threads by viewModel.recentThreads.collectAsState()
    val users by viewModel.userMap.collectAsState()

    Scaffold(
        modifier = modifier,
        // Make the top bar perfectly flush with the background
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.SemiBold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = onNavigateToCreateGroup) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSearch,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // Map the scaffold padding directly to the content padding of the list
                // This makes the items scroll seamlessly under the top bar
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = 80.dp // Extra space at the bottom so the FAB doesn't cover the last message
            )
        ) {
            items(threads) { message ->
                val displayTitle = users[message.threadId] ?: "Unknown"
                val previewText = getMessagePreviewText(message)

                ListItem(
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    leadingContent = { UserAvatar(displayTitle) },
                    headlineContent = { Text(displayTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                    supportingContent = {
                        Text(
                            text = if (message.isFromMe) "You: $previewText" else previewText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Text(message.deliveryState.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier.clickable { onNavigateToChat(message.threadId) }
                )
            }
        }
    }
}

// Modern initial-based avatar
@Composable
fun UserAvatar(name: String) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Helper to generate a preview snippet for the Inbox
fun getMessagePreviewText(message: LocalMessageEntity): String {
    return when (message.messageType) {
        MessageType.IMAGE -> {
            if (message.plaintextContent.isNotBlank()) {
                "[Photo] ${message.plaintextContent}"
            } else {
                "[Photo]"
            }
        }
        MessageType.FILE -> {
            val fileName = message.plaintextContent.ifBlank { "Document" }
            "[File] $fileName"
        }
        else -> {
            message.plaintextContent
        }
    }
}