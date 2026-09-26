package com.example.yoporth

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.yoporth.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var detections: List<DetectionLocation> = emptyList()
    private var currentDetectionIndex = 0
    private var sessionId: String? = null
    private var session: DetectionSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        sessionId = intent.getStringExtra("SESSION_ID")
        Log.d("MapsActivity", "Session ID: $sessionId")

        if (sessionId != null) {
            detections = DetectionStorage.getDetectionsForSession(sessionId!!)
            Log.d("MapsActivity", "Found ${detections.size} detections for session")
            
            session = DetectionStorage.getSessions().find { it.id == sessionId }
            session?.let {
                binding.textViewDetailLocation1.text = it.locationName
                binding.textViewDetailLocation2.text = it.locationName
                binding.textViewDetailDistance.text = it.distance.replace(" km", "")
                binding.textViewDetailPotholes.text = it.totalDetections.toString()
            }
        }

        updateDetectionUI()

        binding.btnNext.setOnClickListener {
            if (currentDetectionIndex < detections.size - 1) {
                currentDetectionIndex++
                updateDetectionUI()
                moveMapToCurrent()
            }
        }

        binding.btnPrev.setOnClickListener {
            if (currentDetectionIndex > 0) {
                currentDetectionIndex--
                updateDetectionUI()
                moveMapToCurrent()
            }
        }

        binding.btnDeleteSession.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.btnGeneratePdf.setOnClickListener {
            generatePdfReport()
        }

        // Initialize Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun generatePdfReport() {
        val currentSession = session ?: return
        if (detections.isEmpty()) {
            Toast.makeText(this, "No detections to report", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val textPaint = Paint().apply {
            textSize = 14f
            color = Color.BLACK
        }

        // Page info: A4 size is roughly 595 x 842 points
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var yPos = 50f
        canvas.drawText("Monkey Pox Detection Report", 50f, yPos, titlePaint)
        yPos += 40f
        canvas.drawText("Session: ${currentSession.date} ${currentSession.time}", 50f, yPos, textPaint)
        yPos += 25f
        canvas.drawText("Location: ${currentSession.locationName}", 50f, yPos, textPaint)
        yPos += 25f
        canvas.drawText("Total Detections: ${currentSession.totalDetections}", 50f, yPos, textPaint)
        yPos += 25f
        canvas.drawText("Distance Covered: ${currentSession.distance}", 50f, yPos, textPaint)
        yPos += 50f

        detections.forEachIndexed { index, detection ->
            // Check if we need a new page
            if (yPos > 600) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50f
            }

            canvas.drawText("Detection #${index + 1}: ${detection.formattedDate}", 50f, yPos, textPaint)
            yPos += 20f
            canvas.drawText("Coordinates: ${detection.latitude}, ${detection.longitude}", 50f, yPos, textPaint)
            yPos += 20f

            detection.imagePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val targetWidth = 400f
                    val targetHeight = targetWidth / aspectRatio
                    
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth.toInt(), targetHeight.toInt(), true)
                    canvas.drawBitmap(scaledBitmap, 50f, yPos, paint)
                    yPos += targetHeight + 30f
                }
            }
            yPos += 20f
        }

        pdfDocument.finishPage(page)

        val fileName = "MonkeyPoxReport_${currentSession.id}.pdf"
        val file = File(getExternalFilesDir(null), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "PDF saved and opening...", Toast.LENGTH_SHORT).show()
            sharePdf(file)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun sharePdf(file: File) {
        val authority = "${applicationContext.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(this, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Report"))
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to delete this session and all its detections?")
            .setPositiveButton("Delete") { _, _ ->
                sessionId?.let {
                    DetectionStorage.deleteSession(this, it)
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDetectionUI() {
        if (detections.isEmpty()) {
            binding.imageCard.visibility = View.GONE
            binding.textViewImageCounter.text = "0 of 0"
            return
        }

        binding.imageCard.visibility = View.VISIBLE
        val detection = detections[currentDetectionIndex]
        binding.textViewImageCounter.text = "${currentDetectionIndex + 1} of ${detections.size}"
        
        detection.imagePath?.let {
            val imgFile = File(it)
            if (imgFile.exists()) {
                Glide.with(this).load(imgFile).into(binding.imageViewDetection)
            } else {
                Log.e("MapsActivity", "Image file not found: $it")
            }
        }

        binding.btnPrev.visibility = if (currentDetectionIndex > 0) View.VISIBLE else View.INVISIBLE
        binding.btnNext.visibility = if (currentDetectionIndex < detections.size - 1) View.VISIBLE else View.INVISIBLE
    }

    private fun moveMapToCurrent() {
        if (::mMap.isInitialized && detections.isNotEmpty()) {
            val pos = detections[currentDetectionIndex].latLng
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 18f))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        // Restore Zoom Controls
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true

        Log.d("MapsActivity", "Map Ready. Adding ${detections.size} markers")

        if (detections.isEmpty()) return

        val builder = LatLngBounds.Builder()
        for (detection in detections) {
            mMap.addMarker(MarkerOptions()
                .position(detection.latLng)
                .title(detection.label))
            builder.include(detection.latLng)
        }

        // Auto focus on all markers when map loads
        if (detections.isNotEmpty()) {
            val bounds = builder.build()
            val padding = 100 
            mMap.setOnMapLoadedCallback {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            }
        }
    }
}
