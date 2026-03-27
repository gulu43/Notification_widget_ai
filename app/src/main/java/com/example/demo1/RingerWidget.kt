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
                val prefs = context.getSharedPreferences("ringer_prefs", Context.MODE_PRIVATE)

                try {
                    // Step 1: Save volume if currently in RING mode
                    if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                        if (currentVol > 0) {
                            prefs.edit().putInt("saved_ring_volume", currentVol).apply()
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
                        val settingsIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(settingsIntent)
                        return
                    }

                    // Set Ringer Mode
                    audioManager.ringerMode = targetMode

                    if (targetMode == AudioManager.RINGER_MODE_NORMAL) {
                        // Step 3: Restore volume
                        val savedVol = prefs.getInt("saved_ring_volume", -1)
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                        val restoreVol = if (savedVol > 0) savedVol else (maxVol * 0.6).toInt()

                        try {
                            // Step 4: Use FLAG_SHOW_UI for Samsung Android 16 on STREAM_RING
                            audioManager.setStreamVolume(AudioManager.STREAM_RING, restoreVol, AudioManager.FLAG_SHOW_UI)
                            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, restoreVol, 0)
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM), 0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        // Step 2: Mute all relevant streams
                        val streams = listOf(
                            AudioManager.STREAM_RING,
                            AudioManager.STREAM_NOTIFICATION,
                            AudioManager.STREAM_ALARM,
                            AudioManager.STREAM_SYSTEM,
                            AudioManager.STREAM_MUSIC
                        )

                        for (stream in streams) {
                            try {
                                if (stream == AudioManager.STREAM_RING) {
                                    // Step 4: Use FLAG_SHOW_UI for Samsung
                                    audioManager.setStreamVolume(stream, 0, AudioManager.FLAG_SHOW_UI)
                                } else {
                                    audioManager.setStreamVolume(stream, 0, 0)
                                }
                            } catch (e: Exception) {
                                // Skip protected streams
                            }
                        }
                        
                        // Double set for Silent to be sure
                        if (targetMode == AudioManager.RINGER_MODE_SILENT) {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                        }
                    }
                } catch (se: SecurityException) {
                    val settingsIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
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