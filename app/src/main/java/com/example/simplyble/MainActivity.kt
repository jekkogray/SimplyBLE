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
    companion object {
        val TAG = MainActivity::class.simpleName
    }

    // Permissions
    private val REQUEST_ENABLE_BT = 1
    private val PERMISSIONS = 1

    // Views
    private lateinit var blStateScanning: TextView
    private lateinit var blDevicesFound: TextView
    private lateinit var progressBarScar: ProgressBar
    private lateinit var sortButton: Button
    private lateinit var devicesRecyclerView: RecyclerView
    private lateinit var devicesAdapter: DevicesAdapter

    // Bluetooth Objects
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val SCAN_DURATION = 30000L
    private var scanning = false
    private var sort = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()
        checkPermissions()
    }

    /**
     * Initializes the views of this activity.
     */
    private fun initializeViews() {
        blStateScanning = findViewById(R.id.bl_state_scanning)
        blDevicesFound = findViewById(R.id.bl_devices_found)
        progressBarScar = findViewById(R.id.progressBar)
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView)
        sortButton = findViewById(R.id.sortingButton)

        devicesAdapter = DevicesAdapter(mutableListOf<BLEDevice>())
        devicesRecyclerView.adapter = devicesAdapter
        devicesRecyclerView.layoutManager = LinearLayoutManager(this)
        sortButton.setOnClickListener {
            sort = devicesAdapter.toggleSorting()
            updateUIState()
        }
    }

    /**
     * Checks and handles the following permissions BLUETOOTH, BLUETOOTH_ADMIN, FINE_LOCATION.
     */
    private fun checkPermissions() {
        // Verify permissions
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
                    .setTitle(getString(R.string.dialog_permissions_title_message))
                    .setMessage(getString(R.string.dialog_permissions_message))
                    .setPositiveButton(
                        getString(R.string.dialog_positive_button)
                    ) { _, _ ->
                        requestPermissions()
                    }
                    .setNeutralButton(getString(R.string.dialog_neutral_button), null)
                    .create()
                    .show()
            } else {
                // Requests permissions without explanation
                requestPermissions()
            }
        } else {
            Toast.makeText(
                applicationContext,
                getString(R.string.toast_permissions_already_granted),
                Toast.LENGTH_SHORT
            )
                .show()
            initializeBluetooth()
        }
    }

    /**
     * Requests necessary permissions to the user.
     */
    private fun requestPermissions(){
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

    /**
     * Verifies all permissions are granted.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS -> {
                val permissionsGranted =
                    !grantResults.any { it != PackageManager.PERMISSION_GRANTED }
                if (permissionsGranted) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.toast_permissions_granted),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Start bluetooth if all required permissions are granted.
                    initializeBluetooth()
                } else {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.toast_permissions_denied),
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
                // Begin scan after bluetooth is enabled.
                startScanning()
            }
        }
    }

    /**
     * Initializes Bluetooth services
     * and performs scanning immediately
     * When available.
     */
    private fun initializeBluetooth() {
        // Verify bluetooth exists in this device.
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
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
        } else {
            Toast.makeText(
                applicationContext,
                getString(R.string.toast_ble_not_supported),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    /**
     * Listens for connections request to a BLE device
     */
    override fun onResume() {
        super.onResume()
        // Broadcast receiver listens for connection requests to a BLE device
        val intentFilterBLEConnect = IntentFilter("BLEDeviceConnect")
        val receiverBLEDeviceConnect = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val deviceAddress = intent?.getStringExtra("BLEDeviceAddress")
                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                // Attempt connection to the Gatt service in device
                device.connectGatt(applicationContext, false, bluetoothGattCallback)
            }
        }
        registerReceiver(receiverBLEDeviceConnect, intentFilterBLEConnect)
    }

    override fun onDestroy() {
        stopScanning()
        super.onDestroy()
    }

    /**
     * Starts scanning by default scanning duration.
     * Calls bluetoothLeScanner
     * @param scanDuration the duration of the scan in millisceconds.
     */
    private fun startScanning(scanDuration: Long = SCAN_DURATION) {
        Log.i(TAG, "Scanner started...")
        scanning = true
        updateUIState()
        bluetoothLeScanner.startScan(leScanCallback)
        Timer().schedule(object : TimerTask() {
            override fun run() {
                stopScanning()
            }
        }, scanDuration)

        val timerHandler = Handler(Looper.getMainLooper())
        timerHandler.post(object : Runnable {
            var timeElapsed = 0
            override fun run() {
                if (timeElapsed < (scanDuration / 1000).toInt()) {
                    timeElapsed += 1
                    progressBarScar.incrementProgressBy((100L / (scanDuration / 1000)).toInt())
                    timerHandler.postDelayed(this, 1000)
                }
            }
        })
    }

    /**
     * Stops the bluetooth LE scanner
     */
    private fun stopScanning() {
        Log.i(TAG, "Scanner stopped...")
        scanning = false
        updateUIState()
        bluetoothLeScanner.stopScan(leScanCallback)
    }

    /**
     * The bluetooth gattCallBack returns the result of attempting
     * to connect to the Bluetooth device.
     * Determines if the connection was successful
     */
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceName = if (gatt?.device?.name != null) gatt?.device?.name else "Unknown"
            // Successful connection attempt
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i("${TAG}/bluetoothGattCallBack", "STATE_CONNECTED")
                        runOnUiThread() {
                            Toast.makeText(
                                applicationContext,
                                "$deviceName\n${getString(R.string.toast_gatt_connected)}",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i("${TAG}/bluetoothGattCallBack", "STATE_DISCONNECTED")
                        runOnUiThread() {
                            Toast.makeText(
                                applicationContext,
                                "$deviceName\n${getString(R.string.toast_gatt_disconnected)}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                // Connection attempt failed
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "${deviceName}\nFailed to connect",
                        Toast.LENGTH_SHORT
                    ).show()
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

    /**
     * The Le scan callback scans for devices and adds to the list.
     */
    private val leScanCallback = object : ScanCallback() {
        // API 26 is required to use .isConnectable
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
                blDevicesFound.visibility = View.VISIBLE
                val deviceName: String =
                    if (result?.device?.name != null) result?.device?.name!! else "Unknown"
                val bleDevice = BLEDevice(
                    deviceName,
                    result.device.address,
                    result.rssi,
                    result.isConnectable
                )
                blDevicesFound.text =
                    "${getString(R.string.label_bl_devices_found)}${devicesAdapter.itemCount}"
                Log.d(TAG, "Device Name: ${result.device.name}")
                Log.d(TAG, "Alias Name: ${result.device.alias}")
                Log.d(TAG, "Address: ${result.device.address}")
                Log.d(TAG, "RSSI: ${result.rssi}")
                Log.d(TAG, "Bluetooth Device Type: ${result.device.type}")
                Log.d(TAG, "Connectable: ${result.isConnectable}")

                devicesAdapter.addDevice(bleDevice)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "ERROR: ${errorCode}")
            Toast.makeText(
                applicationContext,
                getString(R.string.toast_bl_state_scan_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Updates the UI based on the states of the scanner and the sort
     */
    private fun updateUIState() {
        runOnUiThread() {
            if (scanning) {
                blStateScanning.text = getString(R.string.label_bl_state_scanning)
                blDevicesFound.visibility = View.VISIBLE
                progressBarScar.visibility = View.VISIBLE
            } else {
                blStateScanning.text = getString(R.string.label_bl_state_scanning_completed)
                blDevicesFound.visibility = View.VISIBLE
                progressBarScar.visibility = View.GONE
                progressBarScar.progress = 0
            }
            // No bluetooth devices found
            if (devicesAdapter.itemCount == 0) {
                blStateScanning.text = getString(R.string.toast_bl_devices_not_found)
            }
            if (sort) {
                sortButton.text = getString(R.string.sorting_button_label_enabled)
            } else {
                sortButton.text = getString(R.string.sorting_button_label_disabled)
            }
        }
    }
}