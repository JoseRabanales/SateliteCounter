package com.example.stelite_counter

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private val MY_UUID: UUID = UUID.randomUUID()

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
            sendAction("teste")
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
        // Actualización eliminada ya que el Spinner muestra los dispositivos
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
    }

    private fun updateTimeAndDate() {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        timeText.text = currentTime
        dateText.text = currentDate
    }

    private fun sendCounterValue(action: String) {
        // Asegúrate de que la conexión Bluetooth está activa
        if (bluetoothSocket?.isConnected == true) {
            val message = "$counter,$secondCounter,$action"
            try {
                outputStream?.write(message.toByteArray())
                Toast.makeText(this, "Datos enviados: $message", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al enviar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No conectado a ningún dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendAction(action: String) {
        sendCounterValue(action)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        // Asegúrate de que se ejecute en un hilo separado
        Thread {
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_ENABLE_BT)
                    return@Thread
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothAdapter.cancelDiscovery()
                bluetoothSocket?.connect()

                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "Conectado a ${device.name}", Toast.LENGTH_SHORT).show()
                }

                listenForIncomingData()

            } catch (e: IOException) {
                e.printStackTrace()
                // Cerrar la conexión en caso de error
                closeBluetoothConnection()
                // Mostrar un mensaje de error en el hilo principal
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "Error al conectar con el dispositivo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }


    private fun listenForIncomingData() {
        val buffer = ByteArray(1024)
        var bytes: Int

        try {
            while (true) {
                bytes = inputStream?.read(buffer) ?: break
                val incomingMessage = String(buffer, 0, bytes)
                runOnUiThread {
                    handleIncomingData(incomingMessage)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun handleIncomingData(data: String) {
        val values = data.split(",")
        if (values.size == 3) {
            try {
                counter = values[0].toInt()
                secondCounter = values[1].toInt()
                val action = values[2]
                updateCounter()
                updateSecondCounter()
                // Manejar acción "export"
                if (action == "export") {
                    saveNpastaContent(data)
                }
                // No hacer nada si recibe "teste"
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
    }

    private fun saveNpastaContent(content: String) {
        val filename = "Npasta.txt"
        val file = File(filesDir, filename)
        file.writeText(content)
        Toast.makeText(this, "Contenido guardado en $filename", Toast.LENGTH_SHORT).show()
    }

    private fun closeBluetoothConnection() {
        try {
            outputStream?.close()
            inputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        closeBluetoothConnection()
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }
}
