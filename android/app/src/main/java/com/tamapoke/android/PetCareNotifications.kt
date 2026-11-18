package com.tamapoke.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Low-frequency care reminders while TamaPoke is in the background.
 * WorkManager runs the check; Android may defer it under battery restrictions.
 */
object PetCareNotifications {
    private const val CHANNEL_ID = "pet_care_reminders"
    private const val PREFS = "tamapoke_care_notifications"
    private const val COOLDOWN_SECONDS = 6 * 60 * 60L

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "포켓몬 돌봄 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "배고픔, 에너지, 청결도, 행복도가 낮을 때 알려드려요."
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun checkAndNotify(context: Context, pet: PetState) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val now = System.currentTimeMillis() / 1000
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val alerts = listOf(
            CareAlert("hunger", pet.fullness, 30, 1101, "🍓 배가 고파요!", "먹이를 주면 기분이 좋아질 거예요."),
            CareAlert("energy", pet.energy, 20, 1102, "⚡ 졸려요!", "잠깐 재워서 에너지를 회복시켜 주세요."),
            CareAlert("hygiene", pet.hygiene, 30, 1103, "🫧 씻고 싶어요!", "청소해서 깨끗하게 해 주세요."),
            CareAlert("joy", pet.joy, 30, 1104, "💗 놀고 싶어요!", "함께 놀아 주면 행복도가 올라가요.")
        )
        alerts.forEach { alert ->
            if (alert.value > alert.threshold) return@forEach
            val lastSent = prefs.getLong("last_${alert.key}", 0L)
            if (now - lastSent < COOLDOWN_SECONDS) return@forEach

            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(context, MainActivity::class.java)
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pending = PendingIntent.getActivity(
                context, alert.notificationId, launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(context.applicationInfo.icon)
                .setContentTitle(alert.title)
                .setContentText(alert.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            runCatching {
                NotificationManagerCompat.from(context).notify(alert.notificationId, notification)
                prefs.edit().putLong("last_${alert.key}", now).apply()
            }
        }
    }

    private data class CareAlert(
        val key: String,
        val value: Int,
        val threshold: Int,
        val notificationId: Int,
        val title: String,
        val message: String
    )
}
