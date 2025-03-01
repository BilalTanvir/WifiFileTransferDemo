package dev.bilal.wifi.file.transfer.demo

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log

private const val TAG = "WifiP2PBroadCastReceive"
class WifiP2PBroadCastReceiver(
    private val wifiP2pManager: WifiP2pManager, private val wifiP2pChannel: WifiP2pManager.Channel,
    private val directActionListener: WifiP2PListener
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val enabled = intent.getIntExtra(
                    WifiP2pManager.EXTRA_WIFI_STATE,
                    -1
                ) == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                directActionListener.wifiP2pEnabled(enabled)
                if (!enabled) {
                    directActionListener.onPeersAvailable(emptyList())
                }

                Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION:  $enabled")
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {

                wifiP2pManager.requestPeers(wifiP2pChannel) { peers ->
                    directActionListener.onPeersAvailable(peers.deviceList.toList())
                }
                Log.d(TAG, "onReceive: WIFI_P2P_PEERS_CHANGED_ACTION ")
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo =
                    intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)

                if (networkInfo != null && networkInfo.isConnected) {
                    wifiP2pManager.requestConnectionInfo(wifiP2pChannel) { info ->
                        if (info != null) {
                            directActionListener.onConnectionInfoAvailable(info)
                        }
                    }
                    Log.d(
                        TAG,
                        "onReceive: WIFI_P2P_CONNECTION_CHANGED_ACTION   ${networkInfo?.isConnected}"
                    )
                } else {
                    directActionListener.onDisconnection()
                    Log.d(
                        TAG,
                        "onReceive: WIFI_P2P_CONNECTION_CHANGED_ACTION   ${networkInfo?.isConnected}"
                    )
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val wifiP2pDevice =
                    intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                if (wifiP2pDevice != null) {
                    directActionListener.onSelfDeviceAvailable(wifiP2pDevice)
                }
                Log.d(TAG, "onReceive: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION $wifiP2pDevice")
            }
        }
    }

}