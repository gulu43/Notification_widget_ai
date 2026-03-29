package com.example.demo1

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Info button — set AFTER setContentView
        val btnInfo = findViewById<ImageButton>(R.id.btn_info)
        btnInfo.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        checkAndShowPermission()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission status every time user comes back from settings
        updatePermissionStatus()
    }

    private fun checkAndShowPermission() {
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!notifManager.isNotificationPolicyAccessGranted) {
            // Show explanation dialog before sending to settings
            DndPermissionDialog().show(supportFragmentManager, "dnd_dialog")
        }
        updatePermissionStatus()
    }

    fun openDndSettings() {
        startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    fun updatePermissionStatus() {
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val granted = notifManager.isNotificationPolicyAccessGranted

        val statusIcon = findViewById<ImageView>(R.id.status_icon)
        val statusText = findViewById<TextView>(R.id.status_text)
        val permButton = findViewById<Button>(R.id.btn_grant_permission)

        if (granted) {
            statusIcon.setImageResource(R.drawable.ic_check_circle)
            statusIcon.setColorFilter(
                android.graphics.Color.parseColor("#4CAF50")
            )
            statusText.text = "All permissions granted"
            statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            permButton.visibility = android.view.View.GONE
        } else {
            statusIcon.setImageResource(R.drawable.ic_warning)
            statusIcon.setColorFilter(
                android.graphics.Color.parseColor("#FF9800")
            )
            statusText.text = "Do Not Disturb permission needed"
            statusText.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            permButton.visibility = android.view.View.VISIBLE
            permButton.setOnClickListener {
                DndPermissionDialog().show(supportFragmentManager, "dnd_dialog")
            }
        }
    }
}