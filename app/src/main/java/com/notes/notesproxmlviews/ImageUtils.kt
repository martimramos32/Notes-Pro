package com.notes.notesproxmlviews

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Utilitários para compressão, redimensionamento e conversão Base64 de imagens.
 * Usado para guardar imagens comprimidas diretamente no Firestore.
 */
object ImageUtils {

    private const val MAX_WIDTH = 800
    private const val MAX_HEIGHT = 800
    private const val JPEG_QUALITY = 70

    /**
     * Carrega um Bitmap a partir de um URI de forma eficiente,
     * usando inSampleSize para evitar carregar imagens enormes para memória.
     *
     * @param uri URI da imagem selecionada pelo utilizador
     * @param contentResolver ContentResolver da Activity
     * @return Bitmap redimensionado, ou null se falhar
     */
    fun decodeSampledBitmap(uri: Uri, contentResolver: ContentResolver): Bitmap? {
        return try {
            // Passo 1: obter as dimensões originais SEM carregar o bitmap para memória
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            // Passo 2: calcular o inSampleSize ideal
            options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT)
            options.inJustDecodeBounds = false

            // Passo 3: carregar o bitmap com o inSampleSize calculado
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converte um Bitmap para uma string Base64 comprimida.
     * Redimensiona a imagem se necessário e comprime para JPEG.
     *
     * @param bitmap O bitmap a converter
     * @return String Base64 da imagem comprimida
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        // Redimensionar mantendo o aspect ratio
        val resizedBitmap = resizeBitmap(bitmap, MAX_WIDTH, MAX_HEIGHT)

        // Comprimir para JPEG
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)

        // Reciclar o bitmap redimensionado se for diferente do original
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        // Converter para Base64
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Converte uma string Base64 de volta para um Bitmap.
     *
     * @param base64String A string Base64 da imagem
     * @return Bitmap da imagem, ou null se falhar
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Redimensiona um Bitmap mantendo o aspect ratio.
     * Se o bitmap já está dentro dos limites, devolve o original.
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratioWidth = maxWidth.toFloat() / width
        val ratioHeight = maxHeight.toFloat() / height
        val ratio = minOf(ratioWidth, ratioHeight)

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Calcula o inSampleSize ideal para carregar uma imagem reduzida.
     * O inSampleSize é uma potência de 2 (1, 2, 4, 8, ...).
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
