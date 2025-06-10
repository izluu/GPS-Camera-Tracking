package com.example.gpscameratracking.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val urls: List<String> = emptyList()
)