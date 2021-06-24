package com.example.simplyble

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class DevicesAdapter(
    val BLEDevices: MutableList<BLEDevice>
) :
    RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceCard: CardView = itemView.findViewById(R.id.deviceCard)
        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        val deviceRSSI: TextView = itemView.findViewById(R.id.deviceRSSI)
        val deviceConnectable: TextView = itemView.findViewById(R.id.deviceConnectable)
    }

    var sort: Boolean = false

    fun addItem(device: BLEDevice) {
        val updateDeviceIndex = BLEDevices.indexOfFirst { it.deviceAddress == device.deviceAddress }
        if (updateDeviceIndex != -1) {
            BLEDevices[updateDeviceIndex] = device
        } else {
            BLEDevices.add(device)
        }

        if (sort) {
            sortByDescending()
        }
        this.notifyDataSetChanged()
    }

    fun toggleSorting(): Boolean {
        sort = !sort
        if (sort) {
            sortByDescending()
        }
        this.notifyDataSetChanged()
        return sort
    }

    private fun sortByDescending() {
        BLEDevices.sortByDescending { it.deviceRSSI }
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
            if (currentDevice.deviceConnectable) {
                Toast.makeText(viewContext, "Connecting...", Toast.LENGTH_SHORT).show()
                val intentBLEDeviceConnect = Intent("BLEDeviceConnect")
                intentBLEDeviceConnect.putExtra("BLEDeviceAddress", currentDevice.deviceAddress)
                it.context.sendBroadcast(intentBLEDeviceConnect)
            } else {
                Toast.makeText(viewContext, "${currentDevice.deviceName} is not connectable.", Toast.LENGTH_SHORT).show()
            }
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