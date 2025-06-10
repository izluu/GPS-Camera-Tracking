package com.example.gpscameratracking.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpscameratracking.data.reponsitory.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val userRepository: UserRepository): ViewModel() {
    private val _loginState = MutableStateFlow<Result<FirebaseUser?>?>(null)
    val loginState: MutableStateFlow<Result<FirebaseUser?>?> = _loginState
    fun login (email: String, password: String) {
        viewModelScope.launch {
            val result = userRepository.login(email, password)
            _loginState.value = result
        }

    }

}