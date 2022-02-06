package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
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
        if (isPermissionGranted()) {
            zoomToCurrentLocation()
            enableMyLocation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
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

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                zoomToCurrentLocation()
                enableMyLocation()
            }
        }
    }

    companion object {
        private val TAG = SelectLocationFragment::class.java.simpleName
    }
}
