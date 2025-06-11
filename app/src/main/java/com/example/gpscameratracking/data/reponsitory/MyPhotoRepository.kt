package com.example.gpscameratracking.data.reponsitory

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object MyPhotoRepository {
    fun getUrls(callback: (List<String>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return callback(emptyList())

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val urls = document.get("urls") as? List<String> ?: emptyList()
                    callback(urls)
                } else {
                    callback(emptyList())
                }
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun downloadImageToGallery(
        context: Context,
        imageUrl: String,
        callback: (Boolean, String) -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
        val fileName = imageUrl.substringAfterLast("/")

        storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val imageUri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    outputStream.write(bytes)
                    outputStream.flush()
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)

                callback(true, fileName)
            } else {
                callback(false, fileName)
            }

        }.addOnFailureListener {
            callback(false, fileName)
        }
    }

}