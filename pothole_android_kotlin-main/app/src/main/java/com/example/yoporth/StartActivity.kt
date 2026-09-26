package com.example.yoporth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.yoporth.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        DetectionStorage.init(this)
        
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_saved_reports -> {
                    startActivity(Intent(this, SavedReportsActivity::class.java))
                }
                R.id.nav_about_us -> {
                    showAboutUsDialog()
                }
                R.id.nav_contact_support -> {
                    showContactSupportDialog()
                }
            }
            true
        }

        binding.buttonOpenGallery.setOnClickListener {
            startGalleryAnalysisActivity()
        }
    }

    private fun showContactSupportDialog() {
        val supportText = """
            If you experience any problems while using Monkey Pox Detector, or if you have questions, suggestions, or feedback, please contact our support team. We are always ready to help you.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Contact Support")
            .setMessage(supportText)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAboutUsDialog() {
        val aboutText = """
            Monkey Pox Detector is a cutting-edge mobile application designed to assist in the early identification and classification of Monkey Pox (Mpox) symptoms. By leveraging advanced Artificial Intelligence and Deep Learning, the app analyzes skin lesions to provide immediate, preliminary feedback.

            Key Features:
            • AI-Powered Analysis: State-of-the-art image recognition for Mpox detection.
            • Real-time Screening: Analyze images directly from your camera or gallery.
            • AI Insights: Detailed explanations and healthcare advice powered by DeepSeek AI.
            • Privacy Focused: Secure handling of classification reports and history.

            Mission:
            Our goal is to improve public health outcomes by providing accessible diagnostic tools for infectious diseases. Early detection is vital for effective treatment and preventing community spread.

            Disclaimer:
            This application is intended for informational and educational purposes only. It is not a substitute for professional medical advice, diagnosis, or treatment. Always seek the advice of a qualified healthcare provider with any questions you may have regarding a medical condition.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("About Us")
            .setMessage(aboutText)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun startGalleryAnalysisActivity() {
        val intent = Intent(this, GalleryAnalysisActivity::class.java)
        startActivity(intent)
    }
}
