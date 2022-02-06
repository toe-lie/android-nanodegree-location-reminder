package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.showSnackbar
import org.koin.android.ext.android.inject
import java.util.*


private const val REQUEST_LOCATION_PERMISSION = 1

class SelectLocationFragment : BaseFragment() {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private var selectedLocation: LatLng? = null
    private var selectedPoi: PointOfInterest? = null
    private var marker: Marker? = null
    private var poiMarker: Marker? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                onPermissionGranted()
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(
                    R.string.permission_denied_explanation, R.string.settings
                ) { // Build intent that displays the App settings screen.
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri: Uri = Uri.fromParts(
                        "package",
                        BuildConfig.APPLICATION_ID, null
                    )
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate<FragmentSelectLocationBinding>(
                inflater,
                R.layout.fragment_select_location,
                container,
                false
            ).apply {
                submitButton.setOnClickListener {
                    onLocationSelected()
                }
            }

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        setupMap()

        return binding.root
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync {
            onMapReady(it)
        }
    }

    private fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        handlePermission()
        setMapLongClick(googleMap)
        setOnPoiClick(googleMap)
        setMapStyle(googleMap)
    }

    private fun onLocationSelected() {
        if (selectedLocation == null && selectedPoi == null) {
            Toast.makeText(requireContext(), R.string.select_location, Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPoi != null) {
            val latlng = selectedPoi?.latLng
            _viewModel.latitude.value = latlng?.latitude
            _viewModel.longitude.value = latlng?.longitude
            _viewModel.reminderSelectedLocationStr.value = selectedPoi?.name
        } else {
            _viewModel.latitude.value = selectedLocation?.latitude
            _viewModel.longitude.value = selectedLocation?.longitude
            selectedLocation?.let {
                _viewModel.reminderSelectedLocationStr.value = buildLocationString(it)
            }
        }

        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun handlePermission() {
        when {
            checkPermissions() -> {
                // Already have required permission.
                onPermissionGranted()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
                showRequestPermissionRationale()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestLocationPermission()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun showRequestPermissionRationale() {
        Log.i(TAG, "Displaying permission rationale to provide additional context.")
        showSnackbar(
            R.string.location_required_error, android.R.string.ok
        ) {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        map.isMyLocationEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun zoomToCurrentLocation() {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()

        val location: Location? = locationManager
            .getBestProvider(criteria, false)?.let {
                locationManager.getLastKnownLocation(
                    it
                )
            }
        if (location != null) {
            val latitude: Double = location.latitude
            val longitude: Double = location.longitude
            val latlng = LatLng(latitude, longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, MapZoomLevel.Streets.value))
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            removeOldSelectedValues()
            selectedLocation = latLng
            val snippet = buildLocationString(latLng)
            val markerOptions =
                MarkerOptions().position(latLng).title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            marker = map.addMarker(markerOptions)
        }
    }

    private fun buildLocationString(latLng: LatLng): String {
        return String.format(
            Locale.getDefault(),
            "Lat: %1$.5f, Long: %2$.5f",
            latLng.latitude,
            latLng.longitude
        )
    }

    private fun setOnPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            removeOldSelectedValues()
            selectedPoi = poi
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker?.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun removeOldSelectedValues() {
        selectedLocation = null
        selectedPoi = null
        poiMarker?.remove()
        marker?.remove()
    }

    /**
     * Zoom to current location and enable my location on permission granted.
     * This method should be called after the user has granted the location
     * permission.
     */
    private fun onPermissionGranted() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions))
            return
        }
        zoomToCurrentLocation()
        enableMyLocation()
    }

    companion object {
        private val TAG = SelectLocationFragment::class.java.simpleName
    }
}
