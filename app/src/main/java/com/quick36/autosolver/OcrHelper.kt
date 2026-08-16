package com.quick36.autosolver

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Fallback OCR path — only needed if the question text is NOT exposed
 * through the accessibility node tree (e.g. drawn directly on a Canvas
 * or inside a game engine view). Prefer the node-tree path whenever possible;
 * it's roughly 10-20x faster than this.
 *
 * Call [warmUp] once when the service starts, so the model is already
 * loaded by the time the first real question appears (avoids a 200-300ms
 * cold-start hit on the very first recognition call).
 */
class OcrHelper {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun warmUp() {
        val dummy = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        recognizer.process(InputImage.fromBitmap(dummy, 0))
            .addOnCompleteListener { dummy.recycle() }
    }

    /**
     * Runs OCR on [bitmap] (ideally already cropped to just the question region
     * for speed) and calls [onResult] with the raw recognized text, or null on failure.
     */
    fun recognize(bitmap: Bitmap, onResult: (String?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText -> onResult(visionText.text) }
            .addOnFailureListener { onResult(null) }
    }

    /**
     * Crops [source] down to just the region where the question text appears,
     * so OCR has far less to scan. Adjust the fractions below to match where
     * the question sits on your device (use uiautomator dump bounds as reference).
     */
    fun cropToQuestionRegion(source: Bitmap): Bitmap {
        val left = (source.width * 0.05f).toInt()
        val top = (source.height * 0.28f).toInt()
        val right = (source.width * 0.95f).toInt()
        val bottom = (source.height * 0.38f).toInt()
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        return Bitmap.createBitmap(source, left, top, width, height)
    }
}
