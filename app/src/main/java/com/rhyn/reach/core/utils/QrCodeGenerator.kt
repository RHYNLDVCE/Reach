package com.rhyn.reach.core.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

object QrCodeGenerator {

    // Generates a square QR Code Bitmap from a string payload
    fun generateQrCode(content: String, size: Int = 512): Bitmap? {
        if (content.isBlank()) return null

        return try {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            bitmap
        } catch (e: Exception) {
            Log.e("QrCodeGenerator", "Failed to generate QR Code", e)
            null
        }
    }
}