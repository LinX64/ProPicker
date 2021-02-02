package com.linx64.propicker.util

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


object FileUtil {
    private val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    suspend fun fileFromContentUri(context: Context, contentUri: Uri): File {
        // Preparing Temp file name
        val fileExtension = getFileExtension(context, contentUri)
        val fileName = "temp_file" + if (fileExtension != null) ".$fileExtension" else ""

        // Creating Temp file
        val tempFile = File(context.cacheDir, fileName)
        tempFile.createNewFile()

        try {
            val oStream = FileOutputStream(tempFile)
            val inputStream = context.contentResolver.openInputStream(contentUri)

            inputStream?.let {
                copy(inputStream, oStream)
            }

            oStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tempFile
    }

    suspend fun getFileExtension(context: Context, uri: Uri): String? {
        val fileType: String? = context.contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)
    }

    suspend fun getFileExtension(fileName: String): String? {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
    }

    @Throws(IOException::class)
    suspend fun copy(source: InputStream, target: OutputStream) {
        val buf = ByteArray(8192)
        var length: Int
        while (source.read(buf).also { length = it } > 0) {
            target.write(buf, 0, length)
        }
    }

    // Method to save an bitmap to a file
    suspend fun bitmapToFile(context: Context, bitmap: Bitmap): File {
        val file = getImageOutputDirectory(context)

        try {
            // Compress the bitmap and save in png format
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Return the saved bitmap uri
        return file
    }

    /**
     * It creates a image file with png extension
     * */
    fun getImageOutputDirectory(context: Context): File {

        val mediaDir =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                context.getExternalFilesDirs(Environment.DIRECTORY_DCIM).firstOrNull()?.let {
                    File(it, "images").apply { mkdirs() }
                }
            } else {
                null
            }
        return if (mediaDir != null && mediaDir.exists())
            File(
                mediaDir,
                SimpleDateFormat(
                    FILENAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()) + ".png"
            )
        else File(
            context.filesDir,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".png"
        )
    }


    suspend fun delete(file: File) {
        if (file.exists()) file.delete()
    }


    /**
     * It compresses the image maintaining the ratio
     * */
    suspend fun compressImage(
        context: Context,
        uri: Uri,
        maxWidth: Float,
        maxHeight: Float
    ): File {

        val filePath: String = FileUriUtils.getRealPath(context, uri)!!
        var scaledBitmap: Bitmap? = null
        val options = BitmapFactory.Options()

        //      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
        //      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true
        var bmp = BitmapFactory.decodeFile(filePath, options)
        var actualHeight = options.outHeight
        var actualWidth = options.outWidth

        var imgRatio = actualWidth / actualHeight.toFloat()
        val maxRatio = maxWidth / maxHeight

        //      width and height values are set maintaining the aspect ratio of the image
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            when {
                imgRatio < maxRatio -> {
                    imgRatio = maxHeight / actualHeight
                    actualWidth = (imgRatio * actualWidth).toInt()
                    actualHeight = maxHeight.toInt()
                }
                imgRatio > maxRatio -> {
                    imgRatio = maxWidth / actualWidth
                    actualHeight = (imgRatio * actualHeight).toInt()
                    actualWidth = maxWidth.toInt()
                }
                else -> {
                    actualHeight = maxHeight.toInt()
                    actualWidth = maxWidth.toInt()
                }
            }
        }

        //      setting inSampleSize value allows to load a scaled down version of the original image
        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)

        //      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false

        options.inTempStorage = ByteArray(16 * 1024)
        try {
            //          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()
        val middleX = actualWidth / 2.0f
        val middleY = actualHeight / 2.0f
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
        val canvas = Canvas(scaledBitmap!!)
        canvas.setMatrix(scaleMatrix)
        canvas.drawBitmap(
            bmp,
            middleX - bmp.width / 2,
            middleY - bmp.height / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )

        //      check the rotation of the image and display it properly
        try {
            scaledBitmap = checkRotation(filePath, scaledBitmap, context)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var out: FileOutputStream? = null
        val file = getImageOutputDirectory(context)
        try {
            out = FileOutputStream(file)

            //          write the compressed bitmap at the destination specified by filename.
            scaledBitmap!!.compress(Bitmap.CompressFormat.PNG, 75, out)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return file
    }

    /*After getting image check the orientation of the image*/
    @Throws(IOException::class)
    private fun checkRotation(
        photoPath: String?,
        bitmap: Bitmap?,
        context: Context
    ): Bitmap? {
        val ei = ExifInterface(photoPath!!)
        val orientation =
            ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        val pref = context.getSharedPreferences("propicker", Context.MODE_PRIVATE)
        val frontCameraVertical = pref.getBoolean("front_camera_vertical", false)

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> return rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> return rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> return rotateImage(bitmap, 270f)
            ExifInterface.ORIENTATION_TRANSVERSE -> return if (frontCameraVertical) bitmap?.flip(
                false,
                true
            ) else bitmap?.flip(true, false)
        }
        return bitmap
    }

    /**
     * This extension fuction is used to flip the image for front end camera
     * */
    private fun Bitmap.flip(horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.preScale((if (horizontal) -1 else 1).toFloat(), (if (vertical) -1 else 1).toFloat())

        if (vertical)
            matrix.postRotate(270f)
        else matrix.postRotate(90f)

        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun rotateImage(source: Bitmap?, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source!!, 0, 0, source.width, source.height, matrix, true)
    }


    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width lower or equal to the requested height and width.
            while (height / inSampleSize > reqHeight || width / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
