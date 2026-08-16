package com.quick36.autosolver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service(), AutomationState.StateListener {

    private lateinit var windowManager: WindowManager
    private lateinit var rootView: View
    private lateinit var toggleButton: TextView
    private lateinit var debugPanel: View
    private lateinit var statusLine: TextView
    private lateinit var packageLine: TextView
    private lateinit var questionLine: TextView
    private lateinit var answerLine: TextView
    private lateinit var params: WindowManager.LayoutParams
    private var panelVisible = false

    companion object {
        private const val CHANNEL_ID = "quick36_overlay_channel"
        private const val NOTIF_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        rootView = LayoutInflater.from(this).inflate(R.layout.overlay_widget, null)

        toggleButton = rootView.findViewById(R.id.toggleButton)
        debugPanel = rootView.findViewById(R.id.debugPanel)
        statusLine = rootView.findViewById(R.id.statusLine)
        packageLine = rootView.findViewById(R.id.packageLine)
        questionLine = rootView.findViewById(R.id.questionLine)
        answerLine = rootView.findViewById(R.id.answerLine)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40; y = 200
        }

        setupTouch()
        refreshUi()
        windowManager.addView(rootView, params)
        AutomationState.listener = this
    }

    private fun setupTouch() {
        var ix = 0; var iy = 0; var tx = 0f; var ty = 0f; var moved = false
        toggleButton.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    ix = params.x; iy = params.y; tx = e.rawX; ty = e.rawY; moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                    params.x = ix + dx; params.y = iy + dy
                    windowManager.updateViewLayout(rootView, params); true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        AutomationState.isActive = !AutomationState.isActive
                        refreshUi()
                    }
                    true
                }
                else -> false
            }
        }
        toggleButton.setOnLongClickListener {
            panelVisible = !panelVisible
            debugPanel.visibility = if (panelVisible) View.VISIBLE else View.GONE
            true
        }
    }

    override fun onStateUpdated() {
        rootView.post { refreshUi() }
    }

    private fun refreshUi() {
        val on = AutomationState.isActive
        toggleButton.text = if (on) "ON" else "OFF"
        toggleButton.setBackgroundColor(if (on) 0xCC00AA55.toInt() else 0xCC222222.toInt())
        statusLine.text = "Status: ${AutomationState.lastStatus}"
        packageLine.text = "Pkg: ${AutomationState.lastSeenPackage}"
        questionLine.text = "Q: ${AutomationState.lastQuestion}"
        answerLine.text = "A: ${AutomationState.lastAnswer}"
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Quick36 Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Quick36 AutoSolver")
            .setContentText("Floating control active")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notif)
    }

    override fun onDestroy() {
        AutomationState.listener = null
        try { windowManager.removeView(rootView) } catch (_: Exception) {}
        super.onDestroy()
    }
}
