package com.example.stelite_counter

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.stelite_counter.databinding.ActivityBluetoothScreenBinding
import com.google.android.material.switchmaterial.SwitchMaterial

class BluetoothScreen : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSwitch: SwitchMaterial
    private lateinit var binding: ActivityBluetoothScreenBinding

    private val requestBluetoothEnableLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Handle the result if necessary
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        bluetoothSwitch = findViewById(R.id.bluetoothSwitch)

        // Verifica y solicita permisos necesarios
        checkBluetoothPermissions()

        // Actualiza el estado del Switch según el estado actual del Bluetooth
        bluetoothSwitch.isChecked = bluetoothAdapter.isEnabled

        // Configura el listener para el Switch
        binding.bluetoothSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Encender Bluetooth
                if (!bluetoothAdapter.isEnabled) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    requestBluetoothEnableLauncher.launch(intent)
                }
            } else {
                // Apagar Bluetooth
                if (bluetoothAdapter.isEnabled) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        bluetoothAdapter.disable()
                        // Usa un handler para asegurarte de que el estado del Switch se actualice correctamente
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (bluetoothAdapter.isEnabled) {
                                Toast.makeText(this, "No se pudo apagar el Bluetooth", Toast.LENGTH_SHORT).show()
                            }
                        }, 1000)
                    }
                }
            }
        }
    }

    private fun checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 2)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1, 2 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permisos necesarios para Bluetooth no concedidos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
