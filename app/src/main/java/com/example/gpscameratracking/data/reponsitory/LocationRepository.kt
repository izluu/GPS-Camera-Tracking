package com.example.gpscameratracking.data.reponsitory

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.gpscameratracking.data.model.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.Locale

class LocationRepository(private val context: Context) {
    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationRequest: LocationRequest =
        LocationRequest.create().setInterval(1000).setFastestInterval(1000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

    interface LocationCallback {
        fun onLocationUpdate(location: Location)
        fun onLocationError(error: String)
    }

    private fun checkLocation(locationCallBack: LocationCallback): Boolean {
        android.util.Log.d("LocationRepository", "Checking location permissions...")
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        android.util.Log.d("LocationRepository", "Has location permission: $hasPermission")

        if (!hasPermission) {
            android.util.Log.d("LocationRepository", "Permission not granted")
            locationCallBack.onLocationError("Permission not granted")
            return false
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        android.util.Log.d("LocationRepository", "GPS enabled: $isGPSEnable")
        android.util.Log.d("LocationRepository", "Network enabled: $isNetworkEnable")

        if (!isGPSEnable && !isNetworkEnable) {
            android.util.Log.d("LocationRepository", "Both GPS and Network are disabled")
            locationCallBack.onLocationError("GPS not enabled or network not connected")
            return false
        }

        android.util.Log.d("LocationRepository", "Location check passed")
        return true
    }

    fun openLocationSetting() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    @SuppressLint("SimpleDateFormat", "DefaultLocale")
    fun getDetailsFromLocation(location: android.location.Location): Location {
        val geocoder = Geocoder(context)
        var addresses = ArrayList<Address>()
        try {
            addresses =
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                ) as ArrayList<Address>
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val address = if (addresses.isNotEmpty()) addresses[0] else null
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy")
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss")
        val date = java.util.Date(System.currentTimeMillis())
        val locationDetails = address?.getAddressLine(0) ?: "Unknown Location"
        val finalAddress = locationDetails.split(",").map { it.trim() }
        val result = if (finalAddress.size > 2) {
            finalAddress.subList(1, finalAddress.size - 1).joinToString(", ")
        } else {
            locationDetails
        }
        val lon = String.format(Locale.US, "%.3f", location.longitude).toDouble()
        val lat = String.format(Locale.US, "%.3f", location.latitude).toDouble()
        return Location(
            result,
            "",
            lat,
            lon,
            dateFormat.format(date),
            timeFormat.format(date)
        )
    }

    @SuppressLint("MissingPermission")
    fun getLocationDetails(locationCallBack: LocationCallback) {
        if (!checkLocation(locationCallBack)) {
            return
        }
        android.util.Log.d("LocationRepository", "Requesting location updates")
        client.requestLocationUpdates(
            locationRequest,
            object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    val location = p0.lastLocation
                    if (location != null) {
                        val locationDetails = getDetailsFromLocation(location)
                        locationCallBack.onLocationUpdate(locationDetails)
                    } else {
                        locationCallBack.onLocationError("Location is null")
                        openLocationSetting()
                    }
                }
            },
            Looper.getMainLooper()
        )
    }
}