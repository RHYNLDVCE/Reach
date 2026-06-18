package com.rhyn.reach.presentation.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Reach") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AboutSection(
                title = "Reach",
                icon = Icons.Default.Info,
                description = "Reach is an off-grid communication application that leverages BLE and WiFi Direct for decentralized mesh networking. Built for situations where internet is unavailable."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            AboutSection(
                title = "Encryption Used",
                icon = Icons.Default.Lock,
                description = "Reach uses Elliptic Curve Diffie-Hellman (ECDH) key exchange over secp256r1 to generate a shared secret, and AES-256-GCM to ensure end-to-end encryption for all messages."
            )

            Spacer(modifier = Modifier.height(16.dp))

            AboutSection(
                title = "Routing & Loop Prevention",
                icon = Icons.Default.Route,
                description = "Reach uses a controlled flooding algorithm to find nodes. It prevents loops by tracking unique message IDs and maintaining a Time-To-Live (TTL) limit on how many hops a message can make before being dropped."
            )

            Spacer(modifier = Modifier.height(16.dp))

            AboutSection(
                title = "Developers",
                icon = Icons.Default.People,
                description = "Rhayan Lodovice\nEthel Von Inrich Lawan\nRayhan Suaib"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AboutSection(title: String, icon: ImageVector, description: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
