package com.example.moments

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.moments.databinding.NewMomentBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewMoment : DialogFragment() {

    private var _binding: NewMomentBinding? = null
    private val binding get() = _binding!!
    private var photoUri: Uri? = null
    private var photoFile: File? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentAddress: String? = null
    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val mainActivity = activity as MainActivity
        val inflater = mainActivity.layoutInflater
        _binding = NewMomentBinding.inflate(inflater)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity)
        Log.d("NewMoment", "Dialog created")
        val builder = AlertDialog.Builder(mainActivity)
            .setView(binding.root)
            .setMessage(resources.getString(R.string.add_new_moment))

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        getLastLocation()

        binding.btnOK.setOnClickListener {
            val title = binding.editTitle.text.toString()
            val contents = binding.editContents.text.toString()

            if (title.length > 25) {
                Toast.makeText(mainActivity, resources.getString(R.string.moment_title_to_long), Toast.LENGTH_LONG).show()
            } else if (contents.length > 400) {
                Toast.makeText(mainActivity, resources.getString(R.string.moment_content_to_long), Toast.LENGTH_LONG).show()
            } else if (title.isNotEmpty() && contents.isNotEmpty()) {
                val moment = Moment(title, contents, photoUri?.toString(), currentAddress)
                // La date sera automatiquement ajoutée grâce au paramètre par défaut dans la classe Moment
                mainActivity.createNewMoment(moment)
                Toast.makeText(mainActivity, resources.getString(R.string.moment_saved), Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(mainActivity, resources.getString(R.string.moment_empty), Toast.LENGTH_LONG).show()
            }
        }

        binding.btnTakePhoto.setOnClickListener {
            Log.d("NewMoment", "Take Photo button clicked")
            dispatchTakePictureIntent()
        }
        binding.btnOK.setOnClickListener {
            val title = binding.editTitle.text.toString()
            val contents = binding.editContents.text.toString()

            if(title.length >25){

                Toast.makeText(mainActivity, resources.getString(R.string.moment_title_to_long), Toast.LENGTH_LONG).show()

            }else if(contents.length > 400){

                Toast.makeText(mainActivity, resources.getString(R.string.moment_content_to_long), Toast.LENGTH_LONG).show()


            } else if (title.isNotEmpty() && contents.isNotEmpty()) {
                val moment = Moment(title, contents, photoUri?.toString(),currentAddress)
                mainActivity.createNewMoment(moment)
                Toast.makeText(mainActivity, resources.getString(R.string.moment_saved), Toast.LENGTH_SHORT).show()
                dismiss()
            } else Toast.makeText(mainActivity, resources.getString(R.string.moment_empty), Toast.LENGTH_LONG).show()
        }

        return builder.create()
    }
    private fun getLastLocation() {
        Log.d("NewMoment", "Getting last location")
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("NewMoment", "Location permissions not granted")
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d("NewMoment", "Location obtained: ${location.latitude}, ${location.longitude}")
                    getAddressFromLocation(location)
                } else {
                    Log.d("NewMoment", "Location is null, trying to request new location")
                    requestNewLocation()
                }
            }
            .addOnFailureListener { e ->
                Log.e("NewMoment", "Error getting location", e)
            }
    }

    private fun requestNewLocation() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d("NewMoment", "New location obtained: ${location.latitude}, ${location.longitude}")
                    getAddressFromLocation(location)
                }
            }
        }, Looper.getMainLooper())
    }
    private fun getAddressFromLocation(location: Location) {
        Log.d("NewMoment", "Getting address from location")
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                currentAddress = address.getAddressLine(0)
                Log.d("NewMoment", "Address obtained: $currentAddress")
            } else {
                Log.d("NewMoment", "No addresses found")
            }
        } catch (e: Exception) {
            Log.e("NewMoment", "Error getting address", e)
        }
    }



    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoFile = createImageFile()
            photoFile?.also {
                photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.example.moments.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                try {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                } catch (e: ActivityNotFoundException) {
                    Log.e("NewMoment", "No camera activity found", e)
                    Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
                }
            }

    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                Log.d("NewMoment", "Photo file created at: ${this.absolutePath}")
            }
        } catch (ex: IOException) {
            Log.e("NewMoment", "Error creating image file", ex)
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("NewMoment", "onActivityResult called. RequestCode: $requestCode, ResultCode: $resultCode")
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Log.d("NewMoment", "Photo captured successfully")
            lifecycleScope.launch {
                saveImageToGallery()
                binding.imagePreview.setImageURI(photoUri)
                binding.imagePreview.visibility = View.VISIBLE
            }
        } else {
            Log.d("NewMoment", "Photo capture failed or was cancelled")
        }
    }

    private suspend fun saveImageToGallery() = withContext(Dispatchers.IO) {
        photoFile?.let { file ->
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val contentResolver = requireContext().contentResolver

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MomentApp")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        Log.d("NewMoment", "Photo saved to gallery: $it")
                    }
                }
            } catch (ex: Exception) {
                Log.e("NewMoment", "Error saving image to gallery", ex)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
