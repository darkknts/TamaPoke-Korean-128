package com.tamapoke.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import java.util.concurrent.TimeUnit

class MiniPetService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: TextView? = null
    private var windowManager: WindowManager? = null
    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    private val refreshTask = object : Runnable {
        override fun run() {
            runCatching {
                PetNative.update()
                val pet = PetNative.state()
                updateOverlay(pet)
                notifyImportantStatus(pet)
            }
            handler.postDelayed(this, REFRESH_INTERVAL)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        if (Settings.canDrawOverlays(this)) attachOverlay()
        handler.post(refreshTask)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            prefs.edit().putBoolean(KEY_ENABLED, false).apply()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            prefs.edit().putBoolean(KEY_ENABLED, false).apply()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        detachOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun attachOverlay() {
        if (overlay != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = TextView(this).apply {
            text = "🐾"
            textSize = 25f
            gravity = Gravity.CENTER
            setPadding(dp(11), dp(5), dp(11), dp(5))
            setTextColor(0xFF49375F.toInt())
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(0xEEF7F0FF.toInt())
            contentDescription = "TamaPoke 미니펫"
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage(packageName)
                launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                if (launch != null) startActivity(launch)
            }
        }
        val statusBarId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (statusBarId > 0) resources.getDimensionPixelSize(statusBarId) else dp(24)
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeight + dp(3)
        }
        runCatching { windowManager?.addView(view, params); overlay = view }
    }

    private fun updateOverlay(pet: PetState) {
        val view = overlay ?: return
        view.text = "🐾  Lv.${pet.level}"
        view.alpha = if (pet.sleeping) 0.72f else 1f
    }

    private fun notifyImportantStatus(pet: PetState) {
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_ALERT, 0L)
        if (now - last < ALERT_COOLDOWN) return
        val alerts = mutableListOf<String>()
        if (pet.fullness <= 15) alerts += "배고픔이 심해요"
        if (pet.energy <= 15) alerts += "에너지가 부족해요"
        if (pet.hygiene <= 15 || pet.poops > 0) alerts += "청결 상태를 확인해 주세요"
        if (pet.joy <= 15) alerts += "기분이 많이 낮아졌어요"
        if (alerts.isEmpty()) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(ALERT_NOTIFICATION_ID, Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("TamaPoke 돌봄 알림")
            .setContentText(alerts.joinToString(" · "))
            .setStyle(Notification.BigTextStyle().bigText(alerts.joinToString("\n")))
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .build())
        prefs.edit().putLong(KEY_LAST_ALERT, now).apply()
    }

    private fun buildForegroundNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("TamaPoke 미니펫 실행 중")
            .setContentText("화면 위쪽에 미니펫을 표시하고 중요한 돌봄 상태만 알려줍니다.")
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent())
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "미니펫 끄기",
                PendingIntent.getService(this, 3, Intent(this, MiniPetService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            .build()

    private fun openAppPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "미니펫 및 중요 돌봄 알림", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    private fun detachOverlay() {
        overlay?.let { view -> runCatching { windowManager?.removeView(view) } }
        overlay = null
        windowManager = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PREFS = "tamapoke_settings"
        const val KEY_ENABLED = "mini_pet_enabled"
        private const val KEY_LAST_ALERT = "mini_pet_last_alert"
        private const val ACTION_STOP = "com.tamapoke.android.MINI_PET_STOP"
        private const val CHANNEL_ID = "tamapoke_mini_pet"
        private const val NOTIFICATION_ID = 41
        private const val ALERT_NOTIFICATION_ID = 42
        private const val REFRESH_INTERVAL = 60_000L
        private val ALERT_COOLDOWN = TimeUnit.HOURS.toMillis(6)

        fun start(context: Context) {
            val intent = Intent(context, MiniPetService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MiniPetService::class.java))
        }
    }
}
