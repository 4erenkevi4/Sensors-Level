package com.cher.sensorlevels

import android.app.Application
import com.cher.sensorlevels.di.sensorViewModel

import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(listOf(sensorViewModel))
        }
    }
}