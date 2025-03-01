package dev.bilal.wifi.file.transfer.demo

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.addCallback
import dev.bilal.wifi.file.transfer.demo.MainActivity.Companion.PORT
import dev.bilal.wifi.file.transfer.demo.databinding.ActivityReceiverBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket


private const val TAG = "ReceiverActivity"
class ReceiverActivity : BaseActivity(), WifiP2PListener {
    private val binding by lazy {
        ActivityReceiverBinding.inflate(layoutInflater)
    }
    private var wifiDirectReceiver: WifiP2PBroadCastReceiver? = null

    private var isConnectionFormed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        initialize()
        setListener()
    }

    private fun initialize() {
        wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, null)
        wifiDirectReceiver = WifiP2PBroadCastReceiver(wifiP2pManager, wifiP2pChannel!!, this)
    }

    private fun setListener() {
        binding.startListen.setOnClickListener {
            if (removeGroupIfNeed()) {
                createGroupToListen()
            }
        }

        onBackPressedDispatcher.addCallback {
            removeGroupIfNeed()
        }
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
            removeGroupIfNeed()
        }
    }


    @SuppressLint("MissingPermission")
    private fun createGroupToListen() {
        wifiP2pManager.createGroup(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "onSuccess: createGroupToListen Receiver")
                receiveFile()
            }

            override fun onFailure(reason: Int) {
                Log.d(TAG, "onFailure: createGroupToListen Receiver")
            }
        })
    }

    private fun receiveFile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverSocket = ServerSocket(PORT)
                val socket = serverSocket.accept()

                val inputStream = socket.getInputStream()

                // Read file name
                val metadataBuffer = ByteArray(256)
                val read = inputStream.read(metadataBuffer)
                val fileName = String(metadataBuffer, 0, read).trim()  // Trim and only convert the actual bytes read

                Log.d(TAG, "receiveFile: $fileName")

                // Assume a file size is passed (optional but helpful for tracking progress)
                val fileSize = 1024 * 1024L // Placeholder for file size

                val file = File(getDownloadFolder(), fileName)
                val outputStream = FileOutputStream(file)

                val buffer = ByteArray(1024)
                var bytesTransferred = 0L
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesTransferred += bytesRead

                    // Update progress on UI thread
                    runOnUiThread {
                        val progress = (bytesTransferred * 100 / fileSize).toInt()
                        binding.progressBar.progress = progress
                        binding.progressTextView.text = "Received: $progress%"
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                socket.close()

                runOnUiThread {
                    Toast.makeText(
                        this@ReceiverActivity,
                        "File received: $fileName",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error receiving file", e)
                runOnUiThread {
                    Toast.makeText(
                        this@ReceiverActivity,
                        "Error receiving file: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    }

    private fun getDownloadFolder(): String {
        val downloadsFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder = File(downloadsFolder, "ShareItTransfer")
        if (!folder.exists()) {
            folder.mkdir() // Create the folder if it doesn't exist
        }
        return folder.absolutePath
    }


    @SuppressLint("MissingPermission")
    private fun removeGroupIfNeed(): Boolean {

        wifiP2pManager.requestGroupInfo(wifiP2pChannel) { group ->
            group?.let {
                wifiP2pManager.removeGroup(
                    wifiP2pChannel,
                    object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            Log.d(TAG, "onSuccess: removeGroupIfNeed Receiver")
                        }

                        override fun onFailure(reason: Int) {
                            Log.d(TAG, "onFailure: removeGroupIfNeed Receiver")
                        }
                    })
            }

        }
        return true
    }

    override fun wifiP2pEnabled(enabled: Boolean) {
        Log.d(TAG, "wifiP2pEnabled: Receiver $enabled")
    }

    override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
        Log.d(TAG, "onConnectionInfoAvailable: Receiver $wifiP2pInfo")
        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            isConnectionFormed = true
            Log.d(TAG, "onConnectionInfoAvailable: ready To receiver Receiver")
        }
    }

    override fun onDisconnection() {
        Log.d(TAG, "onDisconnection: Receiver")
    }

    override fun onSelfDeviceAvailable(device: WifiP2pDevice) {
        Log.d(TAG, "onSelfDeviceAvailable: Receiver $device")
    }

    override fun onPeersAvailable(devices: List<WifiP2pDevice>) {
        Log.d(TAG, "onPeersAvailable: Receiver $devices")
    }


}