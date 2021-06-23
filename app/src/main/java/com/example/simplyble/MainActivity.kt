package com.example.simplyble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.location.LocationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    val TAG = MainActivity::class.java.simpleName
    private val REQUEST_ENABLE_BT = 1
    private val PERMISSIONS = 1
    private val PERMISSION_REQUEST_BLUETOOTH = 1

    private lateinit var devicesRecyclerView: RecyclerView
    private lateinit var sortButton: Button
    private var sorted = false


    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    // ScanCallBack to be called when results are found
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
                Log.d(TAG, "Device Name: ${result.device.name}")
                Log.d(TAG, "Alias Name: ${result.device.alias}")
                Log.d(TAG, "Address: ${result.device.address}")
                Log.d(TAG, "RSSI: ${result.rssi}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "ERROR: ${errorCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startBluetooth()
        startApplication()
    }

    private fun startBluetooth() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        // Enable bluetooth
        if (!bluetoothAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.BLUETOOTH_ADMIN),
                PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
           PERMISSIONS -> {
                // All granted
                if (!grantResults.any{it != PackageManager.PERMISSION_GRANTED} ) {
                    Toast.makeText(
                        applicationContext,
                        "Permissions granted.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Permissions is not granted.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startScanning() {
        AsyncTask.execute {
            bluetoothLeScanner.startScan(leScanCallback)
        }
    }

    private fun startApplication() {

        devicesRecyclerView = findViewById(R.id.devicesRecyclerView)
        sortButton = findViewById(R.id.sortButton)

        startScanning()

//        var devicesList: MutableList<BLEDevice> = getFakeDevices() // Mutable order
        var devicesList: MutableList<BLEDevice> = mutableListOf<BLEDevice>() // Mutable order
        val devicesListOriginal = mutableListOf<BLEDevice>() // Original order
        devicesListOriginal.addAll(devicesList)
        val devicesAdapter = DevicesAdapter(devicesList)
        devicesRecyclerView.adapter = devicesAdapter
        devicesRecyclerView.layoutManager = LinearLayoutManager(this)
        sortButton.setOnClickListener {
            sorted = !sorted
            updateSortButtonText()
            if (!sorted) {
                devicesList.clear()
                devicesList.addAll(devicesListOriginal)
                devicesAdapter.notifyDataSetChanged()
            } else {
                devicesList.sortByDescending { it.deviceRSSI }
                devicesAdapter.notifyDataSetChanged()
            }
        }
    }


    private fun updateSortButtonText() {
        if (sorted) {
            sortButton.text = getString(R.string.disable_sort_button)
        } else {
            sortButton.text = getString(R.string.enable_sort_button)
        }
    }


    private fun getFakeDevices(): MutableList<BLEDevice> {
        return mutableListOf(
            BLEDevice("Test Device", -90.0f, false),
            BLEDevice("Test Device", -20.0f, false),
            BLEDevice("Test Device", -30.0f, false),
            BLEDevice("Test Device", -10.0f, false),
            BLEDevice("Test Device", -2.0f, false),
            BLEDevice("Test Device", -0.0f, false),
            BLEDevice("Test Device", -3.0f, false)
        )
    }
}