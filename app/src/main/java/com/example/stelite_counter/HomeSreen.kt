package com.example.stelite_counter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView


class HomeSreen : AppCompatActivity() {

    private lateinit var bluetoothButton: Button
    private lateinit var cardViewCotecia: CardView
    private lateinit var cardViewTrichograma: CardView
    private lateinit var cardView: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_sreen)
        bluetoothButton = findViewById(R.id.bluetoothButton)
        cardViewCotecia = findViewById(R.id.cardViewCotecia)
        cardViewTrichograma = findViewById(R.id.cardViewTrichograma)
        cardView = findViewById(R.id.cardView)


        cardViewCotecia.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        cardViewTrichograma.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        bluetoothButton.setOnClickListener{
            val intent = Intent(this, BluetoothScreen::class.java)
            startActivity(intent)
        }

        cardView.setOnClickListener {
            // Crea el AlertDialog
            AlertDialog.Builder(it.context)
                .setTitle("Aviso")
                .setMessage("Esta opção estará disponível em breve")
                .setPositiveButton("OK", null)
                .show()
        }

    }
}