package com.quick36.autosolver

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrHelper {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun warmUp() {
        val dummy = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        recognizer.process(InputImage.fromBitmap(dummy, 0))
            .addOnCompleteListener { dummy.recycle() }
    }

    fun recognize(bitmap: Bitmap, onResult: (String?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText -> onResult(visionText.text) }
            .addOnFailureListener { onResult(null) }
    }

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
