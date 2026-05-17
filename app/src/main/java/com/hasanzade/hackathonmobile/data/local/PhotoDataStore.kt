package com.hasanzade.hackathonmobile.data.local

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

class PhotoDataStore(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_photos", Context.MODE_PRIVATE)

    fun savePhoto(userId: String, bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        // Şəkili sıxışdır — keyfiyyəti 70% saxla, ölçüsü kiçilt
        val scaled = scaleBitmap(bitmap, 400)
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val encoded = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
        prefs.edit().putString("photo_$userId", encoded).apply()
    }

    fun loadPhoto(userId: String): Bitmap? {
        val encoded = prefs.getString("photo_$userId", null) ?: return null
        return try {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun hasPhoto(userId: String): Boolean =
        prefs.contains("photo_$userId")

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width  = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val ratio = maxSize.toFloat() / maxOf(width, height)
        return Bitmap.createScaledBitmap(
            bitmap,
            (width * ratio).toInt(),
            (height * ratio).toInt(),
            true
        )
    }
}