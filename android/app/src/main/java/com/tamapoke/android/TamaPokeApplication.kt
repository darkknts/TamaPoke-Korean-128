package com.tamapoke.android

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class TamaPokeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PetCareNotifications.ensureChannel(this)
        val refresh = PeriodicWorkRequestBuilder<MiniPetWidgetRefreshWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            refresh
        )
    }

    private companion object {
        const val WORK_NAME = "tamapoke_mini_pet_widget_refresh"
    }
}
