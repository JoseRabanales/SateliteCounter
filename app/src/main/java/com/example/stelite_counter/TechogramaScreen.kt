package com.example.stelite_counter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class TechogramaScreen : AppCompatActivity() {

    private lateinit var backButton : Button
    private lateinit var buttonOptionOne : RelativeLayout
    private lateinit var buttonOptionTwo : RelativeLayout
    private lateinit var buttonOptionThree : RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_techograma_screen)

        backButton = findViewById(R.id.backButton)
        buttonOptionOne = findViewById(R.id.button6_layout)
        buttonOptionTwo = findViewById(R.id.button5)
        buttonOptionThree = findViewById(R.id.button4)

        backButton.setOnClickListener {
            finish()
        }

        buttonOptionOne.setOnClickListener{

            val intent = Intent(this, DroneLottie::class.java)
            startActivity(intent)

        }
        buttonOptionTwo.setOnClickListener{

            val intent = Intent(this, DroneLottie::class.java)
            startActivity(intent)

        }
        buttonOptionThree.setOnClickListener{

            val intent = Intent(this, DroneLottie::class.java)
            startActivity(intent)

        }

    }
}