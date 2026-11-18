package com.tamapoke.android

import android.content.Context

/**
 * Diagnostic build intentionally disables launcher-widget updates while
 * validating the main pet UI and JNI engine independently.
 */
object MiniPetWidgetRenderer {
    fun updateAll(context: Context) = Unit
}
