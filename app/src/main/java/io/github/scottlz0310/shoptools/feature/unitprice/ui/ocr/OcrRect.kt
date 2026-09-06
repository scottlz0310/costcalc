package io.github.scottlz0310.shoptools.feature.unitprice.ui.ocr

data class OcrRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2

    fun regionBucket(
        imageWidth: Int,
        imageHeight: Int,
        gridRows: Int = 6,
        gridCols: Int = 6,
    ): Int {
        if (imageWidth <= 0 || imageHeight <= 0) return 0
        val col = ((centerX.toFloat() / imageWidth) * gridCols).toInt().coerceIn(0, gridCols - 1)
        val row = ((centerY.toFloat() / imageHeight) * gridRows).toInt().coerceIn(0, gridRows - 1)
        return row * gridCols + col
    }
}

data class OcrRectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun contains(
        x: Float,
        y: Float,
    ): Boolean = x in left..right && y in top..bottom

    fun width(): Float = right - left

    fun height(): Float = bottom - top

    fun centerX(): Float = (left + right) / 2f

    fun centerY(): Float = (top + bottom) / 2f
}
