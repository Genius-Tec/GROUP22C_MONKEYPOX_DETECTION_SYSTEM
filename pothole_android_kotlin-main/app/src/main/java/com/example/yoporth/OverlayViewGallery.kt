package com.example.yoporth

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayViewGallery(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        results = listOf() // Properly clear the results list
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach {
            val left = it.x1
            val top = it.y1
            val right = it.x2
            val bottom = it.y2

            canvas.drawRect(left, top, right, bottom, boxPaint)

            val confidencePercentage = "%.0f%%".format(it.cnf * 100)
            val drawableText = "${it.clsName} $confidencePercentage"

            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()

            val textBoxTop = maxOf(
                top - textHeight - BOUNDING_RECT_TEXT_PADDING,
                0f
            )
            val textBoxBottom = top

            canvas.drawRect(
                left,
                textBoxTop,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                textBoxBottom,
                textBackgroundPaint
            )

            canvas.drawText(
                drawableText,
                left + BOUNDING_RECT_TEXT_PADDING,
                textBoxBottom - BOUNDING_RECT_TEXT_PADDING,
                textPaint
            )
        }
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8f
    }
}
