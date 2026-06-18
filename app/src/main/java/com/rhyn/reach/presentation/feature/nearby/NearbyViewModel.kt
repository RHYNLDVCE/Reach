package com.rhyn.reach.presentation.feature.nearby

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhyn.reach.core.nearby.MeshService
import com.rhyn.reach.data.local.LocalUserEntity
import com.rhyn.reach.data.local.dao.UserDao
import com.rhyn.reach.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import androidx.core.net.toUri

// 1. ADDED connectionType so the UI knows HOW we found them
data class NearbyPeer(
    val endpointId: String,
    val stableUserId: String,
    val username: String,
    val signalStrength: Int,
    val connectionType: String
)

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val nearbyManager: NearbyManager,
    private val lanManager: LanManager, // <-- NEW: Inject LanManager
    private val repository: ChatRepository,
    private val userDao: UserDao
) : ViewModel() {

    val isScanning: StateFlow<Boolean> = nearbyManager.isMeshActive.asStateFlow()
    val hasRadioError: StateFlow<Boolean> = nearbyManager.hasRadioError.asStateFlow() // ---> NEW
    val activeConnectionCount: StateFlow<Int> = nearbyManager.activeConnectionCount.asStateFlow() // Expose to UI

    // 2. COMBINE both Bluetooth/Direct peers and LAN peers into one list
    val discoveredPeers: StateFlow<List<NearbyPeer>> = combine(
        nearbyManager.discoveredPeers,
        lanManager.discoveredLanPeers
    ) { nearbyList, lanMap ->
        // Use a Map so we can easily look up if a user is already in the list
        val peerMap = mutableMapOf<String, NearbyPeer>()

        // A. Add Bluetooth/Mesh Peers First
        nearbyList.forEach { device ->
            peerMap[device.stableUserId] = NearbyPeer(
                endpointId = device.endpointId,
                stableUserId = device.stableUserId,
                username = device.username,
                signalStrength = 4,
                connectionType = "Bluetooth Mesh"
            )
        }

        // B. Evaluate Local Wi-Fi Peers
        lanMap.values.forEach { lanPeer ->
            val existingPeer = peerMap[lanPeer.stableUserId]

            if (existingPeer != null) {
                // They are already found via Bluetooth! Update the label to show BOTH.
                peerMap[lanPeer.stableUserId] = existingPeer.copy(
                    connectionType = "LAN & Bluetooth"
                )
            } else {
                // They are ONLY found on LAN.
                peerMap[lanPeer.stableUserId] = NearbyPeer(
                    endpointId = lanPeer.ipAddress, // Use IP as the "endpointId" for LAN
                    stableUserId = lanPeer.stableUserId,
                    username = lanPeer.username,
                    signalStrength = 4,
                    connectionType = "Local Wi-Fi"
                )
            }
        }

        peerMap.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleScanning(context: Context, hasPermission: Boolean) {
        if (!hasPermission) return

        val serviceIntent = Intent(context, MeshService::class.java)

        if (isScanning.value) {
            serviceIntent.action = MeshService.ACTION_STOP
            context.startService(serviceIntent)
        } else {
            serviceIntent.action = MeshService.ACTION_START
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    fun connectToPeer(endpointId: String, stableUserId: String, username: String) {
        val myName = repository.getCurrentUsername() ?: "Guest"
        val myId = repository.getCurrentUserId() ?: UUID.randomUUID().toString()

        viewModelScope.launch {
            // --- THE FIX: Check if the user already exists to prevent wiping their keys ---
            val existingUser = userDao.getUserById(stableUserId, myId)
            
            if (existingUser == null) {
                // User doesn't exist at all, safe to insert a new blank record
                val tempUser = LocalUserEntity(
                    userId = stableUserId,
                    ownerId = myId,
                    username = username,
                    isGroup = false,
                    publicKey = null
                )
                userDao.insertUser(tempUser)
            } else if (existingUser.username.startsWith("Unknown")) {
                // User exists but has no name. Update the name but preserve their keys!
                userDao.insertUser(existingUser.copy(username = username))
            }

            // If it's a Bluetooth mesh endpoint, we request a connection.
            // If it's an IP address (LAN), we don't need to "connect" because HTTP is stateless!
            if (!endpointId.contains(".")) {
                nearbyManager.requestConnection(endpointId, "$myId|$myName")
            }
        }
    }

    suspend fun handleScannedQrCode(qrData: String): String? {
        return try {
            val uri = qrData.toUri()

            // Validate that this is our specific app's QR format
            if (uri.scheme == "reach" && uri.host == "contact") {
                val targetId = uri.getQueryParameter("id") ?: return null
                val targetName = uri.getQueryParameter("name") ?: "Unknown User"
                val targetKey = uri.getQueryParameter("key") ?: ""

                val myId = repository.getCurrentUserId() ?: return null

                // Save their cryptographic identity directly to the local database!
                // No internet required to establish trust.
                val newUser = LocalUserEntity(
                    userId = targetId,
                    ownerId = myId,
                    username = targetName,
                    isGroup = false,
                    publicKey = targetKey
                )
                userDao.insertUser(newUser)

                return targetId
            }
            null
        } catch (e: Exception) {
            Log.e("NearbyViewModel", "Failed to parse QR Code", e)
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        nearbyManager.stopAll()
        lanManager.stopAll()
    }
}