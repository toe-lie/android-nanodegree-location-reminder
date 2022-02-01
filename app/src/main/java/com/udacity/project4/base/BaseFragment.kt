package com.udacity.project4.base

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.locationreminders.data.model.AuthenticationState

/**
 * Base Fragment to observe on the common LiveData objects
 */
abstract class BaseFragment : Fragment() {
    /**
     * Every fragment has to have an instance of a view model that extends from the BaseViewModel
     */
    abstract val _viewModel: BaseViewModel

    open val protectedScreen: Boolean = true

    override fun onStart() {
        super.onStart()
        _viewModel.showErrorMessage.observe(this, Observer {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        })
        _viewModel.showToast.observe(this, Observer {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        })
        _viewModel.showSnackBar.observe(this, Observer {
            Snackbar.make(this.view!!, it, Snackbar.LENGTH_LONG).show()
        })
        _viewModel.showSnackBarInt.observe(this, Observer {
            Snackbar.make(this.view!!, getString(it), Snackbar.LENGTH_LONG).show()
        })

        _viewModel.navigationCommand.observe(this, Observer { command ->
            when (command) {
                is NavigationCommand.To -> findNavController().navigate(command.directions)
                is NavigationCommand.Back -> findNavController().popBackStack()
                is NavigationCommand.BackTo -> findNavController().popBackStack(
                    command.destinationId,
                    false
                )
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (protectedScreen) {
            observeUnathenticatedState()
        }
    }

    private fun observeUnathenticatedState() {
        _viewModel.authenticationState.observe(viewLifecycleOwner) { authState ->
            if (authState != AuthenticationState.AUTHENTICATED) {
                val intent = Intent(requireContext(), AuthenticationActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }
}