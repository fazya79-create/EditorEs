package com.editor.es.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.editor.es.MainActivity
import com.editor.es.R
import com.termux.terminal.TerminalSession
import java.util.concurrent.atomic.AtomicInteger

class TermuxService : Service() {

    companion object {
        const val CHANNEL_ID = "terminal_sessions"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_EXIT = "com.editor.es.ACTION_EXIT"

        private val sessions = LinkedHashMap<Int, TerminalSession>()
        private val sessionCounter = AtomicInteger(0)
        var onExitRequested: (() -> Unit)? = null

        fun registerSession(context: Context, session: TerminalSession): Int {
            val id = sessionCounter.incrementAndGet()
            sessions[id] = session
            context.startForegroundService(Intent(context, TermuxService::class.java))
            return id
        }

        fun unregisterSession(context: Context, id: Int) {
            sessions.remove(id)
            if (sessions.isEmpty()) {
                context.stopService(Intent(context, TermuxService::class.java))
            }
        }

        fun createChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.session_notification_channel),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching { startForeground(NOTIFICATION_ID, buildNotification()) }
        if (intent?.action == ACTION_EXIT) {
            killAllSessions()
            Handler(Looper.getMainLooper()).post { onExitRequested?.invoke() }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun killAllSessions() {
        sessions.values.forEach { runCatching { it.finishIfRunning() } }
        sessions.clear()
    }

    private fun buildNotification(): Notification {
        val count = sessions.size
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val exitIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TermuxService::class.java).setAction(ACTION_EXIT),
            PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (count == 1) "1 active session" else "$count active sessions"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.session_notification_exit), exitIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
