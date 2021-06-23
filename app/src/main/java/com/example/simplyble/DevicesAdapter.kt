package com.example.simplyble

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class DevicesAdapter(val BLEDevices: MutableList<BLEDevice>) :
    RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceCard: CardView = itemView.findViewById(R.id.deviceCard)
        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        val deviceRSSI: TextView = itemView.findViewById(R.id.deviceRSSI)
        val deviceConnectable: TextView = itemView.findViewById(R.id.deviceConnectable)
    }

    fun addItem(device: BLEDevice) {
        BLEDevices.add(device)
        this.notifyDataSetChanged()
    }

    // Recyclerview new row creation -- specify XML.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = layoutInflater.inflate(R.layout.row_device, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        // TODO: Prevent duplicate inputs
        return BLEDevices.size
    }


    // ViewHolder ready to display new row.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentDevice = BLEDevices[position]
        val viewContext = holder.itemView.context
        holder.deviceCard.setOnClickListener {
            // TODO: Connect here
            Toast.makeText(viewContext, "Connecting...", Toast.LENGTH_SHORT).show()
            BLEDevices[position].deviceConnectable = !BLEDevices[position].deviceConnectable
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