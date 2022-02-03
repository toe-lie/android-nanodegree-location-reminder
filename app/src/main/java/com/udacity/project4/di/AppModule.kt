package com.udacity.project4.di

import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        RemindersListViewModel(
            get(),
            get() as ReminderDataSource
        )
    }
    viewModel {
        AuthenticationViewModel(
            get()
        )
    }
    single {
        //This view model is declared singleton to be used across multiple fragments
        SaveReminderViewModel(
            get(),
            get() as ReminderDataSource
        )
    }

}

val dataModule = module {
    single { RemindersLocalRepository(get()) }
    single { LocalDB.createRemindersDao(androidContext()) }
}

val appModules = viewModelModule + dataModule