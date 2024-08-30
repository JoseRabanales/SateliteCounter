package com.example.stelite_counter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var testButton: Button
    private lateinit var exportNpastaButton: Button
    private lateinit var btnEnviar: Button
    private lateinit var backButton: Button
    private lateinit var resetButton: Button
    private lateinit var counterText: TextView
    private lateinit var inputNumberText: EditText
    private lateinit var secondCounterText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        counterText = findViewById(R.id.counterText)
        inputNumberText = findViewById(R.id.inputNumberText)
        btnEnviar = findViewById(R.id.btnEnviar)
        secondCounterText = findViewById(R.id.secondCounterText)
        testButton = findViewById(R.id.testButton)
        resetButton = findViewById(R.id.resetButton)
        exportNpastaButton = findViewById(R.id.exportNpastaButton)
        backButton = findViewById(R.id.backButton)

        // Agregar un observador para actualizar la UI cuando cambien meters o sat en el BluetoothService
        BluetoothService.addObserver {
            updateUI()
        }

        runOnUiThread {
            counterText.text = BluetoothService.meters
            secondCounterText.text = BluetoothService.sat

            updateSecondCounter()
        }

        backButton.setOnClickListener {
            finish()
        }

        // Asegurarse de que los permisos necesarios están concedidos
        checkPermissions()

        // Establecer listeners para los botones
        btnEnviar.setOnClickListener {
            val enteredNumber = inputNumberText.text.toString()

            if (enteredNumber.isNotEmpty()) {
                val number = enteredNumber.toIntOrNull()

                if (number != null && number in 10..50) {
                    counterText.text = enteredNumber
                    val data = "$enteredNumber,${BluetoothService.sat},none"
                    BluetoothService.sendData(data)
                } else {
                    Toast.makeText(this, "El número debe estar entre 10 y 50", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor ingrese un número", Toast.LENGTH_SHORT).show()
            }
        }

        testButton.setOnClickListener {
            disableButtonsFor(1000)
            val data = "${BluetoothService.meters},${BluetoothService.sat},teste"
            BluetoothService.sendData(data)
        }

        resetButton.setOnClickListener {
            disableButtonsFor(1000)
            val data = "${BluetoothService.meters},${BluetoothService.sat},reboot"
            BluetoothService.sendData(data)
        }

        exportNpastaButton.setOnClickListener {
            disableButtonsFor(1000)
            val data = "${BluetoothService.meters},${BluetoothService.sat},export"
            BluetoothService.sendData(data)
        }

        updateSecondCounter()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), REQUEST_ENABLE_BT
            )
        }
    }

    private fun disableButtonsFor(duration: Long) {
        testButton.isEnabled = false
        resetButton.isEnabled = false
        exportNpastaButton.isEnabled = false

        testButton.postDelayed({
            testButton.isEnabled = true
            resetButton.isEnabled = true
            exportNpastaButton.isEnabled = true
        }, duration)
    }

    private fun updateUI() {
        runOnUiThread {
            counterText.text = BluetoothService.meters
            secondCounterText.text = BluetoothService.sat

            updateSecondCounter()
        }
    }

    private fun updateSecondCounter() {
        secondCounterText.text = BluetoothService.sat
        secondCounterText.setTextColor(
            when {
                (BluetoothService.sat.toIntOrNull() ?: 0) >= 7 -> getColor(android.R.color.holo_green_light)
                (BluetoothService.sat.toIntOrNull() ?: 0) in 3..6 -> getColor(android.R.color.holo_orange_light)
                else -> getColor(android.R.color.holo_red_light)
            }
        )
        secondCounterText.postDelayed({
            updateSecondCounter()
        }, 1000)
    }

    companion object {
        const val REQUEST_ENABLE_BT = 1
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothService.removeObserver { updateUI() }
    }
}