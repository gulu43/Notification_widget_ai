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
            val currentMode = forcedMode ?: audioManager.ringerMode
            val views = RemoteViews(context.packageName, R.layout.widget_ringer)

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

                if (isActive) {
                    // ACTIVE button — darker semi-transparent circle + full white icon
                    views.setInt(icId, "setColorFilter", Color.parseColor("#FFFFFF"))
                    views.setInt(bgId, "setImageAlpha", 255)
                    views.setInt(bgId, "setColorFilter", Color.parseColor("#88FFFFFF"))
                } else {
                    // INACTIVE buttons — dimmed icon + circle fully hidden
                    views.setInt(icId, "setColorFilter", Color.parseColor("#66FFFFFF"))
                    views.setInt(bgId, "setImageAlpha", 0)
                    views.setInt(bgId, "setColorFilter", Color.parseColor("#00000000"))
                }

                val intent = Intent(context, RingerWidget::class.java).apply {
                    action = ACTION_CYCLE_RINGER
                    putExtra(EXTRA_TARGET_MODE, mode)
                }
                val pendingIntent = PendingIntent.getBroadcast(context, mode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
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
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

                try {
                    when (targetMode) {
                        AudioManager.RINGER_MODE_NORMAL -> {
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