package com.example.simplyble

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

// Interacts with the BLE device and provides the API.
class BluetoothLEService: Service() {
    private val binder = LocalBinder()
    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder(), IBinder {
        fun getService() : BluetoothLEService {
            return this@BluetoothLEService
        }
    }
}