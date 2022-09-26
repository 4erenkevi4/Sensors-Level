package com.cher.sensorlevels.di

import com.cher.sensorlevels.SensorViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sensorViewModel = module {
    single { SensorViewModel(androidContext())}
}


