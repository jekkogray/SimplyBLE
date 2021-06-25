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

    /**
     * Adds a new device to the end of the list.
     * If sorting is enabled it will automatically be sorted to their appropriate places.
     */
    fun addDevice(device: BLEDevice) {
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

    /**
     * Toggles sorting of current detected devices and devices to be scanned.
     * Return the status of the sort
     * @return sort
     */
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

    /**
     * Inflates XML to be used for the new row creation.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = layoutInflater.inflate(R.layout.row_device, parent, false)
        return ViewHolder(view)
    }

    /**
     * Gets device list size
     */
    override fun getItemCount(): Int {
        return BLEDevices.size
    }

    /**
     * Binds row when viewholder is ready to display a new row
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentDevice = BLEDevices.elementAt(position)
        val viewContext = holder.itemView.context
        val deviceName = currentDevice.deviceName

        holder.deviceName.text =
            "${viewContext.getString(R.string.card_label_device_name)} ${currentDevice.deviceName}"
        holder.deviceRSSI.text =
            "${viewContext.getString(R.string.card_label_device_RSSI)} ${currentDevice.deviceRSSI}"
        holder.deviceConnectable.text =
            "${viewContext.getString(R.string.card_label_device_connectable)} ${currentDevice.deviceConnectable}"

        holder.deviceCard.setOnClickListener {
            // Broadcast attempt to connect to device
            if (currentDevice.deviceConnectable) {
                Toast.makeText(
                    viewContext,
                    "${viewContext.getString(R.string.toast_gatt_connecting)} $deviceName",
                    Toast.LENGTH_SHORT
                ).show()
                val intentBLEDeviceConnect = Intent("BLEDeviceConnect")
                intentBLEDeviceConnect.putExtra("BLEDeviceAddress", currentDevice.deviceAddress)
                it.context.sendBroadcast(intentBLEDeviceConnect)
            } else {
                // Not connectable
                Toast.makeText(
                    viewContext,
                    "$deviceName ${viewContext.getString(R.string.toast_gatt_not_connectable)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            this.notifyDataSetChanged()
        }

    }
}