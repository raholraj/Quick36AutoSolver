package com.quick36.autosolver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Simple launcher screen. Its jobs:
 *  1. Send the user to the system Accessibility Settings page to enable
 *     SolverAccessibilityService.
 *  2. Request the "draw over other apps" permission and start OverlayService,
 *     which shows the floating Start/Stop button + live debug panel.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val openSettingsBtn = findViewById<Button>(R.id.openSettingsBtn)
        val enableOverlayBtn = findViewById<Button>(R.id.enableOverlayBtn)

        openSettingsBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        enableOverlayBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                Toast.makeText(this, "Allow the overlay permission, then tap this button again", Toast.LENGTH_LONG).show()
            } else {
                startOverlay()
            }
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        // If permission was just granted in Settings, start the overlay automatically.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            startOverlay()
        }
    }

    private fun startOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun refreshStatus() {
        val accEnabled = isAccessibilityServiceEnabled()
        val overlayEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        statusText.text = buildString {
            append(if (accEnabled) "Accessibility: ENABLED ✓\n" else "Accessibility: NOT enabled\n")
            append(if (overlayEnabled) "Overlay: ENABLED ✓" else "Overlay: NOT enabled")
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${SolverAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(":").any { it.equals(expectedComponentName, ignoreCase = true) }
    }
}
