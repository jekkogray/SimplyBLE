package com.example.simplyble
import java.io.Serializable

data class BLEDevice (val deviceName: String, val deviceRSSI:Float, var deviceConnectable: Boolean)