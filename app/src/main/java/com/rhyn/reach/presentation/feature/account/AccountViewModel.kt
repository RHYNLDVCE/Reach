package com.rhyn.reach.presentation.feature.account

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhyn.reach.core.nearby.MeshService
import com.rhyn.reach.data.local.prefs.SessionManager
import com.rhyn.reach.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AccountUiState(
    val username: String = "",
    val userId: String = "",
    val publicKey: String = "",
    val qrPayload: String = "",
    val isBackupEnabled: Boolean = false
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: ChatRepository // Injected to handle deep account operations
) : ViewModel() {

    private val tag = "AccountViewModel"

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        loadIdentity()
    }

    private fun loadIdentity() {
        val id = sessionManager.getUserId() ?: "UNKNOWN_ID"
        val name = sessionManager.getUsername() ?: "Unknown User"
        val pubKey = sessionManager.getPublicKey() ?: "UNKNOWN_KEY"

        // Check sessionManager for existing backup status
        val backupStatus = sessionManager.isCloudSynced()

        val payload = "reach://contact?id=$id&name=$name&key=$pubKey"

        _uiState.update {
            it.copy(
                username = name,
                userId = id,
                publicKey = pubKey,
                qrPayload = payload,
                isBackupEnabled = backupStatus
            )
        }
    }

    /**
     * Secures the local identity by encrypting the private key with a PIN
     * and backing it up to the backend alongside their Google Auth Token.
     */
    fun enableCloudBackup(
        idToken: String,
        pin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (pin.isBlank() || pin.length != 6) {
            onError("Please enter a valid 6-digit PIN.")
            return
        }

        viewModelScope.launch {
            try {
                // Note: You will need to implement backupIdentityToCloud inside your ChatRepository.
                // This function should handle the key encryption and call the /backup-identity API.
                val result = repository.backupIdentityToCloud(idToken, pin)

                if (result.isSuccess) {
                    sessionManager.setCloudSynced(true)
                    _uiState.update { it.copy(isBackupEnabled = true) }

                    // Automatically connect to the cloud relay if the backup succeeds
                    launch { repository.connectAndListenToCloud() }

                    onSuccess()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to backup account."
                    Log.e(tag, "Manual cloud backup failed: $errorMsg")
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception during cloud backup", e)
                onError("An unexpected error occurred during backup.")
            }
        }
    }

    /**
     * Toggles local settings for cloud sync (if the user already backed up their account
     * but wants to temporarily stop syncing messages to the cloud relay).
     */
    fun toggleBackup(enabled: Boolean) {
        sessionManager.setCloudSynced(enabled)
        _uiState.update { it.copy(isBackupEnabled = enabled) }

        // If disabled, you might also want to disconnect the websocket here via repository
    }

    /**
     * Completely wipes the local identity and halts all background mesh communications.
     */
    fun logout(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Stop the background mesh service securely
                val serviceIntent = Intent(context, MeshService::class.java)
                context.stopService(serviceIntent)

                // 2. Clear repository, keys, and database
                repository.logout()

                // 3. Clear local UI state
                _uiState.value = AccountUiState()

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error occurred during logout process.", e)
                // Force complete to ensure the user isn't trapped on a broken screen
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}