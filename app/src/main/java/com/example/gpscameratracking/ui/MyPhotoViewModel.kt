package com.example.gpscameratracking.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gpscameratracking.data.reponsitory.MyPhotoRepository

class MyPhotoViewModel : ViewModel() {
    private val _urls = MutableLiveData<List<String>>()
    val urls: MutableLiveData<List<String>> = _urls
    private val _downloadResult = MutableLiveData<String>()
    val downloadResult: LiveData<String> get() = _downloadResult
    fun fetchUrls() {
        MyPhotoRepository.getUrls { urls ->
            _urls.postValue(urls)
        }

    }


    fun download(context: Context, url: List<String>) {
        if (url.isEmpty()) {
            _downloadResult.postValue("Vui lòng chọn ảnh để tải xuống")
            return
        }
        var successCount = 0
        var failCount = 0
        for (a in url) {
            MyPhotoRepository.downloadImageToGallery(context, a) { success, fileName ->
                if (success) {
                    successCount++
                } else {
                    failCount++
                }

                if (successCount + failCount == url.size) {
                    val result = buildString {
                        append("Tải xuống thành công $successCount ảnh \n")
                        append("Tải xuống thất bại $failCount ảnh")
                    }
                    _downloadResult.postValue(result)
                }

            }
        }

    }

}