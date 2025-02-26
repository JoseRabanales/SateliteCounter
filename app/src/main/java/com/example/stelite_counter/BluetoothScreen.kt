package com.example.stelite_counter

import BluetoothService
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class BluetoothScreen : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSwitch: SwitchMaterial
    private lateinit var pairedDevicesListView: ListView
    private lateinit var disconnectTextView: Button
    private lateinit var backButton: Button



    private val requestBluetoothEnableLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (bluetoothAdapter.isEnabled) {
                updatePairedDevicesList()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_screen)

        val bluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        bluetoothSwitch = findViewById(R.id.bluetoothSwitch)
        pairedDevicesListView = findViewById(R.id.pairedDevicesListView)
        disconnectTextView = findViewById(R.id.disconnectButton)
        backButton = findViewById(R.id.backButton)

        BluetoothService.addObserver {
            updateUI()
        }

        backButton.setOnClickListener {
            finish()
        }

        checkBluetoothPermissions()
        updateUI()

        bluetoothSwitch.isChecked = bluetoothAdapter.isEnabled

        bluetoothSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!bluetoothAdapter.isEnabled) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    requestBluetoothEnableLauncher.launch(intent)
                }
            } else {
                if (bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.disable()
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (bluetoothAdapter.isEnabled) {
                            Toast.makeText(
                                this,
                                "No se pudo apagar el Bluetooth",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        updatePairedDevicesList()
                    }, 1000)
                }
            }
        }

        pairedDevicesListView.setOnItemClickListener { parent, view, position, id ->
            val selectedDevice = parent.getItemAtPosition(position) as BluetoothDevice
            BluetoothService.selectDevice(selectedDevice)
            BluetoothService.connectToDevice(this, BluetoothService.gattCallback)
            Toast.makeText(this, "Conectado a ${selectedDevice.name}", Toast.LENGTH_SHORT).show()
            updateUI()
        }

        disconnectTextView.setOnClickListener {

            if (BluetoothService.deviceConnected != "none") {
                BluetoothService.disconnect()
                updateUI()
                Toast.makeText(this, "Dispositivos desconectado", Toast.LENGTH_SHORT).show()
            } else {
                updateUI()
                Toast.makeText(this, "Não há dispositivo", Toast.LENGTH_SHORT).show()
            }

        }

        updatePairedDevicesList()
    }

    private fun updateUI() {
        runOnUiThread {
            if (BluetoothService.deviceConnected != "none") {
                disconnectTextView.text = getString(R.string.device_disconnect)
                disconnectTextView.isEnabled = true
                disconnectTextView.alpha = 1.0f
            } else {
                disconnectTextView.text = getString(R.string.device_connect)
                disconnectTextView.isEnabled = false
                disconnectTextView.alpha = 0.5f
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualiza la lista de dispositivos emparejados cuando la actividad vuelve a estar en primer plano
        updatePairedDevicesList()
    }

    private fun checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1
            )
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                2
            )
        }
    }

    private fun updatePairedDevicesList() {
        if (bluetoothAdapter.isEnabled) {
            val pairedDevices = bluetoothAdapter.bondedDevices
            val deviceList = pairedDevices.toList()

            if (deviceList.isNotEmpty()) {
                val adapter = object :
                    ArrayAdapter<BluetoothDevice>(this, R.layout.list_item_bluetooth, deviceList) {
                    override fun getView(
                        position: Int,
                        convertView: android.view.View?,
                        parent: android.view.ViewGroup
                    ): android.view.View {
                        val view = convertView ?: layoutInflater.inflate(
                            R.layout.list_item_bluetooth,
                            parent,
                            false
                        )

                        val device = getItem(position)
                        val deviceName = view.findViewById<android.widget.TextView>(R.id.deviceName)
                        val icon = view.findViewById<android.widget.ImageView>(R.id.icon)

                        val displayName = if (device?.name == "M5Stack_BLE") {
                            "Aeroagri Device"
                        } else {
                            device?.name ?: "Dispositivo desconocido"
                        }

                        deviceName.text = displayName
                        icon.setImageResource(R.drawable.bluetooth_icon_resized)

                        return view
                    }
                }

                pairedDevicesListView.adapter = adapter
            }
            else {
                val noDevicesAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    listOf("Ningún dispositivo emparejado")
                )
                pairedDevicesListView.adapter = noDevicesAdapter
            }
        } else {
            val bluetoothOffAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf("Por favor, encienda el Bluetooth primero")
            )
            pairedDevicesListView.adapter = bluetoothOffAdapter
        }
        updateUI()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1, 2 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "Permisos necesarios para Bluetooth no concedidos",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    updatePairedDevicesList()
                }
            }
        }
    }
}

