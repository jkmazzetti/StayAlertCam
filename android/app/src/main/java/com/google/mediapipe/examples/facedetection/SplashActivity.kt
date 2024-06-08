package com.google.mediapipe.examples.facedetection

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Duración de la pantalla de splash (en milisegundos)
        val splashDuration = 3000L

        Handler(Looper.getMainLooper()).postDelayed({
            // Iniciar la MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            // Finalizar SplashActivity para que no se pueda volver a ella
            finish()
        }, splashDuration)
    }
}