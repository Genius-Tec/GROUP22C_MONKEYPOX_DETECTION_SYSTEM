package com.example.yoporth

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val imageView = findViewById<ImageView>(R.id.imageViewSplash)
        
        try {
            // Using "icon.png" from assets as requested
            val inputStream = assets.open("icon.png")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            imageView.setImageBitmap(bitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }, 3000) // 3 seconds delay
    }
}
