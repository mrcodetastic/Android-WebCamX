package com.faptastic.webcamx.camerax

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.faptastic.webcamx.utils.Commons.showLog
import com.faptastic.webcamx.utils.Upload.uploadJPEG
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer


class CameraX(
    private var context: Context,
    private var owner: LifecycleOwner,
) {

    private var imageCapture: ImageCapture? = null
    private var previewUseCase: Preview? = null
    private var previewIsActive: Boolean = true

    fun getPreviewIsActive():Boolean {
        return previewIsActive
    }

    fun pauseCameraPreview()
    {
        if (!previewIsActive) return

        //val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        //val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // Must unbind the use-cases before rebinding them
        //cameraProvider.unbindAll()
        //cameraProvider.get

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        try {
           // cameraProviderFuture.get().unbind(preview)
            cameraProviderFuture.get().unbind(previewUseCase)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        previewIsActive = false
    }

    fun resumeCameraPreview()
    {
        if (previewIsActive) return

        //val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        //val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // Must unbind the use-cases before rebinding them
        //cameraProvider.unbindAll()
        //cameraProvider.get

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val camSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        try {
            // cameraProviderFuture.get().unbind(preview)
            cameraProviderFuture.get().bindToLifecycle(
                owner,
                camSelector,
                previewUseCase // only the preview usecase in this case
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }

        previewIsActive = true
    }

    fun createCameraPreviewView(): PreviewView {
        val previewView = PreviewView(context)
        return previewView
    }

    fun bindCameraPreviewView(): Unit {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val previewView = PreviewView(context)
        /*
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        */
        previewUseCase = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }


        imageCapture = ImageCapture.Builder().build()

        val camSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        try {
            cameraProviderFuture.get().bindToLifecycle(
                owner,
                camSelector,
                previewUseCase,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    // Old function that does it all
    fun startCameraPreviewView(): PreviewView {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val previewView = PreviewView(context)
        /*
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        */
        previewUseCase = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }


        imageCapture = ImageCapture.Builder().build()

        val camSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        try {
            cameraProviderFuture.get().bindToLifecycle(
                owner,
                camSelector,
                previewUseCase,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return previewView
    }

     fun capturePhoto(doUpload:Boolean) = owner.lifecycleScope.launch {

         if (doUpload) {
             showLog("Waiting for two seconds.")
             delay(2000L)
             showLog("Resuming camera preview.")
             resumeCameraPreview()
             showLog("Waiting for five seconds.")
             delay(5000L)
             showLog("Taking photo with upload.")
         }


        val imageCapture = imageCapture ?: return@launch

        imageCapture.takePicture(ContextCompat.getMainExecutor(context), object :
            ImageCapture.OnImageCapturedCallback() {

            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)

              //  val rotation = image.imageInfo.rotationDegrees
              //  showLog("Image rotation is: " + rotation)

                owner.lifecycleScope.launch {
                    saveMediaToStorage(
                        imageProxyToBitmap(image),
                        System.currentTimeMillis().toString(),
                        doUpload
                    )
                    image.close() // https://stackoverflow.com/questions/57786636/cant-take-multiple-images-using-camerax
                }
            }
/*
            // This is never called by CameraX....
            // https://stackoverflow.com/questions/70164601/android-camerax-image-capture-onimagesaved-never-runs
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                showLog("onCaptureSuccess: Uri  ${outputFileResults.savedUri}")

            }
*/
            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                showLog("onCaptureSuccess: onError")
            }
        })

         // Pause preview screen again
         if (doUpload) {
             showLog("Waiting for five seconds.")
             delay(5000L)
             showLog("Puasing camera preview.")
             pauseCameraPreview()

         }
    }


    private suspend fun imageProxyToBitmap(image: ImageProxy): Bitmap =
        withContext(owner.lifecycleScope.coroutineContext) {
            val planeProxy = image.planes[0]
            val buffer: ByteBuffer = planeProxy.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }


    private suspend fun saveMediaToStorage(bitmap: Bitmap, name: String, doUpload: Boolean) {
        withContext(IO) {
            val filename = "$name.jpg"
            var fos: OutputStream? = null
            var fileUri: String? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 and above
                context.contentResolver?.also { resolver ->

                    val contentValues = ContentValues().apply {

                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DCIM
                        )
                    }
                    val imageUri: Uri? =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    /*
                    val imageuri2: Uri? =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    showLog(imageuri2.toString())
                    */

                    fos = imageUri?.let { with(resolver) { openOutputStream(it) } }

                }
            } else {
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val image = File(imagesDir, filename).also { fos = FileOutputStream(it) }

                Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
                    mediaScanIntent.data = Uri.fromFile(image)
                    context.sendBroadcast(mediaScanIntent)
                }

             //   MediaScannerConnection.scanFile(context, arrayOf(image.toString()), null, null)
            }

            fos?.use {

                if (doUpload) {
                    var baos = ByteArrayOutputStream()

                    try {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                        //baos.toByteArray()

                        // Process Picture, determine if day or night
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 640, 480, true)

                        var redColors = 0
                        var greenColors = 0
                        var blueColors = 0
                        var pixelCount = 0

                        for (y in 0 until scaledBitmap.height) {
                            for (x in 0 until scaledBitmap.width) {
                                val c = scaledBitmap.getPixel(x, y)
                                pixelCount++
                                redColors += Color.red(c)
                                greenColors += Color.green(c)
                                blueColors += Color.blue(c)
                            }
                        }

                        // calculate average of bitmap r,g,b values

                        // calculate average of bitmap r,g,b values
                        val red = redColors / pixelCount
                        val green = greenColors / pixelCount
                        val blue = blueColors / pixelCount

                        val threshold = 50

                        if (red < threshold && green < threshold && blue < threshold) {

                            showLog("Daylight of capture was below threshold. Skipping photo save and/or upload.")

                        } else {

                            // Lets Upload
                            showLog("Uploading photo to remote web server.")

                            uploadJPEG("https://xxxxx/webcam/upload.php", baos)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Uploaded Successfully", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                    } finally {
                        baos.close()
                    }

                    // We don't actually save the file to the device...
                    it.close()


                } else { /// NORMAL photo capture

                    val success = async(IO) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                    }

                    if (success.await()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Saved Successfully", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } // end else

            }
        }
    }

}