package com.example.simplyble

import android.bluetooth.le.ScanResult

data class BLEDevice(
    var deviceName: String,
    val deviceAddress: String,
    var deviceRSSI: Int,
    var deviceConnectable: Boolean,
    var scanResult: ScanResult? = null
)