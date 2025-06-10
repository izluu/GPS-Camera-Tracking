package com.example.gpscameratracking.data.reponsitory

import android.util.Log
import com.example.gpscameratracking.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    private val firestore = FirebaseFirestore.getInstance()
    suspend fun registerUser(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            user?.let {
                val newUser = User(it.uid, it.email ?: "", emptyList())
                firestore.collection("users").document(it.uid).set(newUser).await()
            }
            Result.success(result.user)
        } catch (e: Exception) {
            Log.d("register repo", "registerUser: ${e.message} ")
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }


}