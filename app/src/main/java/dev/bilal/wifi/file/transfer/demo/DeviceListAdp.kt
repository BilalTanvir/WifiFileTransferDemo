package dev.bilal.wifi.file.transfer.demo

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bilal.wifi.file.transfer.demo.databinding.SingleItemBinding

class DeviceListAdp(
    private val list: MutableList<WifiP2pDevice>,
    private val callback: (WifiP2pDevice) -> Unit
) : RecyclerView.Adapter<DeviceListAdp.DeviceListVH>() {

    inner class DeviceListVH(private val binding: SingleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(device: WifiP2pDevice) {
            binding.deviceName.text = device.deviceName
            binding.root.setOnClickListener {
                callback.invoke(device)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceListVH {
        val binding = SingleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceListVH(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: DeviceListVH, position: Int) {
        val item = list[position]
        holder.bind(item)
    }

    fun submitList(newList: List<WifiP2pDevice>) {
        this.list.clear()
        this.list.addAll(newList)
        notifyDataSetChanged()
    }
}