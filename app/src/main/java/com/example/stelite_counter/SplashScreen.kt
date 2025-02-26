package com.example.stelite_counter

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed({
            val intent = Intent(this, HomeSreen::class.java)
            startActivity(intent)
            finish()
        }, 1000)


    }
}