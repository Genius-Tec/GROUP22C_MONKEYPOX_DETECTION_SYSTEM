package com.example.yoporth

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yoporth.databinding.ActivitySavedReportsBinding
import java.io.File

class SavedReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedReportsBinding
    private lateinit var sessionAdapter: SessionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Saved Reports"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerViews()
        loadData()
    }

    private fun setupRecyclerViews() {
        sessionAdapter = SessionsAdapter(emptyList()) { session ->
            showReportDetailsDialog(session)
        }
        binding.recyclerViewReports.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewReports.adapter = sessionAdapter
    }

    private fun showReportDetailsDialog(session: DetectionSession) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_report_detail, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val title = dialogView.findViewById<TextView>(R.id.detailTitle)
        val dateTime = dialogView.findViewById<TextView>(R.id.detailDateTime)
        val imageView = dialogView.findViewById<ImageView>(R.id.detailImageView)
        val diagnosis = dialogView.findViewById<TextView>(R.id.detailDiagnosis)
        val recommendation = dialogView.findViewById<TextView>(R.id.detailRecommendation)
        val closeButton = dialogView.findViewById<Button>(R.id.detailCloseButton)

        title.text = "Analysis Report"
        dateTime.text = "${session.date} at ${session.time}"
        diagnosis.text = session.locationName // The label with confidence

        // Find the image for this session
        val detections = DetectionStorage.getDetectionsForSession(session.id)
        if (detections.isNotEmpty() && detections[0].imagePath != null) {
            val file = File(detections[0].imagePath!!)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                imageView.setImageBitmap(bitmap)
            }
        }

        // Add recommendations based on detection label
        val label = session.locationName.lowercase()
        recommendation.text = when {
            label.contains("common rust") -> 
                "Common Rust: Use resistant hybrids and apply fungicides if infection occurs early. Rotate crops with non-grass species to reduce spore levels."
            label.contains("gray leaf spot") -> 
                "Gray Leaf Spot: Select tolerant hybrids and ensure good crop rotation. Fungicides can be effective if applied before widespread leaf damage."
            label.contains("northern leaf blight") -> 
                "Northern Leaf Blight: Practice deep tillage and rotation. Resistant maize varieties are the most effective management strategy."
            label.contains("healthy") -> 
                "Your maize plant appears healthy. Continue regular monitoring and ensure proper fertilization and irrigation."
            else -> "Consult an agricultural expert for detailed diagnosis and local treatment options."
        }

        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun loadData() {
        val sessions = DetectionStorage.getSessions()
        if (sessions.isEmpty()) {
            binding.recyclerViewReports.visibility = View.GONE
        } else {
            binding.recyclerViewReports.visibility = View.VISIBLE
            sessionAdapter.updateData(sessions)
        }
    }
}
