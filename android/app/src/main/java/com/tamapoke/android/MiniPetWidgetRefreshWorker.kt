package com.tamapoke.android

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Refreshes both home-screen pet widgets while the game UI is not open.
 * WorkManager may defer execution to honor Android battery and background limits.
 */
class MiniPetWidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result = try {
        MiniPetWidgetRenderer.updateAll(applicationContext)
        Result.success()
    } catch (_: Throwable) {
        Result.retry()
    }
}
