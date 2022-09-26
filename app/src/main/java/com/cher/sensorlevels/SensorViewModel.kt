package com.cher.sensorlevels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SensorViewModel(androidContext: Context) : ViewModel() {

    private val _wifiLevel = MutableLiveData<Float?>()
    val wifiLevel: LiveData<Float?> = _wifiLevel

    private val _gsmLevel = MutableLiveData<Float?>()
    val gsmLevel: LiveData<Float?> = _gsmLevel

    private val _micVolumeLevel = MutableLiveData<Float?>()
    val micVolumeLevel: LiveData<Float?> = _micVolumeLevel

    private val _operatorName = MutableLiveData<String?>()
    val operatorName: LiveData<String?> = _operatorName


    fun updateGSMLevel(level: Float) {
        viewModelScope.launch { _gsmLevel.value = level / 10 }

    }

    fun updateWifiLevel(level: Float) {
        viewModelScope.launch { _wifiLevel.value = level / 10 }

    }

    fun updateMICLevel(level: Float) {
        viewModelScope.launch {  _micVolumeLevel.value = level }

    }

    fun updateOperatorName( string: String){
        if (string!= _operatorName.value)
        _operatorName.value = string
    }

}