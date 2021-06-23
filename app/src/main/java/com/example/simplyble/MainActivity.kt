package com.example.simplyble

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MainActivity : AppCompatActivity() {
    val TAG = MainActivity::class.java.simpleName
    private val REQUEST_ENABLE_BT = 1
    private val PERMISSIONS = 1

    // UI
    private lateinit var blStateScanning: TextView
    private lateinit var blDevicesFound: TextView
    private lateinit var devicesRecyclerView: RecyclerView
    private lateinit var sortButton: Button
    private lateinit var devicesAdapter: DevicesAdapter

    private var sorted = false

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("${TAG}/bluetoothGattCallBack", "STATE_CONNECTED")
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i("${TAG}/bluetoothGattCallBack", "STATE_DISCONNECTED")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val services: List<BluetoothGattService> = gatt.services
            Log.i("onServicesDiscovered", services.toString())
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.i("onCharacteristicsRead", characteristic.toString())
            gatt?.disconnect()
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
        }
    }
    private val leScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result != null && result.device.type == BluetoothDevice.DEVICE_TYPE_LE) {
                blDevicesFound.visibility = View.VISIBLE

                val bleDevice = BLEDevice(
                    result.device.name,
                    result.device.address,
                    result.rssi,
                    result.isConnectable,
                    result
                )

                devicesAdapter.addItem(bleDevice)
                result.device.connectGatt(applicationContext, false, bluetoothGattCallback)
                blDevicesFound.text =
                    "${getString(R.string.bl_devices_found)}${devicesAdapter.itemCount}"
                Log.d(TAG, "Device Name: ${result.device.name}")
                Log.d(TAG, "Alias Name: ${result.device.alias}")
                Log.d(TAG, "Address: ${result.device.address}")
                Log.d(TAG, "RSSI: ${result.rssi}")
                Log.d(TAG, "Bluetooth Device Type: ${result.device.type}")
                Log.d(TAG, "Connectable: ${result.isConnectable}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "ERROR: ${errorCode}")
            Toast.makeText(
                applicationContext,
                getString(R.string.bl_state_scan_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startBluetooth()
        startUI()
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
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN
                ),
                PERMISSIONS
            )
        }
    }

    override fun onDestroy() {
        stopScanning()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS -> {
                // All granted
                if (!grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(
                        applicationContext,
                        "Permissions granted.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Not all required permissions is granted.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startScanning(ms: Long = 10000L) {
        Log.d(TAG, "Scanner stated...")
        blStateScanning.visibility = View.VISIBLE
        AsyncTask.execute {
            bluetoothLeScanner.startScan(leScanCallback)
            blStateScanning.visibility = View.VISIBLE
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    stopScanning()
                }
            }, ms)

        }
    }

    private fun stopScanning() {
        Log.d(TAG, "Scanner stopped...")
        blStateScanning.visibility = View.INVISIBLE
//        blDevicesFound.visibility = View.INVISIBLE
        AsyncTask.execute {
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }


    private fun startUI() {
        // Initialize UI
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView)
        sortButton = findViewById(R.id.sortButton)
        blStateScanning = findViewById(R.id.bl_state_scanning)
        blDevicesFound = findViewById(R.id.bl_devices_found)

        startScanning()
        // var devicesList: MutableList<BLEDevice> = getFakeDevices() // Mutable order
        var devicesList: MutableList<BLEDevice> = mutableListOf<BLEDevice>() // Mutable order
        val devicesListOriginal = mutableListOf<BLEDevice>()// Original order
        devicesListOriginal.addAll(devicesList)
        devicesAdapter = DevicesAdapter(devicesList)
        devicesRecyclerView.adapter = devicesAdapter
        devicesRecyclerView.layoutManager = LinearLayoutManager(this)

        sortButton.visibility = View.INVISIBLE
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
            BLEDevice("Test Device", "74:BF:C0:18:64:0F", -20, false),
            BLEDevice("Test Device", "74:BF:C0:18:64:0F", -90, false),
            BLEDevice("Test Device", "74:BF:C0:18:64:0F", -2, false),
            BLEDevice("Test Device", "74:BF:C0:18:64:0F", -40, false),
            BLEDevice("Test Device", "74:BF:C0:18:64:0F", -50, false)
        )
    }
}