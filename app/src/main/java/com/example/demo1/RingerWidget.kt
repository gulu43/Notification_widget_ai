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
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.RemoteViews

class RingerWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_CYCLE_RINGER = "com.example.demo1.CYCLE_RINGER"
        const val EXTRA_TARGET_MODE = "target_mode"

        const val PREFS_NAME = "ringer_prefs"
        const val KEY_RING_VOL = "vol_ring"
        const val KEY_NOTIF_VOL = "vol_notification"
        const val KEY_ALARM_VOL = "vol_alarm"
        const val KEY_SYSTEM_VOL = "vol_system"
        const val KEY_MEDIA_VOL = "vol_media"
        const val KEY_SAVED = "volumes_saved"

        fun saveAllVolumes(context: Context, audio: AudioManager) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_SAVED, false)) return
            
            prefs.edit()
                .putInt(KEY_RING_VOL,   audio.getStreamVolume(AudioManager.STREAM_RING))
                .putInt(KEY_NOTIF_VOL,  audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION))
                .putInt(KEY_ALARM_VOL,  audio.getStreamVolume(AudioManager.STREAM_ALARM))
                .putInt(KEY_SYSTEM_VOL, audio.getStreamVolume(AudioManager.STREAM_SYSTEM))
                .putInt(KEY_MEDIA_VOL,  audio.getStreamVolume(AudioManager.STREAM_MUSIC))
                .putBoolean(KEY_SAVED, true)
                .apply()
        }

        fun restoreAllVolumes(context: Context, audio: AudioManager) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            fun getOrDefault(key: String, stream: Int): Int {
                val saved = prefs.getInt(key, -1)
                return if (saved > 0) saved else (audio.getStreamMaxVolume(stream) * 0.6).toInt()
            }
            
            try { audio.setStreamVolume(AudioManager.STREAM_RING, getOrDefault(KEY_RING_VOL, AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI) } catch (e: Exception) {}
            try { audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, getOrDefault(KEY_NOTIF_VOL, AudioManager.STREAM_NOTIFICATION), 0) } catch (e: Exception) {}
            try { audio.setStreamVolume(AudioManager.STREAM_ALARM, getOrDefault(KEY_ALARM_VOL, AudioManager.STREAM_ALARM), 0) } catch (e: Exception) {}
            try { audio.setStreamVolume(AudioManager.STREAM_SYSTEM, getOrDefault(KEY_SYSTEM_VOL, AudioManager.STREAM_SYSTEM), 0) } catch (e: Exception) {}
            
            prefs.edit().putBoolean(KEY_SAVED, false).apply()
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, forcedMode: Int? = null) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val mode = forcedMode ?: audioManager.ringerMode
            val views = RemoteViews(context.packageName, R.layout.widget_ringer)

            // ---- RING is active ----
            if (mode == AudioManager.RINGER_MODE_NORMAL) {
                // Ring active
                views.setInt(R.id.bg_ring,    "setImageAlpha", 255)
                views.setInt(R.id.bg_ring,    "setColorFilter", Color.parseColor("#FFFFFF"))
                views.setInt(R.id.icon_ring,  "setColorFilter", Color.parseColor("#E53935"))

                // Vibrate inactive
                views.setInt(R.id.bg_vibrate,   "setImageAlpha", 0)
                views.setInt(R.id.icon_vibrate, "setColorFilter", Color.parseColor("#7A7A7A"))

                // Silent inactive
                views.setInt(R.id.bg_silent,   "setImageAlpha", 0)
                views.setInt(R.id.icon_silent, "setColorFilter", Color.parseColor("#7A7A7A"))
            }

            // ---- VIBRATE is active ----
            if (mode == AudioManager.RINGER_MODE_VIBRATE) {
                // Vibrate active
                views.setInt(R.id.bg_vibrate,   "setImageAlpha", 255)
                views.setInt(R.id.bg_vibrate,   "setColorFilter", Color.parseColor("#FFFFFF"))
                views.setInt(R.id.icon_vibrate, "setColorFilter", Color.parseColor("#E53935"))

                // Ring inactive
                views.setInt(R.id.bg_ring,   "setImageAlpha", 0)
                views.setInt(R.id.icon_ring, "setColorFilter", Color.parseColor("#7A7A7A"))

                // Silent inactive
                views.setInt(R.id.bg_silent,   "setImageAlpha", 0)
                views.setInt(R.id.icon_silent, "setColorFilter", Color.parseColor("#7A7A7A"))
            }

            // ---- SILENT is active ----
            if (mode == AudioManager.RINGER_MODE_SILENT) {
                // Silent active
                views.setInt(R.id.bg_silent,   "setImageAlpha", 255)
                views.setInt(R.id.bg_silent,   "setColorFilter", Color.parseColor("#FFFFFF"))
                views.setInt(R.id.icon_silent, "setColorFilter", Color.parseColor("#E53935"))

                // Ring inactive
                views.setInt(R.id.bg_ring,   "setImageAlpha", 0)
                views.setInt(R.id.icon_ring, "setColorFilter", Color.parseColor("#7A7A7A"))

                // Vibrate inactive
                views.setInt(R.id.bg_vibrate,   "setImageAlpha", 0)
                views.setInt(R.id.icon_vibrate, "setColorFilter", Color.parseColor("#7A7A7A"))
            }

            val modes = listOf(AudioManager.RINGER_MODE_NORMAL, AudioManager.RINGER_MODE_VIBRATE, AudioManager.RINGER_MODE_SILENT)
            val btnIds = listOf(R.id.btn_ring, R.id.btn_vibrate, R.id.btn_silent)

            modes.forEachIndexed { index, m ->
                val intent = Intent(context, RingerWidget::class.java).apply {
                    action = ACTION_CYCLE_RINGER
                    putExtra(EXTRA_TARGET_MODE, m)
                }
                val pendingIntent = PendingIntent.getBroadcast(context, m, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(btnIds[index], pendingIntent)
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
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                try {
                    when (targetMode) {
                        AudioManager.RINGER_MODE_NORMAL -> {
                            // Cancel silent DND if coming from silent mode
                            try {
                                if (notifManager.isNotificationPolicyAccessGranted) {
                                    notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                                }
                            } catch (e: Exception) {}

                            audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            Handler(Looper.getMainLooper()).postDelayed({
                                restoreAllVolumes(context, audio)
                                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                val savedMedia = prefs.getInt(KEY_MEDIA_VOL, -1)
                                val maxMedia = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val restoreMedia = if (savedMedia > 0) savedMedia else (maxMedia * 0.6).toInt()
                                try {
                                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, restoreMedia, 0)
                                } catch (e: Exception) {}
                            }, 300)
                        }
                        AudioManager.RINGER_MODE_VIBRATE -> {
                            // Cancel silent DND if coming from silent mode
                            try {
                                if (notifManager.isNotificationPolicyAccessGranted) {
                                    notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                                }
                            } catch (e: Exception) {}

                            saveAllVolumes(context, audio)
                            audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                            Handler(Looper.getMainLooper()).postDelayed({
                                try { audio.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_SHOW_UI) } catch (e: Exception) {}
                                try { audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0) } catch (e: Exception) {}
                                try { audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0) } catch (e: Exception) {}
                                try { audio.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0) } catch (e: Exception) {}
                                try { audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0) } catch (e: Exception) {}
                            }, 300)
                        }
                        AudioManager.RINGER_MODE_SILENT -> {
                            // Samsung Android 16 fix — must use INTERRUPTION_FILTER_NONE
                            // RINGER_MODE_SILENT alone does not stop vibration on Samsung
                            if (notifManager.isNotificationPolicyAccessGranted) {
                                // This is the ONLY thing that stops vibration on Samsung Android 16
                                notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                            } else {
                                // Permission not granted — send user to grant it
                                val dndIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                dndIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(dndIntent)
                                // Do not proceed until permission granted
                                return
                            }

                            saveAllVolumes(context, audio)
                            audio.ringerMode = AudioManager.RINGER_MODE_SILENT
                            Handler(Looper.getMainLooper()).postDelayed({
                                try { audio.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_SHOW_UI) } catch (e: Exception) {}
                                try { audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0) } catch (e: Exception) {}
                                try { audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0) } catch (e: Exception) {}
                                try { audio.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0) } catch (e: Exception) {}
                                try { audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0) } catch (e: Exception) {}
                                try { audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, 0) } catch (e: Exception) {}

                                Handler(Looper.getMainLooper()).postDelayed({
                                    try { audio.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_SHOW_UI) } catch (e: Exception) {}
                                    try { audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0) } catch (e: Exception) {}
                                }, 500)
                            }, 300)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }

                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, RingerWidget::class.java))
                for (id in ids) {
                    updateWidget(context, manager, id, forcedMode = targetMode)
                }
            }
        }
    }
}