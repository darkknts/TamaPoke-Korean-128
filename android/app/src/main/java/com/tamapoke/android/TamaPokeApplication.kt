package com.tamapoke.android

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class TamaPokeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Background reminders must never prevent the main game from opening.
        // Keep all optional notification/WorkManager setup isolated from startup.
        runCatching { PetCareNotifications.ensureChannel(this) }
        runCatching {
            val refresh = PeriodicWorkRequestBuilder<MiniPetWidgetRefreshWorker>(
                15, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                refresh
            )
        }
    }

    private companion object {
        const val WORK_NAME = "tamapoke_mini_pet_widget_refresh"
    }
}
