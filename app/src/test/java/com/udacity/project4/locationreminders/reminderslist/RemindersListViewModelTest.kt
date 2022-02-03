package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.LiveDataTestUtil.getValue
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var remindersListViewModel: RemindersListViewModel

    private lateinit var reminderDataSource: FakeDataSource

    @Before
    fun setUpViewModel() {
        val app: Application = ApplicationProvider.getApplicationContext()
        FirebaseApp.initializeApp(app)
        reminderDataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(app, reminderDataSource)
    }

    @Test
    fun loadReminders_loading_showHideLoading() {
        mainCoroutineRule.pauseDispatcher()

        remindersListViewModel.loadReminders()

        assertThat(getValue(remindersListViewModel.showLoading), `is`(true))

        mainCoroutineRule.resumeDispatcher()

        assertThat(getValue(remindersListViewModel.showLoading), `is`(false))
    }

    @Test
    fun loadReminders_error_showError() {
        reminderDataSource.setReturnError(true)
        remindersListViewModel.loadReminders()

        assertThat(getValue(remindersListViewModel.showNoData), `is`(true))
        assertThat(getValue(remindersListViewModel.showSnackBar), `is`(notNullValue()))
    }


    @Before
    fun initKoin() {
        stopKoin() // to remove 'A Koin Application has already been started'
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
        }
    }

    @After
    fun tearDownKoin() {
        stopKoin()
    }
}