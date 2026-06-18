package com.rhyn.reach.presentation.feature.chat

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.rhyn.reach.data.local.LocalMessageEntity
import com.rhyn.reach.data.local.MessageType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    targetUserId: String,
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val users by viewModel.userMap.collectAsState()
    val displayTitle = users[targetUserId] ?: "Unknown"
    val selectableUsers by viewModel.selectableUsers.collectAsState()
    val targetUserEntity = selectableUsers.find { it.userId == targetUserId }
    var showMenu by remember { mutableStateOf(false) }
    val isBackupEnabled = targetUserEntity?.isBackupEnabled ?: true

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(targetUserId) {
        viewModel.setChatPartner(targetUserId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChatAvatar(name = displayTitle)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(displayTitle, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Chat Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cloud Backup") },
                            trailingIcon = {
                                Switch(
                                    checked = isBackupEnabled,
                                    onCheckedChange = { isChecked ->
                                        viewModel.toggleCloudBackup(targetUserId, isChecked)
                                    }
                                )
                            },
                            onClick = {
                                viewModel.toggleCloudBackup(targetUserId, !isBackupEnabled)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                onSendMessage = { text ->
                    viewModel.sendMessage(text)
                    coroutineScope.launch {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                },
                onSendImage = { uri ->
                    viewModel.sendImageMessage(uri.toString())
                    coroutineScope.launch {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                },
                onSendFile = { uri ->
                    viewModel.sendFileMessage(uri.toString())
                    coroutineScope.launch {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            reverseLayout = true
        ) {
            items(
                items = messages.reversed(),
                key = { it.messageId }
            ) { message ->
                MessageBubble(
                    message = message,
                    users = users,
                    onDelete = { viewModel.deleteMessage(message.messageId) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: LocalMessageEntity,
    users: Map<String, String>,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isMe = message.isFromMe
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val senderName = users[message.senderId] ?: "Unknown"

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    val bubbleShape = if (isMe) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    val backgroundColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    var showContextMenu by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isMe) {
                ChatAvatar(name = senderName, size = 28.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(horizontalAlignment = alignment) {
                Box {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(bubbleShape)
                            .background(backgroundColor)
                            .combinedClickable(
                                onClick = {
                                    if (message.messageType == MessageType.IMAGE && message.attachmentUri != null) {
                                        showFullImage = true
                                    }
                                },
                                onLongClick = { showContextMenu = true }
                            )
                    ) {
                        Column {
                            // 1. Render Image if available without internal padding
                            if (message.messageType == MessageType.IMAGE && message.attachmentUri != null) {
                                AsyncImage(
                                    model = message.attachmentUri.toUri(),
                                    contentDescription = "Attached Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 250.dp)
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // 2. Render File Card
                            if (message.messageType == MessageType.FILE && message.attachmentUri != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.1f))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                        contentDescription = "File",
                                        tint = contentColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = message.plaintextContent.ifBlank { "Document" },
                                        color = contentColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(onClick = {
                                        saveDocumentToDownloads(context,
                                            message.attachmentUri.toUri(), message.plaintextContent)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download File",
                                            tint = contentColor
                                        )
                                    }
                                }
                            }

                            // 3. Render Text handling links and long presses
                            if (message.messageType != MessageType.FILE && message.plaintextContent.isNotBlank()) {
                                LinkifiedText(
                                    text = message.plaintextContent,
                                    textColor = contentColor,
                                    onLongPress = { showContextMenu = true }
                                )
                            }
                        }
                    }

                    // Context Menu for Deletion
                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete for me", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showContextMenu = false
                                onDelete()
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.deliveryState.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // --- FULL SCREEN IMAGE VIEWER DIALOG ---
    if (showFullImage && message.attachmentUri != null) {
        Dialog(
            onDismissRequest = { showFullImage = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = message.attachmentUri.toUri(),
                    contentDescription = "Full Screen Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Top controls overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showFullImage = false },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            saveImageToGallery(context, message.attachmentUri.toUri())
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download to Gallery",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// --- LINK PARSER & HANDLER COMPOSABLE ---
@Composable
fun LinkifiedText(
    text: String,
    textColor: Color,
    onLongPress: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedString = remember(text) {
        buildAnnotatedString {
            append(text)
            val matcher = android.util.Patterns.WEB_URL.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                val url = text.substring(start, end)

                addStyle(
                    style = SpanStyle(
                        color = textColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Bold
                    ),
                    start = start,
                    end = end
                )

                addStringAnnotation(
                    tag = "URL",
                    annotation = if (url.startsWith("http")) url else "https://$url",
                    start = start,
                    end = end
                )
            }
        }
    }

    Text(
        text = annotatedString,
        color = textColor,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .pointerInput(text) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { pos ->
                        layoutResult?.let { layout ->
                            val offset = layout.getOffsetForPosition(pos)
                            annotatedString.getStringAnnotations("URL", offset, offset)
                                .firstOrNull()?.let { annotation ->
                                    try {
                                        uriHandler.openUri(annotation.item)
                                    } catch (e: Exception) {
                                        android.util.Log.e("LinkifiedText", "Cannot open URI", e)
                                    }
                                }
                        }
                    }
                )
            },
        onTextLayout = { layoutResult = it }
    )
}

// --- HELPER FUNCTION TO SAVE IMAGE TO ANDROID GALLERY ---
fun saveImageToGallery(context: Context, sourceUri: Uri) {
    try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Reach_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Reach")
        }

        val destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (destUri != null) {
            resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(destUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.util.Log.e("ChatScreen", "Error saving image", e)
        Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
    }
}

// --- HELPER FUNCTION TO SAVE GENERIC FILES TO DOWNLOADS ---
fun saveDocumentToDownloads(context: Context, sourceUri: Uri, fileName: String) {
    try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName.ifBlank { "Reach_File_${System.currentTimeMillis()}" })
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Reach")
        }

        val destUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (destUri != null) {
            resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(destUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.util.Log.e("ChatScreen", "Error saving file", e)
        Toast.makeText(context, "Error saving file", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ChatAvatar(name: String, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatInputBar(
    onSendMessage: (String) -> Unit,
    onSendImage: (Uri) -> Unit = {},
    onSendFile: (Uri) -> Unit = {}
) {
    var textState by remember { mutableStateOf("") }

    // Detect if the keyboard is open or if the user has typed something
    val isImeVisible = WindowInsets.isImeVisible
    val hideAttachments = textState.isNotBlank() || isImeVisible

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSendImage(uri)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onSendFile(uri)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Smoothly animate the attachment buttons in and out
            AnimatedVisibility(
                visible = !hideAttachments,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach Photo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            documentPickerLauncher.launch("*/*")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach File",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            TextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)), // Changed to 24.dp to prevent pinching on multi-line text
                placeholder = { Text("Message...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        onSendMessage(textState)
                        textState = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (textState.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (textState.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}