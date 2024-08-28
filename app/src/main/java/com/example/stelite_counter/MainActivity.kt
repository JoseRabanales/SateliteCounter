package com.example.stelite_counter

import android.Manifest
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
    private lateinit var testButton: Button
    private lateinit var resetButton: ImageView
    private lateinit var exportNpastaButton: Button
    private lateinit var deviceSpinner: Spinner
    private lateinit var counterText: TextView
    private lateinit var secondCounterText: TextView
    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var UUIDServiceTX: UUID
    private lateinit var UUIDCharTX: UUID
    private lateinit var UUIDServiceRX: UUID
    private lateinit var UUIDCharRX: UUID

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
                    val state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> updateUI(false, "No conectado")
                        BluetoothAdapter.STATE_ON -> updateUI(true, "No conectado")
                    }
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!deviceList.contains(it)) {
                            deviceList.add(it)
                            adapter.add(it.name)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                                REQUEST_ENABLE_BT
                            )
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
        testButton = findViewById(R.id.testButton)
        resetButton = findViewById(R.id.resetButton)
        exportNpastaButton = findViewById(R.id.exportNpastaButton)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), REQUEST_ENABLE_BT
            )
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
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position >= 0 && position < deviceList.size) {
                    val selectedDevice = deviceList[position]
                    connectToDevice(selectedDevice)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Dispositivo no válido seleccionado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No hacer nada
            }
        }

        counterText.setOnClickListener {
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER

            AlertDialog.Builder(this)
                .setTitle("Ingresar número")
                .setView(input)
                .setPositiveButton("Aceptar") { dialog, _ ->
                    val enteredNumber = input.text.toString()
                    if (enteredNumber.isNotEmpty()) {
                        counterText.text = enteredNumber
                        val data = "$enteredNumber,$secondCounter,none"
                        sendValue(data)
                    }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        }

        bluetoothStatusCircle.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.disable()
                } else {
                    bluetoothAdapter.enable()
                }
            } else {
                Toast.makeText(this, "Permiso de Bluetooth denegado", Toast.LENGTH_SHORT).show()
            }
        }


        testButton.setOnClickListener {
            disableButtonsFor(1000)
            val data = "$counter,$secondCounter,teste"
            sendValue(data)
        }

        resetButton.setOnClickListener {
            disableButtonsFor(1000)
            val data = "$counter,$secondCounter,reset"
            sendValue(data)
        }

        exportNpastaButton.setOnClickListener {
            disableButtonsFor(1000)
            val data = "$counter,$secondCounter,export"
            sendValue(data)
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

    private fun disableButtonsFor(duration: Long) {
        testButton.isEnabled = false
        resetButton.isEnabled = false
        exportNpastaButton.isEnabled = false

        Handler(Looper.getMainLooper()).postDelayed({
            testButton.isEnabled = true
            resetButton.isEnabled = true
            exportNpastaButton.isEnabled = true
        }, duration)
    }


    private fun updateUI(bluetoothEnabled: Boolean, device: String) {
        if (bluetoothEnabled) {
            bluetoothStatusCircle.background =
                ContextCompat.getDrawable(this, R.drawable.bluetooth_status_border_green)
            bluetoothStatusCircletwo.background.setTint(getColor(R.color.green_light))
        } else {
            bluetoothStatusCircle.background =
                ContextCompat.getDrawable(this, R.drawable.bluetooth_status_border)
            bluetoothStatusCircletwo.background.setTint(getColor(R.color.red_light))
        }
    }

    private fun updateCounter() {
        counterText.text = counter.toString()
    }

    private fun updateSecondCounter() {
        secondCounterText.text = secondCounter.toString()
        if (secondCounter >= 7) {
            secondCounterText.setTextColor(getColor(android.R.color.holo_green_light))
        } else if (secondCounter in 3..6) {
            secondCounterText.setTextColor(Color.YELLOW)
        } else {
            secondCounterText.setTextColor(getColor(android.R.color.holo_red_light))
        }
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
                        Toast.makeText(
                            this@MainActivity,
                            "Conectado a ${gatt?.device?.name}",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Descubrir servicios disponibles en el dispositivo BLE
                        gatt?.discoverServices()
                    }
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
                        Toast.makeText(
                            this@MainActivity,
                            "Error de conexión: $status",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.let {
                    // Variables para almacenar las características TX y RX
                    var rxCharacteristic: BluetoothGattCharacteristic? = null
                    var txCharacteristic: BluetoothGattCharacteristic? = null

                    // Iterar sobre los servicios descubiertos
                    for (service in it.services) {
                        println("Servicio descubierto: ${service.uuid}")

                        // Iterar sobre las características del servicio
                        for (characteristic in service.characteristics) {
                            println("Característica descubierta: ${characteristic.uuid}")

                            // Verificar si es la característica TX
                            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                                txCharacteristic = characteristic
                                UUIDCharTX =
                                    characteristic.uuid  // Asignar la característica TX a la variable global
                                UUIDServiceTX =
                                    service.uuid  // Asignar el servicio a la variable global
                                println("  -> Característica TX encontrada")
                            }
                            // Verificar si es la característica RX
                            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 ||
                                characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
                            ) {
                                rxCharacteristic = characteristic
                                UUIDCharRX =
                                    characteristic.uuid  // Asignar la característica RX a la variable global
                                UUIDServiceRX =
                                    service.uuid  // Asignar el servicio a la variable global
                                println("  -> Característica RX encontrada")
                            }
                        }

                        // Si encontramos tanto RX como TX, configurar notificaciones y salir del bucle
                        if (rxCharacteristic != null && txCharacteristic != null) {
                            // Habilitar notificaciones en RX
                            gatt.setCharacteristicNotification(rxCharacteristic, true)

                            val descriptor = rxCharacteristic.getDescriptor(
                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")  // UUID del descriptor de notificación
                            )
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)

                            Toast.makeText(
                                this@MainActivity,
                                "Servicios y características configurados",
                                Toast.LENGTH_SHORT
                            ).show()
                            break  // Salir del bucle una vez que hemos encontrado lo que necesitamos
                        }
                    }

                    if (rxCharacteristic == null || txCharacteristic == null) {
                        Toast.makeText(
                            this@MainActivity,
                            "No se encontraron características RX/TX válidas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Error al descubrir servicios: $status",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            characteristic?.let {
                val value = it.value // Valor en bytes recibido
                val stringValue = String(value) // Convertir a string

                runOnUiThread {
                    // Procesar la cadena recibida
                    val values = stringValue.split(",") // Separar por comas

                    if (values.size >= 2) {
                        try {
                            // Asignar los valores
                            counter = values[0].toInt()
                            secondCounter = values[1].toInt()

                            // Actualizar la UI con los nuevos valores
                            updateCounter()
                        } catch (e: NumberFormatException) {
                            // Manejar la excepción si los valores no se pueden convertir a entero
                            Toast.makeText(
                                this@MainActivity,
                                "Error al convertir los valores: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Si la cadena no contiene los valores esperados
                        Toast.makeText(
                            this@MainActivity,
                            "Datos incompletos recibidos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            runOnUiThread {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Toast.makeText(
                        this@MainActivity,
                        "Valor escrito exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val errorMessage = when (status) {
                        BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "Escritura no permitida"
                        BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "Autenticación insuficiente"
                        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "Solicitud no soportada"
                        BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "Encriptación insuficiente"
                        else -> "Error desconocido: $status"
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Error al escribir característica: $errorMessage",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun closeBluetoothConnection() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private fun sendValue(value: String) {
        val valueBytes = value.toByteArray()

        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(UUIDServiceRX)
            if (service != null) {
                val txCharacteristic = service.getCharacteristic(UUIDCharRX)

                if (txCharacteristic != null) {
                    txCharacteristic.value = valueBytes

                    // Verificar las propiedades antes de escribir
                    if (txCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                        val success = gatt.writeCharacteristic(txCharacteristic)
                        if (!success) {
                            Toast.makeText(
                                this,
                                "Error al iniciar la escritura",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "La característica TX no admite escritura",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Característica TX no encontrada", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this, "Servicio TX no encontrado", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "BluetoothGatt no disponible", Toast.LENGTH_SHORT).show()
        }
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