package com.example.simplyble

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
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

    val devicesList: MutableList<BLEDevice> = mutableListOf<BLEDevice>()

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
    private var bluetoothService: BluetoothLEService? = null


    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as BluetoothLEService.LocalBinder).getService()
            bluetoothService?.let { bluetooth ->
                // call functions on service to check connection and connect to devices
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("${TAG}/bluetoothGattCallBack", "STATE_CONNECTED")
                    runOnUiThread() {
                       Toast.makeText(applicationContext, "Connected to Gatt", Toast.LENGTH_SHORT).show()
                        gatt?.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i("${TAG}/bluetoothGattCallBack", "STATE_DISCONNECTED")
                    runOnUiThread() {
                        Toast.makeText(applicationContext, "Disconnected to Gatt", Toast.LENGTH_SHORT).show()
                    }
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

            if (result != null && result.device.type == BluetoothDevice.DEVICE_TYPE_LE) {
                blDevicesFound.visibility = View.VISIBLE

                val bleDevice = BLEDevice(
                    result.device.name,
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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeUI()
        checkPermissions()
    }

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
                    .setTitle("Please the following permissions")
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
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView)
        sortButton = findViewById(R.id.sortButton)
        blStateScanning = findViewById(R.id.bl_state_scanning)
        blDevicesFound = findViewById(R.id.bl_devices_found)

//        val devicesListOriginal = mutableListOf<BLEDevice>()// Original order
//        devicesListOriginal.addAll(devicesList)
        devicesAdapter = DevicesAdapter(devicesList)
        devicesRecyclerView.adapter = devicesAdapter
        devicesRecyclerView.layoutManager = LinearLayoutManager(this)


        sortButton.visibility = View.INVISIBLE
        sortButton.isEnabled = false

        sortButton.setOnClickListener {
            sorted = !sorted
            updateSortButtonText()
            if (!sorted) {
//                devicesList.clear()
//                devicesList.addAll(devicesListOriginal)
//                devicesAdapter.notifyDataSetChanged()
            } else {
//                devicesList.sortByDescending { it.deviceRSSI }
//                devicesAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun startUI() {
        startScanning()
        // var devicesList: MutableList<BLEDevice> = getFakeDevices() // Mutable order
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
                val deviceAddress= intent?.getStringExtra("BLEDeviceAddress")
                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                device.connectGatt(applicationContext, false, bluetoothGattCallback)
            }
        }
        registerReceiver(receiverBLEDeviceConnect, intentFilterBLEConnect)
    }

    override fun onDestroy() {
        stopScanning()
        super.onDestroy()
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
        AsyncTask.execute {
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }

    private fun updateSortButtonText() {
        if (sorted) {
            sortButton.text = getString(R.string.disable_sort_button)
        } else {
            sortButton.text = getString(R.string.enable_sort_button)
        }
    }


//    private fun getFakeDevices(): MutableList<BLEDevice> {
//        return mutableListOf(
//            BLEDevice("Test Device", "74:BF:C0:18:64:0F", -20, false),
//            BLEDevice("Test Device", "74:BF:C0:18:64:0F", -90, false),
//            BLEDevice("Test Device", "74:BF:C0:18:64:0F", -2, false),
//            BLEDevice("Test Device", "74:BF:C0:18:64:0F", -40, false),
//            BLEDevice("Test Device", "74:BF:C0:18:64:0F", -50, false)
//        )
//    }
}