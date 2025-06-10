package com.example.gpscameratracking.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.gpscameratracking.R
import com.example.gpscameratracking.databinding.FragmentGPSBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class GPSFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentGPSBinding
    private lateinit var mMap: GoogleMap
    private lateinit var activityViewModel: TakePhotoActivityViewModel
    private var currentMarker: com.google.android.gms.maps.model.Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activityViewModel = ViewModelProvider(requireActivity())[TakePhotoActivityViewModel::class.java]
        binding = FragmentGPSBinding.inflate(inflater, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Observe location changes
        activityViewModel.getLocationState().observe(viewLifecycleOwner) { location ->
            if (location != null) {
                updateMapLocation(location.lat, location.lon)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        // Enable My Location button if permission is granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = true
        }
    }

    private fun updateMapLocation(lat: Double, lon: Double) {
        val latLng = LatLng(lat, lon)
        
        // Remove previous marker if exists
        currentMarker?.remove()
        
        // Add new marker
        currentMarker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Current Location")
        )
        
        // Move camera to new location
        mMap.animateCamera(
            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 15f)
        )
    }

    fun getMapFragment(): SupportMapFragment {
        return childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    }
}