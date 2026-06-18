package com.rhyn.reach.presentation.feature.chat

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhyn.reach.core.nearby.MeshService
import com.rhyn.reach.data.local.LocalMessageEntity
import com.rhyn.reach.data.local.LocalUserEntity
import com.rhyn.reach.data.remote.model.UserResponse
import com.rhyn.reach.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val tag = "ChatViewModel"

    private val _messages = MutableStateFlow<List<LocalMessageEntity>>(emptyList())
    val messages: StateFlow<List<LocalMessageEntity>> = _messages.asStateFlow()
    private var currentChatPartnerId: String? = null
    private var messageObservationJob: Job? = null

    private val _targetUser = MutableStateFlow<UserResponse?>(null)
    val targetUser: StateFlow<UserResponse?> = _targetUser.asStateFlow()

    // --- DYNAMIC SESSION FLOWS ---
    private val _recentThreads = MutableStateFlow<List<LocalMessageEntity>>(emptyList())
    val recentThreads: StateFlow<List<LocalMessageEntity>> = _recentThreads.asStateFlow()

    private val _userMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val userMap: StateFlow<Map<String, String>> = _userMap.asStateFlow()

    private val _selectableUsers = MutableStateFlow<List<LocalUserEntity>>(emptyList())
    val selectableUsers: StateFlow<List<LocalUserEntity>> = _selectableUsers.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var sessionJob: Job? = null
    private var cloudSyncJob: Job? = null

    fun initializeApp(context: Context) {
        // Direct repository check to see if we should start the mesh service
        if (repository.isUserLoggedIn()) {
            loadUserSession()

            viewModelScope.launch {
                val serviceIntent = Intent(context, MeshService::class.java)
                serviceIntent.action = MeshService.ACTION_START
                ContextCompat.startForegroundService(context, serviceIntent)

                if (cloudSyncJob?.isActive != true) {
                    cloudSyncJob = launch {
                        repository.connectAndListenToCloud()
                    }
                }
            }
        }
    }

    fun loadUserSession() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            launch {
                repository.getRecentThreads().collect { _recentThreads.value = it }
            }
            launch {
                repository.getAllUsers().collect { users ->
                    _userMap.value = users.associate { it.userId to it.username }
                }
            }
            launch {
                repository.getOnlyRealUsers().collect { users ->
                    val myId = repository.getCurrentUserId()
                    _selectableUsers.value = users.filter { it.userId != myId }
                }
            }
            launch {
                repository.getTotalUnreadCount().collect { count ->
                    _unreadCount.value = count
                }
            }
        }
    }

    fun setChatPartner(userId: String) {
        if (currentChatPartnerId == userId) return

        currentChatPartnerId = userId
        observeMessages(userId)

        viewModelScope.launch {
            repository.syncUserPublicKey(userId)
            repository.markThreadAsRead(userId)
        }
    }

    fun clearChatPartner() {
        currentChatPartnerId = null
        repository.currentActiveThreadId = null
        messageObservationJob?.cancel()
    }

    private fun observeMessages(threadId: String) {
        messageObservationJob?.cancel()

        messageObservationJob = viewModelScope.launch {
            repository.getMessages(threadId).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(text: String) {
        val targetId = currentChatPartnerId ?: run {
            Log.w(tag, "Attempted to send text message without an active chat partner.")
            return
        }

        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                repository.sendMessage(targetId, text)
            } catch (e: Exception) {
                Log.e(tag, "Failed to send text message.", e)
            }
        }
    }

    fun sendImageMessage(uriString: String) {
        val targetId = currentChatPartnerId ?: run {
            Log.w(tag, "Attempted to send image message without an active chat partner.")
            return
        }

        if (uriString.isBlank()) return

        viewModelScope.launch {
            try {
                repository.sendImageMessage(targetId, uriString)
            } catch (e: Exception) {
                Log.e(tag, "Failed to dispatch image message to repository.", e)
            }
        }
    }

    fun sendFileMessage(uriString: String) {
        val targetId = currentChatPartnerId ?: run {
            Log.w(tag, "Attempted to send file message without an active chat partner.")
            return
        }

        if (uriString.isBlank()) return

        viewModelScope.launch {
            try {
                repository.sendFileMessage(targetId, uriString)
            } catch (e: Exception) {
                Log.e(tag, "Failed to dispatch file message to repository.", e)
            }
        }
    }

    fun searchUser(
        username: String,
        onLoading: (Boolean) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (username.isBlank()) return
        onLoading(true)

        viewModelScope.launch {
            val result = repository.lookupUser(username)
            onLoading(false)

            if (result.isSuccess) {
                val user = result.getOrNull()
                _targetUser.value = user

                if (user != null) {
                    onSuccess(user.user_id)
                }
            } else {
                _targetUser.value = null
                onError("User '$username' not found")
            }
        }
    }

    fun clearSearch() {
        _targetUser.value = null
    }

    fun createGroupChat(groupName: String, memberIds: List<String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (groupName.isBlank()) {
            onError("Group name cannot be empty")
            return
        }

        val allMembers = memberIds.toSet().toList()

        viewModelScope.launch {
            val result = repository.createGroup(groupName, allMembers)
            if (result.isSuccess) {
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to create group"
                Log.e(tag, "Group creation failed: $errorMsg")
                onError(errorMsg)
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessageLocally(messageId)
        }
    }

    fun toggleCloudBackup(targetUserId: String, enableBackup: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleCloudBackup(targetUserId, enableBackup)
            } catch (e: Exception) {
                Log.e(tag, "Failed to toggle cloud backup for user $targetUserId", e)
            }
        }
    }
}