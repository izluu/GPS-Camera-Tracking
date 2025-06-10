package com.example.gpscameratracking.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gpscameratracking.data.reponsitory.PhotoRepository

class TakePhotoActivityVMFactory(
    private val application: Application,
    private val photoRepository: PhotoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TakePhotoActivityViewModel(application, photoRepository) as T
    }
}

