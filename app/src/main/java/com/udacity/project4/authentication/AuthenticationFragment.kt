package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.map
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.model.AuthenticationState
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.utils.FirebaseUserLiveData
import com.udacity.project4.utils.autoCleared
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthenticationFragment : BaseFragment() {

    override val _viewModel: AuthenticationViewModel by viewModel()

    private var binding by autoCleared<FragmentAuthenticationBinding>()

    override val protectedScreen: Boolean = false

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAuthenticationBinding.inflate(inflater, container, false).apply {
            loginButton.setOnClickListener {
                launchSignInFlow()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _viewModel.authenticationState.observe(viewLifecycleOwner) { authState ->
            when (authState) {
                AuthenticationState.AUTHENTICATED -> {
                    navigateToRemindersScreen()
                }
                AuthenticationState.UNAUTHENTICATED -> {
                    // no-op
                }
                else -> {
                    // no-op
                }
            }
        }
    }

    private fun launchSignInFlow() {
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.drawable.map)
            .build()
        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // no-op
        } else {
            if (response?.error != null) {
                Toast.makeText(requireContext(), R.string.error_happened, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToRemindersScreen() {
        startActivity(Intent(requireContext(), RemindersActivity::class.java))
        requireActivity().finish()
    }
}