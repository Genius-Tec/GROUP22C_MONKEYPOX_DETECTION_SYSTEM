package com.example.yoporth

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    // Store the transformation matrix to scale coordinates from 0-1 to view size
    private var transformationMatrix: android.graphics.Matrix? = null

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        transformationMatrix = null // Clear the matrix
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        // Paints for the label text background and foreground
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        // Paint for the bounding box rectangle
        // NOTE: R.color.bounding_box_color must be defined in your app's resources (colors.xml)
        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach { box ->
            // RectF holds normalized coordinates (0-1) initially
            val rectF = RectF(box.x1, box.y1, box.x2, box.y2)

            if (transformationMatrix != null) {
                // Gallery Mode: The transformation matrix handles scaling and translation.
                // Apply the matrix directly to the normalized coordinates in rectF.
                // After this call, rectF holds the final pixel coordinates.
                transformationMatrix!!.mapRect(rectF)

            } else {
                // Camera Mode: No matrix is provided. Scale the normalized coordinates
                // by the view's pixel dimensions to get the final pixel coordinates.
                rectF.left *= width
                rectF.top *= height
                rectF.right *= width
                rectF.bottom *= height
            }

            val left = rectF.left
            val top = rectF.top
            val right = rectF.right
            val bottom = rectF.bottom

            // Draw the bounding box (coordinates are now in pixels for both paths)
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Prepare text: Class name + Confidence (e.g., "pothole 95%")
            val confidencePercentage = "%.0f%%".format(box.cnf * 100)
            val drawableText = "${box.clsName} $confidencePercentage"

            // Get bounds of the text to draw the background rectangle
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width().toFloat()
            val textHeight = bounds.height().toFloat()

            // Calculate text position, ensuring the text background is drawn just above the box.

            // Text box top edge should be at: top - textHeight - PADDING
            // Use maxOf to compare the calculated position with 0f to prevent going off-screen.
            val textBoxTop = maxOf(
                top - textHeight - BOUNDING_RECT_TEXT_PADDING,
                0f
            )

            // Text box bottom edge is the top edge of the bounding box
            val textBoxBottom = top

            // Draw the text background rectangle
            canvas.drawRect(
                left,
                textBoxTop,
                left + textWidth + (BOUNDING_RECT_TEXT_PADDING * 2), // Add padding on both sides
                textBoxBottom,
                textBackgroundPaint
            )

            // Draw the text (text is drawn using its baseline)
            canvas.drawText(
                drawableText,
                left + BOUNDING_RECT_TEXT_PADDING,
                textBoxBottom - BOUNDING_RECT_TEXT_PADDING, // Position text baseline inside the background box
                textPaint
            )

        }
    }

    /**
     * Sets results for the Camera (normalized 0-1) or Gallery (normalized 0-1 + Matrix).
     * @param transformationMatrix Pass null for CameraX, or the calculated matrix for Gallery.
     */
    fun setResults(boundingBoxes: List<BoundingBox>, transformationMatrix: android.graphics.Matrix?) {
        this.results = boundingBoxes
        this.transformationMatrix = transformationMatrix
        // Request a redraw
        invalidate()
    }

    // Overload for CameraX compatibility (passes null matrix)
    fun setResults(boundingBoxes: List<BoundingBox>) {
        setResults(boundingBoxes, null)
    }


    companion object {
        // Use Float for consistency in drawing calculations
        private const val BOUNDING_RECT_TEXT_PADDING = 8f
    }
}
