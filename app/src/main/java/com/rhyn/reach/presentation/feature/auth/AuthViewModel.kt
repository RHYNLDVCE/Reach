package com.rhyn.reach.presentation.feature.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhyn.reach.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val tag = "AuthViewModel"

    // --- UI State for Google Sign-In PIN Flow ---
    var pendingGoogleIdToken by mutableStateOf<String?>(null)
        private set

    var showPinDialog by mutableStateOf(false)
        private set

    // ---------------------------------------------

    /**
     * OFFLINE PATH: Generates keys locally and saves them to the device.
     */
    fun authenticateOffline(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Assuming you update your repository to handle a PIN-less offline creation
                // For now, we pass an empty string or a default offline PIN to satisfy the existing repo signature
                val defaultOfflinePin = "000000"

                // Attempt to login first, if it fails, create the local account
                var result = repository.login(username, password, defaultOfflinePin)

                if (!result.isSuccess) {
                    result = repository.createLocalAccount(username, password, defaultOfflinePin)
                }

                if (result.isSuccess) {
                    onSuccess()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Authentication failed"
                    Log.w(tag, "Offline Auth failed: $errorMsg")
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception during offline auth", e)
                onError("An unexpected error occurred.")
            }
        }
    }

    /**
     * ONLINE PATH - Step 1: User signed in via Google, now pause and ask for the E2EE PIN.
     */
    fun onGoogleSignInSuccess(idToken: String) {
        pendingGoogleIdToken = idToken
        showPinDialog = true
    }

    /**
     * Cancels the Google Sign-In PIN flow.
     */
    fun dismissPinDialog() {
        showPinDialog = false
        pendingGoogleIdToken = null
    }

    /**
     * ONLINE PATH - Step 2: Submits the token and the PIN to the repository.
     */
    fun submitGoogleAuthWithPin(
        pin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val token = pendingGoogleIdToken
        if (token == null) {
            onError("Google token is missing.")
            return
        }

        viewModelScope.launch {
            try {
                // Call the REAL backend route via your updated repository signature
                val result = repository.authenticateWithGoogle(token, pin)

                if (result.isSuccess) {
                    showPinDialog = false
                    onSuccess()
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Invalid PIN or Backend Error"
                    Log.w(tag, "Google Auth Repository Error: $msg")
                    onError(msg)
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception during Google auth routing", e)
                onError("Failed to communicate with backend server.")
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return repository.isUserLoggedIn()
    }
}