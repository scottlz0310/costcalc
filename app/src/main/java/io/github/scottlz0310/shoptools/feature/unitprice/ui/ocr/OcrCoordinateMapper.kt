package io.github.scottlz0310.shoptools.feature.unitprice.ui.ocr

import android.graphics.RectF
import androidx.camera.core.ImageProxy
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.camera.view.transform.OutputTransform

/**
 * CameraX の座標変換を担うクラス。
 * Android / CameraX 依存はここに集約し、他のクラスに漏らさない。
 */
object OcrCoordinateMapper {
    // setUsingRotationDegrees(true) で ImageProxy の rotation を変換に反映する。
    // デフォルト false のままだと縦持ち端末（rotationDegrees=90/270）で
    // ML Kit が返す bounding box が未回転バッファ座標として扱われ、AR チップがずれる。
    private val transformFactory =
        ImageProxyTransformFactory().apply {
            setUsingRotationDegrees(true)
        }

    /**
     * ImageProxy がオープンなアナライザースレッドで呼び出すこと。
     * ML Kit の非同期処理に入る前に取得しておく必要がある。
     */
    fun getInputTransform(imageProxy: ImageProxy): OutputTransform = transformFactory.getOutputTransform(imageProxy)

    /**
     * [inputTransform]: アナライザースレッドで事前取得した ImageProxy の変換。
     * [viewTransform]: PreviewView の OutputTransform（メインスレッドで取得、null 可）。
     *
     * [viewTransform] が null の場合は boundingBoxView を null のまま返す。
     * null の候補は Canvas 描画時にスキップされ、次フレームで再試行される。
     */
    fun mapCandidates(
        candidates: List<OcrCandidate>,
        inputTransform: OutputTransform,
        viewTransform: OutputTransform?,
    ): List<OcrCandidate> {
        viewTransform ?: return candidates
        val coordinateTransform = CoordinateTransform(inputTransform, viewTransform)
        return candidates.map { candidate ->
            val viewRect =
                candidate.boundingBox?.let { rect ->
                    RectF(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat())
                        .also { coordinateTransform.mapRect(it) }
                        .let { OcrRectF(it.left, it.top, it.right, it.bottom) }
                }
            candidate.copy(boundingBoxView = viewRect)
        }
    }
}
