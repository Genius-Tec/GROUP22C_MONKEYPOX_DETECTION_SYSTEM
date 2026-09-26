package com.example.yoporth

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.Executors

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String,
    private val detectorListener: DetectorListener
) {

    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun setup() {
        executor.execute {
            try {
                val model = FileUtil.loadMappedFile(context, modelPath)
                val options = Interpreter.Options()
                options.numThreads = 4
                
                interpreter = Interpreter(model, options)

                val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return@execute
                // Standard classification model input [1, height, width, 3] or [1, 3, height, width]
                // Assuming [1, height, width, 3]
                tensorHeight = inputShape[1]
                tensorWidth = inputShape[2]

                loadLabels()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadLabels() {
        try {
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    // Check if it starts with index (e.g., "0 Common Rust")
                    val parts = trimmed.split(" ", limit = 2)
                    if (parts.size > 1 && parts[0].toIntOrNull() != null) {
                        labels.add(parts[1])
                    } else {
                        labels.add(trimmed)
                    }
                }
                line = reader.readLine()
            }

            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun detectAsync(frame: Bitmap) {
        executor.execute {
            detect(frame)
        }
    }

    private fun detect(frame: Bitmap) {
        interpreter ?: return
        if (tensorWidth <= 0 || tensorHeight <= 0) return

        val inferenceTime = SystemClock.uptimeMillis()
        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, true)
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer
        
        val output = TensorBuffer.createFixedSize(intArrayOf(1, labels.size), DataType.FLOAT32)
        interpreter?.run(imageBuffer, output.buffer)

        val scores = output.floatArray
        var maxIdx = -1
        var maxConf = -1.0f
        
        for (i in scores.indices) {
            if (scores[i] > maxConf) {
                maxConf = scores[i]
                maxIdx = i
            }
        }

        val finalInferenceTime = SystemClock.uptimeMillis() - inferenceTime

        mainHandler.post {
            if (maxIdx != -1) {
                detectorListener.onClassificationResult(labels[maxIdx], maxConf, finalInferenceTime)
            } else {
                detectorListener.onEmptyDetect()
            }
        }
    }

    fun clear() {
        executor.shutdown()
        interpreter?.close()
        interpreter = null
    }

    interface DetectorListener {
        fun onEmptyDetect()
        fun onClassificationResult(label: String, confidence: Float, inferenceTime: Long)
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
    }
}
