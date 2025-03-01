package dev.bilal.wifi.file.transfer.demo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import dev.bilal.wifi.file.transfer.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setListener()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent()
            intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            val uri = Uri.fromParts("package", this.packageName, null)
            intent.setData(uri)
            startActivity(intent)
        }
    }

    private fun setListener() {
        binding.asSender.setOnClickListener {
            startActivity(Intent(this@MainActivity, SenderActivity::class.java))
        }
        binding.asReceiver.setOnClickListener {
            startActivity(Intent(this@MainActivity, ReceiverActivity::class.java))
        }

    }


    companion object {
        const val PORT = 8080
    }

}