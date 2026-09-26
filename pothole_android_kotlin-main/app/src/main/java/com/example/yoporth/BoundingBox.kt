// In BoundingBox.kt

package com.example.yoporth

import android.graphics.Matrix
import android.graphics.RectF

data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val cnf: Float,
    val cls: Int,
    val clsName: String
) {
    /**
     * Applies a transformation matrix to the bounding box coordinates.
     * This is essential for converting model output (normalized 0-1)
     * to the correct pixel coordinates on the screen.
     */
    fun scaleAndMove(matrix: Matrix): BoundingBox {
        // Create a RectF from the current normalized coordinates
        val originalRect = RectF(x1, y1, x2, y2)
        val transformedRect = RectF()

        // Apply the transformation matrix
        matrix.mapRect(transformedRect, originalRect)

        // Return a new BoundingBox with the scaled coordinates
        return BoundingBox(
            x1 = transformedRect.left,
            y1 = transformedRect.top,
            x2 = transformedRect.right,
            y2 = transformedRect.bottom,
            cx = transformedRect.centerX(),
            cy = transformedRect.centerY(),
            w = transformedRect.width(),
            h = transformedRect.height(),
            cnf = this.cnf,
            cls = this.cls,
            clsName = this.clsName
        )
    }
}