package com.example.simplyble

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MainActivity : AppCompatActivity() {
    val TAG = MainActivity::class.java.simpleName
    private val REQUEST_ENABLE_BT = 1
    private val PERMISSIONS = 1

    private lateinit var blStateScanning: TextView
    private lateinit var blDevicesFound: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var sortButton: Button
    private lateinit var devicesRecyclerView: RecyclerView
    private lateinit var devicesAdapter: DevicesAdapter

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceName = if (gatt?.device?.name !=null) gatt?.device?.name else "Unknown"
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i("${TAG}/bluetoothGattCallBack", "STATE_CONNECTED")
                        runOnUiThread() {
                            Toast.makeText(
                                applicationContext,
                                "${deviceName}\nConnected to Gatt",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i("${TAG}/bluetoothGattCallBac", "STATE_DISCONNECTED")
                        runOnUiThread() {
                            Toast.makeText(
                                applicationContext,
                                "${deviceName}\nDisconnected to Gatt",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    else -> {
                        Log.i("${TAG}/bluetoothGattCallBac", "STATE_DISCONNECTED")
                        runOnUiThread() {
                            Toast.makeText(
                                applicationContext,
                                "${deviceName}\nUnable to Connect to Gatt",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                runOnUiThread{
                    Toast.makeText(applicationContext, "${deviceName}\nConnection failed.", Toast.LENGTH_SHORT).show()
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
    }

    private val leScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
                blDevicesFound.visibility = View.VISIBLE
                val deviceName:String= if (result?.device?.name != null) result?.device?.name!! else "Unknown"
                val bleDevice = BLEDevice(
                    deviceName,
                    result.device.address,
                    result.rssi,
                    result.isConnectable
                )

                devicesAdapter.addItem(bleDevice)
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
        initializeUI()
        checkPermissions()
    }

    /**
     * Checks if the following permissions BLUETOOTH, BLUETOOTH_ADMIN, FINE_LOCATION
     */
    private fun checkPermissions() {
        // Check permissions
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) +
            checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) +
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) +
            checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Show alert dialog with request explanation
            if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_ADMIN) ||
                shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            ) {
                AlertDialog.Builder(applicationContext)
                    .setTitle("Please allow the following permissions")
                    .setMessage("Bluetooth, Bluetooth Admin, Access Fine Location, and Access Background are required to find and connect to BLE devices.")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
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
                    .setNeutralButton("Cancel", null)
                    .create()
                    .show()
            } else {
                // Requests permissions without explanation
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
        } else {
            Toast.makeText(applicationContext, "Permissions already granted.", Toast.LENGTH_SHORT)
                .show()
            initializeBluetooth()
        }
    }

    /**
     * Initialize views in this activity.
     */
    private fun initializeUI() {
        blStateScanning = findViewById(R.id.bl_state_scanning)
        blDevicesFound = findViewById(R.id.bl_devices_found)
        progressBar = findViewById(R.id.progressBar)
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView)
        sortButton = findViewById(R.id.sortButton)

        devicesAdapter = DevicesAdapter(mutableListOf<BLEDevice>())
        devicesRecyclerView.adapter = devicesAdapter
        devicesRecyclerView.layoutManager = LinearLayoutManager(this)
        sortButton.setOnClickListener {
            if (devicesAdapter.toggleSorting()) {
                sortButton.text = getString(R.string.sorting_enabled)
            } else {
                sortButton.text = getString(R.string.sorting_disabled)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS -> {
                val granted = !grantResults.any { it != PackageManager.PERMISSION_GRANTED }
                if (granted) {
                    Toast.makeText(
                        applicationContext,
                        "Permissions granted.",
                        Toast.LENGTH_SHORT
                    ).show()
                    initializeBluetooth()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Permissions denied.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                startScanning()
            }
        }
    }

    private fun initializeBluetooth() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        // Enable bluetooth
        if (!bluetoothAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        } else {
            startScanning()
        }
    }

    override fun onResume() {
        super.onResume()
        val intentFilterBLEConnect = IntentFilter("BLEDeviceConnect")
        val receiverBLEDeviceConnect = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                bluetoothAdapter = bluetoothManager.adapter
                val deviceAddress = intent?.getStringExtra("BLEDeviceAddress")
                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                device.connectGatt(applicationContext, false, bluetoothGattCallback)
            }
        }
        // TODO: Create receiver for updating already connected device
        registerReceiver(receiverBLEDeviceConnect, intentFilterBLEConnect)
    }

    override fun onDestroy() {
        stopScanning()
        super.onDestroy()
    }

    private fun startScanning(ms: Long = 30000L) {
        Log.i(TAG, "Scanner started...")
        blStateScanning.visibility = View.VISIBLE
        bluetoothLeScanner.startScan(leScanCallback)
        blStateScanning.visibility = View.VISIBLE
        Timer().schedule(object : TimerTask() {
            override fun run() {
                stopScanning()
            }
        }, ms)

        val timerHandler = Handler(Looper.getMainLooper())
        timerHandler.post(object : Runnable {
            var time = 0
            override fun run() {
                if (time < (ms / 1000).toInt()) {
                    time += 1
                    progressBar.incrementProgressBy((100L / (ms / 1000)).toInt())
                    timerHandler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun stopScanning() {
        Log.i(TAG, "Scanner stopped...")
        runOnUiThread{
            blStateScanning.visibility = View.INVISIBLE
            progressBar.visibility = View.GONE
            progressBar.progress = 0
        }
        bluetoothLeScanner.stopScan(leScanCallback)
    }

}