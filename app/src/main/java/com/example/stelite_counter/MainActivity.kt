package com.example.stelite_counter

import android.Manifest
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothStatusCircle: View
    private lateinit var bluetoothStatusText: ImageView
    private lateinit var bluetoothStatusCircletwo: View
    private lateinit var incrementButton: ImageButton
    private lateinit var decrementButton: ImageButton
    private lateinit var increment10Button: ImageButton
    private lateinit var decrement10Button: ImageButton
    private lateinit var testButton: Button
    private lateinit var exportNpastaButton: Button
    private lateinit var deviceSpinner: Spinner
    private lateinit var counterText: TextView
    private lateinit var secondCounterText: TextView
    private lateinit var timeText: TextView
    private lateinit var dateText: TextView

    private var counter: Int = 0
    private var secondCounter: Int = 0
    private var bluetoothGatt: BluetoothGatt? = null

    private val deviceList: MutableList<BluetoothDevice> = ArrayList()
    private lateinit var adapter: ArrayAdapter<String>

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> updateUI(false, "No conectado")
                        BluetoothAdapter.STATE_ON -> updateUI(true, "No conectado")
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!deviceList.contains(it)) {
                            deviceList.add(it)
                            adapter.add(it.name)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_ENABLE_BT)
                            return
                        }
                        updateUI(true, device.name ?: "Dispositivo desconocido")
                        connectToDevice(device)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    updateUI(true, "No conectado")
                    closeBluetoothConnection()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bluetoothStatusCircletwo = findViewById(R.id.bluetoothStatusCircletwo)
        bluetoothStatusCircle = findViewById(R.id.bluetoothStatusCircle)
        bluetoothStatusText = findViewById(R.id.bluetoothStatusText)
        deviceSpinner = findViewById(R.id.deviceSpinner)
        counterText = findViewById(R.id.counterText)
        secondCounterText = findViewById(R.id.secondCounterText)
        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        incrementButton = findViewById(R.id.incrementButton)
        decrementButton = findViewById(R.id.decrementButton)
        increment10Button = findViewById(R.id.increment10Button)
        decrement10Button = findViewById(R.id.decrement10Button)
        testButton = findViewById(R.id.testButton)
        exportNpastaButton = findViewById(R.id.exportNpastaButton)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_ENABLE_BT)
        }

        // Configuración del Adapter para el Spinner
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        pairedDevices?.forEach { device ->
            deviceList.add(device)
        }

        val deviceNames = deviceList.map { it.name }.toMutableList()

        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSpinner.adapter = adapter

        deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < deviceList.size) {
                    val selectedDevice = deviceList[position]
                    connectToDevice(selectedDevice)
                } else {
                    Toast.makeText(this@MainActivity, "Dispositivo no válido seleccionado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No hacer nada
            }
        }

        bluetoothStatusCircle.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if (bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.disable()
                } else {
                    bluetoothAdapter.enable()
                }
            } else {
                Toast.makeText(this, "Permiso de Bluetooth denegado", Toast.LENGTH_SHORT).show()
            }
        }

        incrementButton.setOnClickListener {
            counter++
            updateCounter()
            sendCounterValue("none")
        }

        decrementButton.setOnClickListener {
            if (counter > 0) {
                counter--
                updateCounter()
                sendCounterValue("none")
            }
        }

        increment10Button.setOnClickListener {
            counter += 10
            updateCounter()
            sendCounterValue("none")
        }

        decrement10Button.setOnClickListener {
            if (counter >= 10) {
                counter -= 10
                updateCounter()
                sendCounterValue("none")
            }
        }

        testButton.setOnClickListener {
            sendAction("test")
        }

        exportNpastaButton.setOnClickListener {
            sendAction("export")
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_FOUND)
        }
        registerReceiver(bluetoothReceiver, filter)

        // Iniciar descubrimiento de dispositivos
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.startDiscovery()
        }

        // Initialize UI
        if (bluetoothAdapter.isEnabled) {
            updateUI(true, "No conectado")
        } else {
            updateUI(false, "No conectado")
        }

        // Set time and date
        updateTimeAndDate()

        // Initialize the second counter
        updateSecondCounter()
    }

    private fun updateUI(bluetoothEnabled: Boolean, device: String) {
        if (bluetoothEnabled) {
            bluetoothStatusCircle.background = ContextCompat.getDrawable(this, R.drawable.bluetooth_status_border_green)
            bluetoothStatusCircletwo.background.setTint(getColor(R.color.green_light))
        } else {
            bluetoothStatusCircle.background = ContextCompat.getDrawable(this, R.drawable.bluetooth_status_border)
            bluetoothStatusCircletwo.background.setTint(getColor(R.color.red_light))
        }
    }

    private fun updateCounter() {
        counterText.text = counter.toString()
    }

    private fun updateSecondCounter() {
        secondCounterText.text = secondCounter.toString()
        if (secondCounter > 7) {
            secondCounterText.setTextColor(getColor(android.R.color.holo_green_light))
        } else {
            secondCounterText.setTextColor(getColor(android.R.color.holo_red_light))
        }
        secondCounter = 0
        Handler(Looper.getMainLooper()).postDelayed({
            updateSecondCounter()
        }, 1000)
    }

    private fun updateTimeAndDate() {
        val currentTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        dateText.text = dateFormat.format(currentTime)
        timeText.text = timeFormat.format(currentTime)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Solicitar permisos si no están concedidos
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_ENABLE_BT
            )
            return
        }

        // Intentar conectar al dispositivo BLE
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    runOnUiThread {
                        // Conexión exitosa
                        updateUI(true, gatt?.device?.name ?: "Desconocido")
                        Toast.makeText(this@MainActivity, "Conectado a ${gatt?.device?.name}", Toast.LENGTH_SHORT).show()
                    }
                    gatt?.discoverServices()  // Descubrir servicios disponibles en el dispositivo BLE
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    runOnUiThread {
                        // Desconexión
                        updateUI(true, "No conectado")
                        Toast.makeText(this@MainActivity, "Desconectado", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {
                    runOnUiThread {
                        // Error en la conexión
                        Toast.makeText(this@MainActivity, "Error de conexión: $status", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Servicios descubiertos exitosamente
                // Aquí puedes interactuar con los servicios y características del dispositivo
            } else {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error al descubrir servicios: $status", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Manejar la lectura de características
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Manejar la escritura de características
            }
        }
    }


    private fun closeBluetoothConnection() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private fun sendCounterValue(value: String) {
        // Implementar el envío del valor del contador al dispositivo BLE
    }

    private fun sendAction(action: String) {
        // Implementar el envío de la acción al dispositivo BLE
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        closeBluetoothConnection()
    }
}