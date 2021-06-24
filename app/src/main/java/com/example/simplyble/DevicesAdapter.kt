package com.example.simplyble

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class DevicesAdapter(
    val BLEDevices: MutableList<BLEDevice>,
    val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
) :
    RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {
    private val cBLEDevices = mutableListOf<BLEDevice>()
    // TODO: Implement a way to store original order of the list
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceCard: CardView = itemView.findViewById(R.id.deviceCard)
        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        val deviceRSSI: TextView = itemView.findViewById(R.id.deviceRSSI)
        val deviceConnectable: TextView = itemView.findViewById(R.id.deviceConnectable)
    }

    fun addItem(device: BLEDevice) {
        // TODO: if sorted automatically sort after insertion and update new original list
        // Update
        val updateDeviceIndex = BLEDevices.indexOfFirst { it.deviceAddress == device.deviceAddress }
        if (updateDeviceIndex != -1) {
            BLEDevices[updateDeviceIndex] = device
        } else {
            BLEDevices.add(device)
        }
        this.notifyDataSetChanged()
    }

    fun sortByDescending() {
        cBLEDevices.clear()
        cBLEDevices.addAll(BLEDevices)
        BLEDevices.sortByDescending { it.deviceRSSI }
        notifyDataSetChanged()
    }

    // Recyclerview new row creation -- specify XML.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = layoutInflater.inflate(R.layout.row_device, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return BLEDevices.size
    }

    // ViewHolder ready to display new row.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentDevice = BLEDevices.elementAt(position)
        val viewContext = holder.itemView.context
        holder.deviceCard.setOnClickListener {
            val intentBLEDeviceConnect = Intent("BLEDeviceConnect")
            intentBLEDeviceConnect.putExtra("BLEDeviceAddress", currentDevice.deviceAddress)
            it.context.sendBroadcast(intentBLEDeviceConnect)
            this.notifyDataSetChanged()
        }
        holder.deviceName.text =
            "${viewContext.getString(R.string.device_name)} ${currentDevice.deviceName}"
        holder.deviceRSSI.text =
            "${viewContext.getString(R.string.device_RSSI)} ${currentDevice.deviceRSSI}"
        holder.deviceConnectable.text =
            "${viewContext.getString(R.string.device_connectable)} ${currentDevice.deviceConnectable}"

    }
}