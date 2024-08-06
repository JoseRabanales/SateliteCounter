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
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private lateinit var bluetoothStatusText: TextView
    private lateinit var incrementButton: ImageButton
    private lateinit var decrementButton: ImageButton
    private lateinit var increment10Button: ImageButton
    private lateinit var decrement10Button: ImageButton
    private lateinit var testButton: Button
    private lateinit var deviceName: TextView
    private lateinit var counterText: TextView
    private lateinit var secondCounterText: TextView
    private lateinit var timeText: TextView
    private lateinit var dateText: TextView

    private var counter: Int = 0
    private var secondCounter: Int = 6
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private val MY_UUID: UUID = UUID.randomUUID()

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
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bluetoothStatusCircle = findViewById(R.id.bluetoothStatusCircle)
        bluetoothStatusText = findViewById(R.id.bluetoothStatusText)
        deviceName = findViewById(R.id.deviceName)
        counterText = findViewById(R.id.counterText)
        secondCounterText = findViewById(R.id.secondCounterText)
        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        incrementButton = findViewById(R.id.incrementButton)
        decrementButton = findViewById(R.id.decrementButton)
        increment10Button = findViewById(R.id.increment10Button)
        decrement10Button = findViewById(R.id.decrement10Button)
        testButton = findViewById(R.id.testButton)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_ENABLE_BT)
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
            sendCounterValue()
        }

        decrementButton.setOnClickListener {
            counter--
            updateCounter()
            sendCounterValue()
        }

        increment10Button.setOnClickListener {
            counter += 10
            updateCounter()
            sendCounterValue()
        }

        decrement10Button.setOnClickListener {
            counter -= 10
            updateCounter()
            sendCounterValue()
        }

        testButton.setOnClickListener {
            Toast.makeText(this, "Test Button Clicked", Toast.LENGTH_SHORT).show()
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)

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
            bluetoothStatusCircle.background.setTint(getColor(android.R.color.holo_green_light))
            bluetoothStatusText.text = "On"
        } else {
            bluetoothStatusCircle.background.setTint(getColor(android.R.color.holo_red_light))
            bluetoothStatusText.text = "Off"
        }
        deviceName.text = device
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

    private fun sendCounterValue() {
        outputStream?.let {
            try {
                it.write(counter.toString().toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Thread {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "Conectado a ${device.name}", Toast.LENGTH_SHORT).show()
                }
                listenForIncomingData() // Start listening for incoming data
            } catch (e: IOException) {
                e.printStackTrace()
                closeBluetoothConnection()
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
        val values = data.split(",").mapNotNull { it.toIntOrNull() }
        if (values.size >= 2) {
            counter = values[0]
            secondCounter = values[1]
            updateCounter()
            updateSecondCounter()
        }
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
