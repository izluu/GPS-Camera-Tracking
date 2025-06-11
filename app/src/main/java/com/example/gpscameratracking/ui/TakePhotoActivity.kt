package com.example.gpscameratracking.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.gpscameratracking.R
import com.example.gpscameratracking.data.reponsitory.PhotoRepository
import com.example.gpscameratracking.databinding.ActivityTakePhotoBinding
import com.example.gpscameratracking.ultils.CameraUtils

class TakePhotoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTakePhotoBinding
    private lateinit var cameraUltils: CameraUtils
    private var isFrontCamera = false
    private var isFlashOn = false
    private lateinit var activityViewModel: TakePhotoActivityViewModel
    private lateinit var gpsFragment: GPSFragment

    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTakePhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val photoRepository = PhotoRepository(this, lifecycleScope)
        val factory = TakePhotoActivityVMFactory(application, photoRepository)
        activityViewModel = ViewModelProvider(this, factory)[TakePhotoActivityViewModel::class.java]
        checkPermissions()

        gpsFragment = GPSFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_gps, gpsFragment)
            .commit()

        cameraUltils = CameraUtils(this)
        setupObservers()
        setOnClick()
    }

    @SuppressLint("InflateParams")
    private fun setOnClick() {
        binding.btnBack.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnNote.setOnClickListener {
            val noteDialog = LayoutInflater.from(this).inflate(R.layout.dialog_note, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(noteDialog)
                .create()
            val edtInput = noteDialog.findViewById<android.widget.EditText>(R.id.edtInput)
            val btnSubmit = noteDialog.findViewById<android.widget.Button>(R.id.btnSubmit)
            btnSubmit.setOnClickListener {
                val note = edtInput.text.toString()
                binding.layoutLocationInfo.tvNote.text = note
                dialog.dismiss()
            }
            dialog.show()
        }
        binding.btnChangeCamera.setOnClickListener {
            isFrontCamera = !isFrontCamera
            if (isFrontCamera) {
                binding.btnChangeCamera.setImageResource(R.drawable.front_cam_icon)
            } else {
                binding.btnChangeCamera.setImageResource(R.drawable.camera_change_icon_main_activity)
            }
            setupCamera()
        }
        binding.flash.setOnClickListener {
            isFlashOn = !isFlashOn
            if (isFlashOn) {
                binding.flash.setImageResource(R.drawable.flash_on_icon)
            } else {
                binding.flash.setImageResource(R.drawable.flash_light_icon_main_activity)
            }
            setupCamera()
        }
        binding.btnCapture.setOnClickListener {
            cameraUltils.takePhoto(
                ContextCompat.getMainExecutor(this),
                object : CameraUtils.OnBitmapCapturedCallback {
                    override fun onBitmapCaptured(bitmap: Bitmap) {
                        activityViewModel.captureImage(
                            this@TakePhotoActivity,
                            binding.previewView,
                            binding.layoutLocationInfo.cvGps,
                            binding.layoutLocationInfo.cvLocation,
                            gpsFragment.getMapFragment()
                        )
                    }

                    override fun onCaptureFailed(exception: Exception) {
                        exception.printStackTrace()
                    }

                })
        }

    }

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val REQUEST_CODE_PERMISSIONS = 10

    private fun checkPermissions() {
        android.util.Log.d("TakePhotoActivity", "Checking permissions...")
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            android.util.Log.d("TakePhotoActivity", "All permissions granted")
            setupCamera()
            // Bắt đầu lấy vị trí GPS sau khi đã có quyền
            activityViewModel.fetchLocation()
        } else {
            android.util.Log.d(
                "TakePhotoActivity",
                "Requesting permissions: ${permissionsToRequest.joinToString()}"
            )
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                android.util.Log.d("TakePhotoActivity", "All permissions granted in result")
                setupCamera()
                // Bắt đầu lấy vị trí GPS sau khi đã có quyền
                activityViewModel.fetchLocation()
            } else {
                android.util.Log.d("TakePhotoActivity", "Some permissions were denied")
                // Kiểm tra xem quyền location có bị từ chối không
                val locationPermissionDenied =
                    permissions.zip(grantResults.toList()).any { (permission, result) ->
                        (permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                                permission == Manifest.permission.ACCESS_COARSE_LOCATION) &&
                                result != PackageManager.PERMISSION_GRANTED
                    }
                if (locationPermissionDenied) {
                    android.util.Log.d("TakePhotoActivity", "Location permission denied")
                    showLocationPermissionDialog()
                }
            }
        }
    }

    private fun showLocationPermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app needs location permission to show your current position on the map. Please grant location permission in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                // Mở settings của ứng dụng
                val intent =
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = android.net.Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                // Hiển thị thông báo và thoát ứng dụng
                android.widget.Toast.makeText(
                    this,
                    "Location permission is required. App will exit.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Kiểm tra quyền location mỗi khi activity resume
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            android.util.Log.d("TakePhotoActivity", "Location permission not granted in onResume")
            showLocationPermissionDialog()
        } else {
            android.util.Log.d("TakePhotoActivity", "Location permission granted in onResume")
            // Bắt đầu lấy vị trí GPS nếu đã có quyền
            activityViewModel.fetchLocation()
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraUltils.bindCamera(
                    cameraProvider,
                    binding.previewView,
                    this,
                    isFrontCamera,
                    isFlashOn
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupObservers() {
        activityViewModel.getLocationState().observe(this) { location ->
            location?.let {
                binding.layoutLocationInfo.tvLocationDetails.text = it.locationDetails
                binding.layoutLocationInfo.tvLat.text = it.lat.toString()
                binding.layoutLocationInfo.tvLon.text = it.lon.toString()
                binding.layoutLocationInfo.tvTime.text = it.time
                binding.layoutLocationInfo.tvDate.text = it.date
            }
        }
    }

}