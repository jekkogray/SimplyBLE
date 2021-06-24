package com.example.simplyble

import android.bluetooth.le.ScanResult
import java.io.Serializable

data class BLEDevice(
    var deviceName: String,
    val deviceAddress: String,
    var deviceRSSI: Int,
    var deviceConnectable: Boolean
):Serializable {
    constructor():this("", "",0, false)
}