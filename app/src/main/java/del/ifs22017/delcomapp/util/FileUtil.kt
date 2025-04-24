package del.ifs22017.delcomapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.IOException

object FileUtil {
    private const val PROFILE_SIZE_DP = 140

    fun fromUri(context: Context, uri: Uri): File {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open input stream for URI: $uri")

            // Dapatkan MIME type untuk menentukan ekstensi
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            Log.d("FileUtil", "MIME type detected: $mimeType")
            val extension = when (mimeType) {
                "image/png" -> "png"
                "image/jpeg", "image/jpg" -> "jpg"
                else -> {
                    Log.w("FileUtil", "Unsupported MIME type: $mimeType, defaulting to jpg")
                    "jpg"
                }
            }

            // Decode inputStream menjadi Bitmap
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                throw IOException("Failed to decode bitmap from URI: $uri")
            }

            val density = context.resources.displayMetrics.density
            val sizePx = (PROFILE_SIZE_DP * density).toInt()


            val resizedBitmap = resizeBitmap(originalBitmap, sizePx, sizePx)
            originalBitmap.recycle()

            // Buat file sementara untuk menyimpan gambar yang sudah di-resize
            val file = File.createTempFile("profile_", ".$extension", context.cacheDir)
            file.outputStream().use { output ->
                when (extension) {
                    "png" -> resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    else -> resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }
            }
            resizedBitmap.recycle()

            if (!file.exists() || file.length().toInt() == 0) {
                throw IOException("Created file is empty: ${file.absolutePath}")
            }

            Log.d("FileUtil", "Created file: ${file.absolutePath}, size: ${file.length()} bytes, format: $extension")
            return file
        } catch (e: Exception) {
            Log.e("FileUtil", "Error converting URI to file: ${e.message}", e)
            throw IOException("Failed to convert URI to file: ${e.message}", e)
        }
    }

    private fun resizeBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
}