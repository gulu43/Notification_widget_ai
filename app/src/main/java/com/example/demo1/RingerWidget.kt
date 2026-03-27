package com.example.demo1

import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

class RingerWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_CYCLE_RINGER = "com.example.demo1.CYCLE_RINGER"
        const val EXTRA_TARGET_MODE = "target_mode"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentMode = audioManager.ringerMode
            val views = RemoteViews(context.packageName, R.layout.widget_ringer)

            // Setup modes and their respective views
            val config = listOf(
                Triple(AudioManager.RINGER_MODE_NORMAL, R.id.bg_ring, R.id.icon_ring),
                Triple(AudioManager.RINGER_MODE_VIBRATE, R.id.bg_vibrate, R.id.icon_vibrate),
                Triple(AudioManager.RINGER_MODE_SILENT, R.id.bg_silent, R.id.icon_silent)
            )
            
            val btnIds = listOf(R.id.btn_ring, R.id.btn_vibrate, R.id.btn_silent)

            config.forEachIndexed { index, triple ->
                val (mode, bgId, icId) = triple
                val btnId = btnIds[index]
                val isActive = currentMode == mode

                // Set alpha for button container (1.0f if active, 0.31f if inactive)
                views.setFloat(btnId, "setAlpha", if (isActive) 1.0f else 0.31f)
                
                // Set circle background visibility (255 if active, 0 if inactive)
                views.setInt(bgId, "setImageAlpha", if (isActive) 255 else 0)

                // Set icon tint (Red #E53935 if active, #8FA896 if inactive)
                val tintColor = if (isActive) {
                    Color.parseColor("#E53935")
                } else {
                    Color.parseColor("#8FA896")
                }
                views.setInt(icId, "setColorFilter", tintColor)

                // Create PendingIntent for each button
                val intent = Intent(context, RingerWidget::class.java).apply {
                    action = ACTION_CYCLE_RINGER
                    putExtra(EXTRA_TARGET_MODE, mode)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    mode, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(btnId, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CYCLE_RINGER) {
            val targetMode = intent.getIntExtra(EXTRA_TARGET_MODE, -1)
            if (targetMode != -1) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
                    val settingsIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                } else {
                    try {
                        // For Silent mode, some devices work better if we use adjustStreamVolume or set ringerMode multiple times
                        if (targetMode == AudioManager.RINGER_MODE_NORMAL) {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING), 0)
                            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION), 0)
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM), 0)
                        } else {
                            // Set Ringer Mode first
                            audioManager.ringerMode = targetMode
                            
                            // Then explicitly mute streams to ensure "Silent" doesn't just "Vibrate"
                            val volumeValue = 0
                            audioManager.setStreamVolume(AudioManager.STREAM_RING, volumeValue, 0)
                            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volumeValue, 0)
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, volumeValue, 0)
                            
                            // Double check ringer mode for Silent
                            if (targetMode == AudioManager.RINGER_MODE_SILENT) {
                                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                            }
                        }
                    } catch (e: SecurityException) {
                        val settingsIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(settingsIntent)
                    }
                }

                // Refresh all widget instances
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, RingerWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }
}