package com.tamapoke.android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class MiniPetWidget1x1Provider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { MiniPetWidgetRenderer.update(context, manager, ids, false) }
}
class MiniPetWidget2x2Provider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { MiniPetWidgetRenderer.update(context, manager, ids, true) }
}
object MiniPetWidgetRenderer {
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        updateProvider(context, manager, MiniPetWidget1x1Provider::class.java, false)
        updateProvider(context, manager, MiniPetWidget2x2Provider::class.java, true)
    }
    private fun updateProvider(context: Context, manager: AppWidgetManager, type: Class<out AppWidgetProvider>, detailed: Boolean) {
        val ids = manager.getAppWidgetIds(ComponentName(context, type))
        if (ids.isNotEmpty()) update(context, manager, ids, detailed)
    }
    fun update(context: Context, manager: AppWidgetManager, ids: IntArray, detailed: Boolean) {
        val views = RemoteViews(context.packageName, if (detailed) R.layout.widget_mini_pet_2x2 else R.layout.widget_mini_pet_1x1)
        runCatching {
            if (PetNative.engineStatus() != "공통 C++ 엔진 연결 완료") PetNative.initialize(context.filesDir.absolutePath)
            PetNative.update()
            val pet = PetNative.state()
            val species = if (pet.egg || pet.speciesId !in 1..151) 1 else pet.speciesId
            loadPetSpriteBitmap(context, species, pet.shiny)?.let { views.setImageViewBitmap(R.id.widget_pet_image, it) }
            if (detailed) {
                views.setTextViewText(R.id.widget_pet_name, if (pet.egg) "알" else "파트너 Lv.${pet.level}")
                views.setTextViewText(R.id.widget_hunger, "배고픔  ${pet.fullness}%")
                views.setTextViewText(R.id.widget_energy, "에너지  ${pet.energy}%")
                views.setTextViewText(R.id.widget_hygiene, "청결도  ${pet.hygiene}%")
                views.setTextViewText(R.id.widget_joy, "행복도  ${pet.joy}%")
                views.setProgressBar(R.id.widget_hunger_bar, 100, pet.fullness.coerceIn(0, 100), false)
                views.setProgressBar(R.id.widget_energy_bar, 100, pet.energy.coerceIn(0, 100), false)
                views.setProgressBar(R.id.widget_hygiene_bar, 100, pet.hygiene.coerceIn(0, 100), false)
                views.setProgressBar(R.id.widget_joy_bar, 100, pet.joy.coerceIn(0, 100), false)
            } else views.setTextViewText(R.id.widget_level, if (pet.egg) "알" else "Lv.${pet.level}")
        }.onFailure { if (detailed) views.setTextViewText(R.id.widget_pet_name, "펫 정보를 불러오지 못했어요") else views.setTextViewText(R.id.widget_level, "TamaPoke") }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(context, MainActivity::class.java)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, if (detailed) 202 else 101, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_root, pending)
        ids.forEach { manager.updateAppWidget(it, views) }
    }
}