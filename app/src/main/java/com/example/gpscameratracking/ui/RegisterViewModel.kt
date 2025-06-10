package com.example.gpscameratracking.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpscameratracking.data.reponsitory.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(private val userRepository: UserRepository): ViewModel() {
    private val _registerState = MutableStateFlow<Result<FirebaseUser?>?>(null)
    val registerState: StateFlow<Result<FirebaseUser?>?> = _registerState
    fun register(email: String, password: String) {
        viewModelScope.launch {
            val result = userRepository.registerUser(email, password)
            _registerState.value = result
        }
    }


}