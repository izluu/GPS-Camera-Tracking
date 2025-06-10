package com.example.gpscameratracking.ultils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

class CameraUtils(private val context: Context) {

    private var hasFlash: Boolean = false
    private lateinit var imageCapture: ImageCapture
    private lateinit var camera: androidx.camera.core.Camera

    init {
        checkFlashSupport()
    }

    private fun checkFlashSupport() {
        hasFlash = false
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (flashAvailable == true && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    hasFlash = true
                    break
                }
            }
            Log.d("CameraUtils", "Flash supported: $hasFlash")
        } catch (e: Exception) {
            Log.e("CameraUtils", "Failed to check flash support: ${e.message}")
            hasFlash = false
        }
    }

    fun bindCamera(
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView,
        lifeCycleOwner: LifecycleOwner,
        isFrontCamera: Boolean,
        isFlashOn: Boolean
    ) {
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(if (isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)
            .build()
        imageCapture = ImageCapture.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider.unbindAll()
        camera =
            cameraProvider.bindToLifecycle(lifeCycleOwner, cameraSelector, preview, imageCapture)
        if (hasFlash && !isFrontCamera) {
            imageCapture.flashMode =
                if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        } else {
            imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
        }

    }

    fun takePhoto(executor: Executor, onBitmapCaptureCallback: OnBitmapCapturedCallback) {
        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                val bitmap = imageProxyToBitmap(image)
                if (bitmap != null) {
                    onBitmapCaptureCallback.onBitmapCaptured(bitmap)
                }
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                onBitmapCaptureCallback.onCaptureFailed(exception)
                exception.printStackTrace()
            }
        })

    }

    fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    interface OnBitmapCapturedCallback {
        fun onBitmapCaptured(bitmap: Bitmap)
        fun onCaptureFailed(exception: Exception)
    }

}
