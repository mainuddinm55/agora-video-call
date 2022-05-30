package info.learncoding.arogavideocall.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import info.learncoding.arogavideocall.R
import info.learncoding.arogavideocall.databinding.ActivityMainBinding
import info.learncoding.arogavideocall.ui.call.CallActivity

class MainActivity : AppCompatActivity() {

    private var redirectCall = false
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        var granted = true
        map.entries.forEach {
            if (!it.value) {
                granted = false
            }
        }
        if (granted && redirectCall) {
            CallActivity.buildIntent(this).also {
                startActivity(it)
            }
            redirectCall = false
        }
    }

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        if (!isPermissionGranted()) {
            redirectCall = false
            requestPermission()
        }

        binding.joinButton.setOnClickListener {
            if (isPermissionGranted()) {
                CallActivity.buildIntent(this).also {
                    startActivity(it)
                }
            } else {
                redirectCall = true
                requestPermission()
            }
        }
    }


    private fun isPermissionGranted(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }
}