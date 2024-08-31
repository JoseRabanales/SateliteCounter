package com.example.stelite_counter

import BluetoothService
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class HomeSreen : AppCompatActivity() {

    private lateinit var bluetoothButton: Button
    private lateinit var cardViewCotecia: CardView
    private lateinit var imagecoteciaImageView: ImageView
    private lateinit var cardViewTrichograma: CardView
    private lateinit var cardView: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_sreen)
        bluetoothButton = findViewById(R.id.bluetoothButton)
        cardViewCotecia = findViewById(R.id.cardViewCotecia)
        cardViewTrichograma = findViewById(R.id.cardViewTrichograma)
        imagecoteciaImageView = findViewById(R.id.imagecoteciaImageView)
        cardView = findViewById(R.id.cardView)


        updateUI()

        cardViewCotecia.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        cardViewTrichograma.setOnClickListener {
            val intent = Intent(this, TechogramaScreen::class.java)
            startActivity(intent)
        }

        bluetoothButton.setOnClickListener{
            val intent = Intent(this, BluetoothScreen::class.java)
            startActivity(intent)
        }

        // Agregar un observador para actualizar la UI cuando cambien meters o sat en el BluetoothService
        BluetoothService.addObserver {
            updateUI()
        }

        cardView.setOnClickListener {
            val intent = Intent(this, DroneLottie::class.java)
            startActivity(intent)
        }

    }

    private fun toggleDrawableEnd() {
        val bluetoothDrawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.icon_bluetooth)
        val newDrawable: Drawable? = if (BluetoothService.deviceConnected != "none") {
            ContextCompat.getDrawable(this, R.drawable.bluetoothconnect_resized)
        } else {
            ContextCompat.getDrawable(this, R.drawable.bluetoothdisconnect_resized)
        }

        bluetoothButton.setCompoundDrawablesRelativeWithIntrinsicBounds(bluetoothDrawable, null, newDrawable, null)
    }

    private fun updateUI() {
        runOnUiThread {
            if (BluetoothService.deviceConnected != "none") {
                toggleDrawableEnd()
                bluetoothButton.text = getString(R.string.bluetooth_connect)

                if (BluetoothService.deviceConnected == "cotecia") {
                    cardViewCotecia.setCardBackgroundColor(getColor(R.color.yellow_light))
                    imagecoteciaImageView.setImageResource(R.drawable.imagencoteciaconnect)
                } else {
                    imagecoteciaImageView.setImageResource(R.drawable.imagecotecia)
                }
            } else {
                toggleDrawableEnd()
                bluetoothButton.text = getString(R.string.bluetooth_disconnect)
                cardViewCotecia.setCardBackgroundColor(getColor(R.color.white))
                imagecoteciaImageView.setImageResource(R.drawable.imagecotecia)
            }
        }
    }


}