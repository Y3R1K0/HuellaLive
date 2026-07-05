package com.huellalive.app

import android.app.Application
import com.huellalive.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class HuellaLiveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@HuellaLiveApp)
        }
    }
}
