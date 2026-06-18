package com.rhyn.reach

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.rhyn.reach.core.nearby.MeshService
import com.rhyn.reach.presentation.navigation.ReachApp
import com.rhyn.reach.ui.theme.ReachTheme
import com.rhyn.reach.core.utils.SettingsManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.net.toUri
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Define the Required Permissions based on Android Version
    private val requiredMeshPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Security.getProvider("BC") == null) {
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
        requestNotificationPermission()
        requestBatteryExemption()
        SettingsManager.init(this)

        // Handle Mesh Permissions and Service Start
        if (hasMeshPermissions()) {
            startMeshService()
        } else {
            ActivityCompat.requestPermissions(this, requiredMeshPermissions, 102)
        }

        setContent {
            val systemTheme = isSystemInDarkTheme()
            val savedTheme by SettingsManager.darkThemeFlow.collectAsState()
            val isDarkTheme = savedTheme ?: systemTheme

            // Binds the Android system bars directly to the Compose theme state,
            // preventing the OS from guessing the theme based on the legacy XML.
            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { isDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { isDarkTheme }
                )
                onDispose {}
            }

            ReachTheme(darkTheme = isDarkTheme) {
                ReachApp()
            }
        }
    }

    // Helper to check if all permissions are granted
    private fun hasMeshPermissions(): Boolean {
        return requiredMeshPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 102 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startMeshService()
        }
    }

    private fun startMeshService() {
        val serviceIntent = Intent(this, MeshService::class.java)
        serviceIntent.action = MeshService.ACTION_START
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun requestBatteryExemption() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val currentPackageName = this.packageName

        if (!powerManager.isIgnoringBatteryOptimizations(currentPackageName)) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = "package:$currentPackageName".toUri()
            }
            startActivity(intent)
        }
    }
}