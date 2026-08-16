package com.quick36.autosolver

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Simple launcher screen. Its only job is to send the user to the
 * system Accessibility Settings page so they can enable
 * SolverAccessibilityService. There is no in-app UI needed beyond that —
 * all the real work happens in the background service.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val openSettingsBtn = findViewById<Button>(R.id.openSettingsButton)

        statusText.text = if (isAccessibilityServiceEnabled()) {
            "Accessibility Service: ENABLED ✓"
        } else {
            "Accessibility Service: NOT enabled"
        }

        openSettingsBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.statusText).text = if (isAccessibilityServiceEnabled()) {
            "Accessibility Service: ENABLED ✓"
        } else {
            "Accessibility Service: NOT enabled"
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
