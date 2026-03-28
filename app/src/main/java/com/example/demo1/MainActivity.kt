package com.example.demo1

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request DND access on first launch — needed for true silent on Samsung
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!notifManager.isNotificationPolicyAccessGranted) {
            startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            )
        }

        finish()
    }
}