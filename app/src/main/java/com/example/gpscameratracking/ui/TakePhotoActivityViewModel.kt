package com.example.gpscameratracking.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.gpscameratracking.data.model.Location
import com.example.gpscameratracking.data.reponsitory.LocationRepository
import com.example.gpscameratracking.data.reponsitory.PhotoRepository
import com.google.android.gms.maps.SupportMapFragment

class TakePhotoActivityViewModel(
    application: Application,
    private val photoRepository: PhotoRepository
) : AndroidViewModel(application) {
    private val locationRepository = LocationRepository(application)
    private val locationDetailsMutableLiveData = MutableLiveData<Location>()
    private val photoMutableLiveData = MutableLiveData<Uri>()
    fun getLocationState(): LiveData<Location> = locationDetailsMutableLiveData

    fun getPhotoState(): LiveData<Uri> = photoMutableLiveData
    fun fetchLocation() {
        android.util.Log.d("TakePhotoViewModel", "Fetching location...")
        locationRepository.getLocationDetails(object : LocationRepository.LocationCallback {
            override fun onLocationUpdate(location: Location) {
                android.util.Log.d(
                    "TakePhotoViewModel",
                    "Location updated: ${location.lat}, ${location.lon}"
                )
                locationDetailsMutableLiveData.postValue(location)
            }

            override fun onLocationError(error: String) {
                android.util.Log.e("TakePhotoViewModel", "Location error: $error")
            }
        })
    }

    fun captureImage(
        context: Context,
        previewView: PreviewView,
        cv1: CardView,
        cv2: CardView,
        supportMapFragment: SupportMapFragment
    ) {
        photoRepository.captureImage(context, previewView, cv1, cv2, supportMapFragment)
    }
}