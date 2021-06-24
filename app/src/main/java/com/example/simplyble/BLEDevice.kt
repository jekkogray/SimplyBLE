package com.example.simplyble

import android.bluetooth.le.ScanResult
import java.io.Serializable

data class BLEDevice(
    var deviceName: String = "",
    val deviceAddress: String = "",
    var deviceRSSI: Int = 0,
    var deviceConnectable: Boolean = false
)