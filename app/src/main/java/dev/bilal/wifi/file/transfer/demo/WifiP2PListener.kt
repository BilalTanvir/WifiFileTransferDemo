package dev.bilal.wifi.file.transfer.demo

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo

interface WifiP2PListener {

    fun wifiP2pEnabled(enabled: Boolean)

    fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo)

    fun onDisconnection()

    fun onSelfDeviceAvailable(device: WifiP2pDevice)

    fun onPeersAvailable(devices: List<WifiP2pDevice>)
}