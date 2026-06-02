package com.huellalive.app.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.java.KoinJavaComponent.get as koinGet

actual fun createSettings(): Settings {
    val context = koinGet<Context>(Context::class.java)
    return SharedPreferencesSettings(
        context.getSharedPreferences("huellalive_prefs", Context.MODE_PRIVATE)
    )
}