package com.example.floatingruler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager

class RulerService : Service() {

    private lateinit var wm: WindowManager
    private var rulerView: RulerView? = null

    private val lp = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (rulerView == null) {
            startAsForeground()
            addRuler()
        }
        return START_NOT_STICKY
    }

    private fun addRuler() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = RulerView(this) { x, y, w, h ->
            lp.x = x; lp.y = y; lp.width = w; lp.height = h
            rulerView?.let { v -> if (v.isAttachedToWindow) wm.updateViewLayout(v, lp) }
        }
        rulerView = view
        view.relayout()          // hitung ukuran window awal
        wm.addView(view, lp)
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Penggaris", NotificationManager.IMPORTANCE_LOW)
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, RulerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Penggaris aktif")
            .setContentText("Ketuk Stop untuk menutup")
            .addAction(Notification.Action.Builder(null, "Stop", stopIntent).build())
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notif)
        }
    }

    override fun onDestroy() {
        rulerView?.let { if (it.isAttachedToWindow) wm.removeView(it) }
        rulerView = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.example.floatingruler.STOP"
        const val CHANNEL_ID = "ruler"
    }
}
