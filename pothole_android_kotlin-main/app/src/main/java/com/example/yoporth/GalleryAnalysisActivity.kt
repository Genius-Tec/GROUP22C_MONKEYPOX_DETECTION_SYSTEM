package com.example.yoporth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class GalleryAnalysisActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var detector: Detector
    private lateinit var imageView: ImageView
    private lateinit var inferenceTimeTextView: TextView
    private lateinit var placeholderText: TextView
    private lateinit var selectImageButton: Button
    private lateinit var takePhotoButton: Button
    private lateinit var toolbar: Toolbar
    
    // AI UI components
    private lateinit var aiResultCard: View
    private lateinit var aiExplanationText: TextView
    private lateinit var aiProgressBar: ProgressBar

    private var imageBitmap: Bitmap? = null
    private var detectorInitialized = false
    private var photoUri: Uri? = null

    // DeepSeek API Configuration
    private val DEEPSEEK_API_KEY = "sk-f4f1103135f64215a288a870bdee5614"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_analysis)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Monkey Pox Analysis"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        imageView = findViewById(R.id.photo_image_view)
        inferenceTimeTextView = findViewById(R.id.inference_time_text)
        placeholderText = findViewById(R.id.placeholder_text)
        selectImageButton = findViewById(R.id.select_image_button)
        takePhotoButton = findViewById(R.id.take_photo_button)
        
        // Initialize AI UI
        aiResultCard = findViewById(R.id.ai_result_card)
        aiExplanationText = findViewById(R.id.ai_explanation_text)
        aiProgressBar = findViewById(R.id.ai_progress_bar)
        
        findViewById<View>(R.id.overlay_view).visibility = View.GONE

        try {
            detector = Detector(
                context = this,
                modelPath = Constants.MODEL_PATH,
                labelPath = Constants.LABELS_PATH,
                detectorListener = this
            )
            detector.setup()
            detectorInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "ERROR: Failed to load classification model", Toast.LENGTH_LONG).show()
            selectImageButton.isEnabled = false
            takePhotoButton.isEnabled = false
            return
        }

        selectImageButton.setOnClickListener {
            if (detectorInitialized) {
                checkGalleryPermissionsAndLaunch()
            }
        }

        takePhotoButton.setOnClickListener {
            if (detectorInitialized) {
                checkCameraPermissionsAndLaunch()
            }
        }
    }

    private fun checkGalleryPermissionsAndLaunch() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            launchGallery()
        } else {
            requestGalleryPermissionLauncher.launch(permission)
        }
    }

    private fun checkCameraPermissionsAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestGalleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) launchGallery()
            else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) launchCamera()
            else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { loadImageAndRunDetection(it) }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                photoUri?.let { uri ->
                    loadImageAndRunDetection(uri)
                }
            }
        }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun launchCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: Exception) {
            null
        }
        
        photoFile?.also {
            photoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                it
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraLauncher.launch(intent)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun loadImageAndRunDetection(imageUri: Uri) {
        if (!detectorInitialized) return

        inferenceTimeTextView.text = "Processing..."
        placeholderText.visibility = View.GONE
        aiResultCard.visibility = View.GONE

        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setImageURI(imageUri)

        lifecycleScope.launch {
            imageBitmap = loadBitmapFromUri(imageUri)
            imageBitmap?.let { detector.detectAsync(it) }
        }
    }

    private suspend fun loadBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options)
            }
            
            var scale = 1
            while (options.outWidth / scale > 1024 || options.outHeight / scale > 1024) {
                scale *= 2
            }
            
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            inferenceTimeTextView.text = "No result found."
            Toast.makeText(this, "Classification failed.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onClassificationResult(label: String, confidence: Float, inferenceTime: Long) {
        runOnUiThread {
            val confPercent = (confidence * 100).toInt()
            inferenceTimeTextView.text = "Result: $label ($confPercent%) \nInference Time: $inferenceTime ms"
            
            Toast.makeText(this, "Detected: $label", Toast.LENGTH_LONG).show()
            
            imageBitmap?.let { bitmap ->
                saveResultToReports(label, confPercent, bitmap)
                generateDeepSeekInsights(label)
            }
        }
    }

    private fun generateDeepSeekInsights(label: String) {
        aiResultCard.visibility = View.VISIBLE
        aiProgressBar.visibility = View.VISIBLE
        aiExplanationText.text = "Generating detailed analysis using DeepSeek..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prompt = "The skin lesion is predicted to have '$label' (Monkey Pox related). " +
                        "Based on this detection, please provide: " +
                        "1. A brief Explanation of what this condition is. " +
                        "2. Common Symptoms and how it spreads. " +
                        "3. Recommended steps for the individual and when to seek medical attention. " +
                        "Format the response with clear headings."

                val json = JSONObject().apply {
                    put("model", "deepseek-chat")
                    val messages = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    }
                    put("messages", messages)
                    put("stream", false)
                }

                val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("https://api.deepseek.com/chat/completions")
                    .addHeader("Authorization", "Bearer $DEEPSEEK_API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("API Error: ${response.code} ${response.message}")

                    val responseData = response.body?.string() ?: throw Exception("Empty response from API")
                    val responseJson = JSONObject(responseData)
                    val choices = responseJson.getJSONArray("choices")
                    val content = choices.getJSONObject(0).getJSONObject("message").getString("content")

                    withContext(Dispatchers.Main) {
                        aiProgressBar.visibility = View.GONE
                        aiExplanationText.text = content
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    aiProgressBar.visibility = View.GONE
                    val errorMsg = e.message ?: "Unknown error"
                    aiExplanationText.text = "Error connecting to DeepSeek AI: $errorMsg"
                }
            }
        }
    }

    private fun saveResultToReports(label: String, confidence: Int, bitmap: Bitmap) {
        val sessionId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        val imagePath = saveImageToInternalStorage(bitmap)
        
        val detection = DetectionLocation(
            latitude = 0.0,
            longitude = 0.0,
            timestamp = timestamp,
            label = "$label ($confidence%)",
            imagePath = imagePath,
            sessionId = sessionId,
            boxCount = 1
        )
        DetectionStorage.addDetection(this, detection)
        
        val now = Date(timestamp)
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        val session = DetectionSession(
            id = sessionId,
            date = dateFormat.format(now),
            time = timeFormat.format(now),
            distance = "",
            totalDetections = 0,
            locationName = "$label ($confidence%)",
            timestamp = timestamp
        )
        DetectionStorage.addSession(this, session)
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): String? {
        val filename = "monkeypox_analysis_${System.currentTimeMillis()}.jpg"
        val directory = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MonkeyPoxReports")
        if (!directory.exists()) directory.mkdirs()
        
        val file = File(directory, filename)
        return try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (detectorInitialized) detector.clear()
    }
}
