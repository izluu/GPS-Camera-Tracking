package com.example.gpscameratracking.data.reponsitory

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PointF
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class PhotoRepository(private val context: Context, private val lifecycleScope: CoroutineScope) {
    private val firestore = Firebase.firestore

    @SuppressLint("UseKtx")
    fun drawMapOnCanvas(mapBitmap: Bitmap, canvas: Canvas, scale: Float, pointF: PointF) {
        val scaledBitmap = Bitmap.createScaledBitmap(
            mapBitmap,
            (mapBitmap.width * scale).toInt(),
            (mapBitmap.height * scale).toInt(),
            true
        )
        canvas.drawBitmap(scaledBitmap, pointF.x, pointF.y, null)
    }

    @SuppressLint("UseKtx")
    fun drawCardOnCanvas(view: View, canvas: Canvas, scale: Float, pointF: PointF) {
        val bitmap = Bitmap.createBitmap(
            view.width * scale.toInt(),
            view.height * scale.toInt(),
            Bitmap.Config.ARGB_8888
        )
        val bitmapCanvas = Canvas(bitmap)
        bitmapCanvas.scale(scale, scale)
        view.draw(bitmapCanvas)
        canvas.drawBitmap(bitmap, pointF.x, pointF.y, null)
        bitmap.recycle()
    }

    fun captureImage(
        context: Context,
        previewView: PreviewView,
        cv1: CardView,
        cv2: CardView,
        supportMapFragment: SupportMapFragment
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        @SuppressLint("UseKtx")
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val captureBitmap = imageProxyToBitmap(image)
                            image.close()

                            val combineBitmap = Bitmap.createBitmap(
                                captureBitmap.width,
                                captureBitmap.height,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = Canvas(combineBitmap)
                            canvas.drawBitmap(captureBitmap, 0f, 0f, null)

                            val scaleFactor = 3.5f
                            val card1 = PointF(0f, 2480f)
                            val card2 = PointF(800f, 2480f)
                            drawCardOnCanvas(cv1, canvas, scaleFactor, card1)
                            drawCardOnCanvas(cv2, canvas, scaleFactor, card2)

                            supportMapFragment.getMapAsync { googleMap ->
                                googleMap.snapshot { bitmap ->
                                    if (bitmap != null) {
                                        drawMapOnCanvas(bitmap, canvas, scaleFactor, card1)

                                        lifecycleScope.launch {
                                            val uid =
                                                Firebase.auth.currentUser?.uid ?: return@launch
                                            val result = uploadStorage(combineBitmap, uid)
                                            result.onSuccess { url ->
                                                firestore.collection("users").document(uid)
                                                    .update("urls", FieldValue.arrayUnion(url))
                                                Toast.makeText(
                                                    context,
                                                    "Tải ảnh lên thành công",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }.onFailure {
                                                Toast.makeText(
                                                    context,
                                                    "Lỗi upload ảnh: ${it.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Toast.makeText(
                                context,
                                "Lỗi chụp ảnh: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    suspend fun uploadStorage(bitmap: Bitmap, uid: String): Result<String> {
        return try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()
            val fileName = UUID.randomUUID().toString() + ".jpg"
            val storageRef = Firebase.storage.reference.child("images/$uid/$fileName")
            storageRef.putBytes(data).await()
            val downloadUrl = storageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.d("uploadStorage", "uploadStorage: ${e.message} ")
            Result.failure(e)
        }

    }
}