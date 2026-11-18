package com.tamapoke.android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class MiniPetWidget1x1Provider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        MiniPetWidgetRenderer.update(context, manager, ids, false)
    }
}

class MiniPetWidget2x2Provider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        MiniPetWidgetRenderer.update(context, manager, ids, true)
    }
}

object MiniPetWidgetRenderer {
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val compactIds = manager.getAppWidgetIds(ComponentName(context, MiniPetWidget1x1Provider::class.java))
        val detailedIds = manager.getAppWidgetIds(ComponentName(context, MiniPetWidget2x2Provider::class.java))
        if (compactIds.isEmpty() && detailedIds.isEmpty()) return

        // Advance the native engine once, then render both sizes from the same snapshot.
        val pet = PetNative.refreshForWidget(context.filesDir.absolutePath)
        if (compactIds.isNotEmpty()) render(context, manager, compactIds, false, pet)
        if (detailedIds.isNotEmpty()) render(context, manager, detailedIds, true, pet)
    }

    private fun updateProvider(
        context: Context,
        manager: AppWidgetManager,
        type: Class<out AppWidgetProvider>,
        detailed: Boolean,
        pet: PetState
    ) {
        val ids = manager.getAppWidgetIds(ComponentName(context, type))
        if (ids.isNotEmpty()) render(context, manager, ids, detailed, pet)
    }

    fun update(context: Context, manager: AppWidgetManager, ids: IntArray, detailed: Boolean) {
        val pet = PetNative.refreshForWidget(context.filesDir.absolutePath)
        render(context, manager, ids, detailed, pet)
    }

    private fun render(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        detailed: Boolean,
        pet: PetState
    ) {
        val views = RemoteViews(
            context.packageName,
            if (detailed) R.layout.widget_mini_pet_2x2 else R.layout.widget_mini_pet_1x1
        )
        runCatching {
            val species = if (pet.egg || pet.speciesId !in 1..151) 1 else pet.speciesId
            loadPetSpriteBitmap(context, species, pet.shiny)?.let {
                views.setImageViewBitmap(R.id.widget_pet_image, it)
            }
            val needsCare = minOf(pet.fullness, pet.energy, pet.hygiene, pet.joy) < 30
            // Do not apply a white tint in the healthy state: it washes out
            // the sprite. Only tint when a care stat is critically low.
            if (needsCare) {
                views.setInt(R.id.widget_pet_image, "setColorFilter", android.graphics.Color.rgb(190, 185, 210))
            }
            if (!detailed) {
                val mood = when {
                    pet.egg -> "🥚"
                    pet.fullness < 30 -> "🍓"
                    pet.energy < 30 -> "💤"
                    pet.hygiene < 30 -> "🫧"
                    pet.joy < 30 -> "💗"
                    else -> "☺"
                }
                views.setTextViewText(R.id.widget_mood, mood)
            }
            if (detailed) {
                val partnerName = if (pet.egg) "알" else {
                    runCatching { PetNative.dexEntries().firstOrNull { it.id == pet.speciesId }?.name }
                        .getOrNull()?.takeIf { it.isNotBlank() } ?: "파트너"
                }
                views.setTextViewText(R.id.widget_pet_name, partnerName)
                views.setTextViewText(R.id.widget_pet_level, if (pet.egg) "부화 대기" else "Lv. ${pet.level}")
                views.setTextViewText(R.id.widget_hunger, "🍓 배고픔  ${pet.fullness}%")
                views.setTextViewText(R.id.widget_energy, "⚡ 에너지  ${pet.energy}%")
                views.setTextViewText(R.id.widget_hygiene, "🫧 청결도  ${pet.hygiene}%")
                views.setTextViewText(R.id.widget_joy, "💗 행복도  ${pet.joy}%")
                views.setProgressBar(R.id.widget_hunger_bar, 100, pet.fullness.coerceIn(0, 100), false)
                views.setProgressBar(R.id.widget_energy_bar, 100, pet.energy.coerceIn(0, 100), false)
                views.setProgressBar(R.id.widget_hygiene_bar, 100, pet.hygiene.coerceIn(0, 100), false)
                views.setProgressBar(R.id.widget_joy_bar, 100, pet.joy.coerceIn(0, 100), false)
            } else {
                views.setTextViewText(R.id.widget_level, if (pet.egg) "알" else "Lv.${pet.level}")
            }
        }.onFailure {
            if (detailed) views.setTextViewText(R.id.widget_pet_name, "펫 정보를 불러오지 못했어요")
            else views.setTextViewText(R.id.widget_level, "TamaPoke")
        }

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            context,
            if (detailed) 202 else 101,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pending)
        ids.forEach { manager.updateAppWidget(it, views) }
    }
}
