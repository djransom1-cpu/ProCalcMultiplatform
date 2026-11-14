package com.djran.constructioncalculator

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun ImageBitmap.encodePng(): ByteArray? {
    val out = ByteArrayOutputStream()
    return if (asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)) out.toByteArray() else null
}
