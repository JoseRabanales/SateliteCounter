package com.example.stelite_counter

import AnimationDialog
import BluetoothService
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView

class MainActivity : AppCompatActivity() {

    private lateinit var testButton: Button
    private lateinit var exportNpastaButton: Button
    private lateinit var btnEnviar: Button
    private lateinit var backButton: Button
    private lateinit var rebootButton: Button
    private lateinit var counterText: TextView
    private lateinit var inputNumberText: EditText
    private lateinit var secondCounterText: TextView
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var lottieOverlay: FrameLayout
    private lateinit var prontoButton: Button
    private lateinit var metroText: TextView
    private lateinit var EditTextLayout: LinearLayout

    private var isAnimationDone = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        counterText = findViewById(R.id.counterText)
        inputNumberText = findViewById(R.id.inputNumberText)
        btnEnviar = findViewById(R.id.btnEnviar)
        secondCounterText = findViewById(R.id.secondCounterText)
        testButton = findViewById(R.id.testButton)
        rebootButton = findViewById(R.id.rebootButton)
        exportNpastaButton = findViewById(R.id.exportNpastaButton)
        backButton = findViewById(R.id.backButton)
        prontoButton = findViewById(R.id.prontoButton)

        metroText = findViewById(R.id.metroText)

        EditTextLayout = findViewById(R.id.linearLayout)

        // Inflar el layout de la animación desde gears_animation.xml
        val inflater = layoutInflater
        val lottieLayout = inflater.inflate(R.layout.gears_animation, null)

        // Agregar el layout inflado al root view del activity_main
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(lottieLayout)

        // Inicializar las vistas de la animación
        lottieOverlay = lottieLayout.findViewById(R.id.lottieOverlay)
        lottieAnimationView = lottieLayout.findViewById(R.id.lottieAnimationView)



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

        checkPermissions()

        btnEnviar.setOnClickListener {
            val enteredNumber = inputNumberText.text.toString()

            if (enteredNumber.isNotEmpty()) {

                    counterText.text = enteredNumber
                    val data = "$enteredNumber,${BluetoothService.sat},none"
                    BluetoothService.sendData(data)

                    inputNumberText.text.clear()

                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(inputNumberText.windowToken, 0)

            } else {
                Toast.makeText(this, "Por favor ingrese un número", Toast.LENGTH_SHORT).show()
            }
        }

        testButton.setOnClickListener {
            if (testButton.text == "Reset"){
                val data = "${BluetoothService.meters},${BluetoothService.sat},reset"
                BluetoothService.sendData(data)
                testButton.text = "Teste"
            } else {
                val data = "${BluetoothService.meters},${BluetoothService.sat},teste"
                BluetoothService.sendData(data)
            }
            disableButtonsFor(1000)
        }

        rebootButton.setOnClickListener {
            disableButtonsFor(1000)
            val data = "${BluetoothService.meters},${BluetoothService.sat},reboot"
            BluetoothService.sendData(data)
        }

        prontoButton.setOnClickListener {

            if (!isAnimationDone) {
                counterText.visibility = View.GONE
                metroText.visibility = View.GONE
                exportNpastaButton.visibility = View.GONE
                rebootButton.visibility = View.GONE
                EditTextLayout.visibility = View.GONE

                prontoButton.animate()
                    .translationYBy(-100f)
                    .setDuration(600)
                    .start()

                isAnimationDone = true
            }

            val data = "${BluetoothService.meters},${BluetoothService.sat},pronto"
            BluetoothService.sendData(data)

        }

        exportNpastaButton.setOnClickListener {
            disableButtonsFor(1000)
            val data = "${BluetoothService.meters},${BluetoothService.sat},exportAndroid"
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
        rebootButton.isEnabled = false
        exportNpastaButton.isEnabled = false

        testButton.postDelayed({
            testButton.isEnabled = true
            rebootButton.isEnabled = true
            exportNpastaButton.isEnabled = true
        }, duration)
    }

    private fun updateUI() {
        runOnUiThread {
            counterText.text = BluetoothService.meters
            secondCounterText.text = BluetoothService.sat

            if (BluetoothService.action == "showReset") {
                testButton.text = "Reset"
                BluetoothService.action = "none"
            } else if (BluetoothService.action == "pronto") {
                Toast.makeText(this, "Pronto pa decolar.", Toast.LENGTH_SHORT).show()
                BluetoothService.action = "none"
            }

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
        const val REQUEST_WRITE_EXTERNAL_STORAGE = 2

    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothService.removeObserver { updateUI() }
    }
}