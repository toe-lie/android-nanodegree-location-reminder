package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.geofence.errorMessage
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.showSnackbar
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment(), OnCompleteListener<Void> {

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding


    /**
     * Provides access to the Geofencing API.
     */
    private lateinit var mGeofencingClient: GeofencingClient

    /**
     * Used when requesting to add or remove geofences.
     */
    private lateinit var mGeofencePendingIntent: PendingIntent

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                addGeofence()
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGeofencing()
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            _viewModel.generateReminderId()
            val reminderDataItem = _viewModel.mapInputToDataItem()
            val isInputValid = _viewModel.validateEnteredData(reminderDataItem)
            if (isInputValid) {
                startAddingGeofence()
            }
        }
    }

    private fun setupGeofencing() {
        mGeofencingClient = LocationServices.getGeofencingClient(requireActivity())
    }

    private fun saveReminder() {
        val reminderDataItem = _viewModel.mapInputToDataItem()
        _viewModel.validateAndSaveReminder(reminderDataItem)
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private fun startAddingGeofence() {
        when {
            checkPermissions() -> {
                // Already have required permission.
                addGeofence()
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

    /**
     * Adds geofence. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions))
            return
        }
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
            .addOnCompleteListener(this)
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private fun getGeofencingRequest(): GeofencingRequest {
        val builder = GeofencingRequest.Builder()

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

        // Add the geofence to be monitored by geofencing service.
        builder.addGeofence(buildGeofence())

        // Return a GeofencingRequest.
        return builder.build()
    }


    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private fun getGeofencePendingIntent(): PendingIntent {
        // Reuse the PendingIntent if we already have it.
        if (this::mGeofencePendingIntent.isInitialized) {
            return mGeofencePendingIntent
        }
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofence.
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        mGeofencePendingIntent =
            PendingIntent.getBroadcast(requireContext(), 0, intent, flags)
        return mGeofencePendingIntent
    }

    private fun buildGeofence(): Geofence? {
        val reminderDataItem = _viewModel.mapInputToDataItem()
        val geofence = Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId(reminderDataItem.id)

            // Set the circular region of this geofence.
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                GeofenceConstants.GEOFENCE_RADIUS_IN_METERS
            )

            // Set the expiration duration of the geofence
            .setExpirationDuration(Geofence.NEVER_EXPIRE)

            // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)

            // Create the geofence.
            .build()

        Log.d(TAG, "ItemId: ${reminderDataItem.id}; RequestId: ${geofence.requestId}")
        return geofence
    }

    //endregion: Geofencing


    /**
     * Runs when the result of calling {@link #addGeofence()}}
     * is available.
     * @param task the resulting Task, containing either a result or error.
     */
    override fun onComplete(task: Task<Void>) {
        if (task.isSuccessful) {
            Toast.makeText(requireContext(), R.string.geofences_added, Toast.LENGTH_SHORT).show()
            Log.d(
                TAG,
                "onComplete: ${_viewModel.reminderSelectedLocationStr.value}: ${_viewModel.latitude.value}, ${_viewModel.longitude.value}"
            )
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            val exception = task.exception
            val errorMessage = if (exception is ApiException) {
                errorMessage(requireContext(), exception.statusCode)
            } else {
                requireContext().resources.getString(R.string.geofence_unknown_error)
            }
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            Log.w(TAG, errorMessage);
        }
        saveReminder()
    }
}

private val TAG = "SaveReminderFragment"