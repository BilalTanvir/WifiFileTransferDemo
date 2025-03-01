package dev.bilal.wifi.file.transfer.demo

import android.annotation.SuppressLint
import android.net.Uri
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.bilal.wifi.file.transfer.demo.MainActivity.Companion.PORT
import dev.bilal.wifi.file.transfer.demo.databinding.ActivitySenderBinding
import java.io.File
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

private const val TAG = "SenderActivity"
class SenderActivity : BaseActivity(), WifiP2PListener {
    private val binding by lazy {
        ActivitySenderBinding.inflate(layoutInflater)
    }
    private var wifiDirectReceiver: WifiP2PBroadCastReceiver? = null

    private var adp: DeviceListAdp? = null

    private var wifiP2pConnectionInfo: WifiP2pInfo? = null

    private var isConnectionFormed: Boolean = false

    private val arrayList: MutableList<File> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initialize()
        setAdp()
        setListeners()
    }

    private fun setListeners() {
        binding.searchForDevices.setOnClickListener {
            discoverDevices()
        }

        binding.pickFiles.setOnClickListener {
            launcher.launch("*/*")
        }

        onBackPressedDispatcher.addCallback {
            if (isConnectionFormed) {
                disconnect()
            }
        }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {


            it?.let {
                sendFile(it)
            } ?: run {
                Toast.makeText(this, "File selection failed", Toast.LENGTH_SHORT).show()
            }
        }


    @SuppressLint("Recycle")
    private fun sendFile(fileUri: Uri) {
        val fileName = getFileName(fileUri)
        Log.d(TAG, "sendFile: File $fileName")
        val fileSize = contentResolver.openFileDescriptor(fileUri, "r")?.statSize ?: 0L

        Thread {
            try {
                val socket = Socket()
                socket.connect(
                    InetSocketAddress(
                        wifiP2pConnectionInfo?.groupOwnerAddress?.hostAddress,
                        PORT
                    ), 5000
                )

                val outputStream = socket.getOutputStream()
                outputStream.write(fileName.toByteArray())

                val inputStream: InputStream? = contentResolver.openInputStream(fileUri)
                inputStream?.let {
                    val buffer = ByteArray(1024)
                    var bytesTransferred = 0L
                    var bytesRead: Int

                    while (it.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesTransferred += bytesRead

                        // Update progress on UI thread
                        runOnUiThread {
                            val progress = (bytesTransferred * 100 / fileSize).toInt()

                            Log.d(TAG, "sendFile: $progress")
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream?.close()
                socket.close()

                runOnUiThread {
                    Toast.makeText(this, "File sent successfully", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error sending file", e)
                runOnUiThread {
                    Toast.makeText(this, "Error sending file: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }.start()
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "unknown_file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("_display_name")
            cursor.moveToFirst()
            fileName = cursor.getString(nameIndex)
        }
        return fileName
    }

    @SuppressLint("MissingPermission")
    private fun discoverDevices() {
        wifiP2pManager.discoverPeers(wifiP2pChannel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "onSuccess: discoverDevices Sender")

            }

            override fun onFailure(reasonCode: Int) {
                Log.d(TAG, "onFailure: discoverDevices Sender")
            }
        })
    }

    private fun setAdp() {
        adp = DeviceListAdp(mutableListOf(), ::connectToDevice)
        binding.devicesRecycler.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.devicesRecycler.adapter = adp
    }

    private fun initialize() {
        wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, null)
        wifiDirectReceiver = WifiP2PBroadCastReceiver(wifiP2pManager, wifiP2pChannel!!, this)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiDirectReceiver, getIntentFilter(), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wifiDirectReceiver, getIntentFilter())
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiDirectReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isConnectionFormed) {
            disconnect()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: WifiP2pDevice) {
        val wifiP2pConfig = WifiP2pConfig()
        wifiP2pConfig.deviceAddress = device.deviceAddress
        wifiP2pConfig.groupOwnerIntent = 0

        wifiP2pManager.connect(wifiP2pChannel, wifiP2pConfig,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "onSuccess: connectToDevice Sender")
                }

                override fun onFailure(reason: Int) {
                    Log.d(TAG, "onFailure: connectToDevice Sender")

                }
            }
        )
    }


    // Override Started
    override fun wifiP2pEnabled(enabled: Boolean) {
        Log.d(TAG, "wifiP2pEnabled:  Sender $enabled ")
    }

    override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
        Log.d(TAG, "onConnectionInfoAvailable: Sender $wifiP2pInfo")
        if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
            isConnectionFormed = true
            wifiP2pConnectionInfo = wifiP2pInfo
        }

    }

    override fun onDisconnection() {
        Log.d(TAG, "onDisconnection: Sender")
    }

    override fun onSelfDeviceAvailable(device: WifiP2pDevice) {
        Log.d(TAG, "onSelfDeviceAvailable: Sender")
    }

    override fun onPeersAvailable(devices: List<WifiP2pDevice>) {
        adp?.submitList(devices)
    }

    private fun disconnect() {
        wifiP2pManager.cancelConnect(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                Log.d(TAG, "onFailure Sender on Disconnect: $reasonCode")
            }

            override fun onSuccess() {
                Log.d(TAG, "onSuccess: Sender Disconnect ")
            }
        })
        wifiP2pManager.removeGroup(wifiP2pChannel, null)
    }
}