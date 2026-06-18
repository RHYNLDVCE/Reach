package com.rhyn.reach.presentation.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    
    val searchedUser by viewModel.targetUser.collectAsState()
    val focusManager = LocalFocusManager.current

    // Clear search when leaving the screen
    DisposableEffect(Unit) {
        onDispose { viewModel.clearSearch() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Find Contact", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // --- 1. Sleek Search Bar ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        searchError = null
                        viewModel.clearSearch()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape),
                    placeholder = { Text("Enter exact username...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                viewModel.clearSearch()
                                searchError = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            if (searchQuery.isNotBlank()) {
                                viewModel.searchUser(
                                    username = searchQuery,
                                    onLoading = { isSearching = it },
                                    onSuccess = { /* Data is loaded into searchedUser state */ },
                                    onError = { searchError = it }
                                )
                            }
                        }
                    ),
                    singleLine = true
                )
            }

            // --- 2. Search Results / States ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when {
                    isSearching -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp)
                        )
                    }
                    searchError != null -> {
                        Text(
                            text = searchError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp)
                        )
                    }
                    searchedUser != null && searchedUser?.username == searchQuery -> {
                        // User Found Card
                        ListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onNavigateToChat(searchedUser!!.user_id) },
                            leadingContent = { UserAvatar(searchedUser!!.username) },
                            headlineContent = { Text(searchedUser!!.username, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                            supportingContent = {
                                Text(
                                    text = if (searchedUser!!.is_online) "Online" else "Offline",
                                    color = if (searchedUser!!.is_online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Button(
                                    onClick = { onNavigateToChat(searchedUser!!.user_id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Message")
                                }
                            }
                        )
                    }
                    else -> {
                        // Empty State (Before searching)
                        Text(
                            text = "Search for a user to start chatting.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp)
                        )
                    }
                }
            }
        }
    }
}