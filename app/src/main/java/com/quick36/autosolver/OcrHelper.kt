package com.quick36.autosolver

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Fallback OCR path — used only if the question text is NOT exposed through
 * the accessibility node tree (e.g. drawn on a Canvas / inside a game engine).
 * Prefer the node-tree path; it's 10–20× faster.
 *
 * The warm-up call prevents a 200-300 ms cold-start hit on the very first real question.
 */
class OcrHelper {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Pre-loads the ML Kit model. Call once in onServiceConnected(). */
    fun warmUp() {
        // Use a 64×64 blank bitmap so ML Kit initialises its model without errors.
        val dummy = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        Canvas(dummy).drawColor(Color.WHITE)
        recognizer.process(InputImage.fromBitmap(dummy, 0))
            .addOnCompleteListener { dummy.recycle() }
    }

    /**
     * Runs OCR on [bitmap] and calls [onResult] with the recognised text, or null on failure.
     * Ideally pass an already-cropped bitmap (see [cropToQuestionRegion]) for speed.
     */
    fun recognize(bitmap: Bitmap, onResult: (String?) -> Unit) {
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { visionText -> onResult(visionText.text) }
            .addOnFailureListener { onResult(null) }
    }

    /**
     * Crops [source] down to the region where the question text typically appears.
     * Adjust the fractions to match where the question sits on your device
     * (use `adb shell uiautomator dump` bounds as a reference).
     */
    fun cropToQuestionRegion(source: Bitmap): Bitmap {
        val left   = (source.width  * 0.05f).toInt()
        val top    = (source.height * 0.28f).toInt()
        val right  = (source.width  * 0.95f).toInt()
        val bottom = (source.height * 0.42f).toInt()
        val w = (right - left).coerceAtLeast(1)
        val h = (bottom - top).coerceAtLeast(1)
        return Bitmap.createBitmap(source, left, top, w, h)
    }
}
