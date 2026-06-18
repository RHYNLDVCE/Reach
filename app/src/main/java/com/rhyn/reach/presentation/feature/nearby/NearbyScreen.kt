package com.rhyn.reach.presentation.feature.nearby

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.rhyn.reach.presentation.feature.chat.UserAvatar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    viewModel: NearbyViewModel = hiltViewModel(),
    onNavigateToChat: (String) -> Unit
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val hasRadioError by viewModel.hasRadioError.collectAsState() // ---> NEW
    val activeLinks by viewModel.activeConnectionCount.collectAsState() // Observe links
    val peers by viewModel.discoveredPeers.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. SMART PERMISSIONS
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION, // <-- ADDED
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION // <-- ADDED
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION // <-- ALREADY HERE, KEEP IT
            )
        }
    }

    // 2. 🚀 THE LAUNCHERS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.toggleScanning(context, true)
        }
    }

    // NEW: QR Scanner Launcher (Handles its own camera permissions natively)
    val qrScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            coroutineScope.launch {
                val targetUserId = viewModel.handleScannedQrCode(result.contents)
                if (targetUserId != null) {
                    onNavigateToChat(targetUserId)
                } else {
                    Toast.makeText(context, "Invalid or unrecognized QR Code", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Reach", fontWeight = FontWeight.SemiBold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    // NEW: QR Scan Action Button
                    IconButton(
                        onClick = {
                            val options = ScanOptions()
                            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            options.setPrompt("Scan a Reach QR Code")
                            options.setBeepEnabled(false)

                            // ---> ADD THESE TWO LINES <---
                            options.setCaptureActivity(com.rhyn.reach.core.utils.PortraitCaptureActivity::class.java)
                            options.setOrientationLocked(true)

                            qrScannerLauncher.launch(options)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR Code",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Existing Radar Start/Stop Button
                    Button(
                        onClick = {
                            val hasAllPermissions = requiredPermissions.all {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }

                            if (hasAllPermissions) {
                                viewModel.toggleScanning(context, true)
                            } else {
                                permissionLauncher.launch(requiredPermissions)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            contentColor = if (isScanning) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(end = 16.dp, start = 8.dp)
                    ) {
                        Text(if (isScanning) "Stop" else "Scan")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ---> NEW: The Friendly Hardware Warning Banner <---
            if (hasRadioError && !isScanning) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = "Bluetooth Error",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Your phone's Bluetooth antenna needs a quick breather. To fix this, try toggling your Bluetooth off and on, or restarting your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // --- 1. The Radar Animation Area ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    RadarAnimation()
                } else {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = "Idle Radar",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            if (isScanning) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Direct Links",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "$activeLinks",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // --- 2. Discovered Peers List ---
            Text(
                text = "DISCOVERED PEERS (${peers.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(peers) { peer ->
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.connectToPeer(peer.endpointId, peer.stableUserId, peer.username)
                                onNavigateToChat(peer.stableUserId)
                            },
                        leadingContent = { UserAvatar(name = peer.username) },
                        headlineContent = {
                            Text(peer.username, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        },
                        supportingContent = {
                            Text(peer.connectionType, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (peer.connectionType.contains("LAN") || peer.connectionType.contains("Wi-Fi")) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = "LAN Signal",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = if (peer.connectionType == "LAN & Bluetooth") 8.dp else 0.dp)
                                    )
                                }
                                if (peer.connectionType.contains("Bluetooth")) {
                                    Icon(
                                        imageVector = Icons.Default.CellTower,
                                        contentDescription = "Bluetooth Signal",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                }

                if (peers.isEmpty() && !isScanning) {
                    item {
                        Text(
                            text = "Tap 'Scan' to find devices, or use the QR Scanner icon top right to connect manually.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
// Custom pulsing radar animation using Jetpack Compose graphics
@Composable
fun RadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    // Animate scale from 1x to 3x
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "radar_scale"
    )

    // Animate alpha from 100% to 0% (fading out as it expands)
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "radar_alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // The expanding ring
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
        )
        // The solid inner circle
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiTethering,
                contentDescription = "Scanning",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}