package com.example.yoporth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.yoporth.databinding.ActivityMainBinding

/**
 * Note: Real-time detection has been disabled as per user request.
 * This activity is kept for reference or future use but is not accessible from the UI.
 */
class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        // This screen is currently unused.
    }

    override fun onEmptyDetect() {
        // Implementation for interface compliance
    }

    override fun onClassificationResult(label: String, confidence: Float, inferenceTime: Long) {
        // Implementation for interface compliance
    }
}
