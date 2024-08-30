package com.example.stelite_counter

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DroneLottie : AppCompatActivity() {

    private lateinit var backButton : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_drone_lottie)

        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

    }
}