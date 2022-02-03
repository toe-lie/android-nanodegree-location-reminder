package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.LiveDataTestUtil.getValue
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel

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

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private lateinit var reminderDataSource: FakeDataSource

    @Before
    fun setUpViewModel() {
        val app: Application = ApplicationProvider.getApplicationContext()
        FirebaseApp.initializeApp(app)
        reminderDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(app, reminderDataSource)
    }

    @Test
    fun validateEnteredData_validData_returnsTrue() {
        // GIVEN - a valid reminder
        val input = ReminderDataItem("title1", "description1", "location1", 0.0, 0.0)
        // WHEN - validate data
        val result = saveReminderViewModel.validateEnteredData(input)
        // THEN - return a valid result
        assertThat(result, `is`(true))
    }

    @Test
    fun validateEnteredData_inValidData_returnsFalse() {
        // GIVEN - an invalid reminder
        val input = ReminderDataItem(null, null, null, -0.0, -0.0)
        // WHEN - validate data
        val result = saveReminderViewModel.validateEnteredData(input)
        // THEN - return an invalid result
        assertThat(result, `is`(false))
    }

    @Test
    fun saveReminder_saving_showLoading() {
        mainCoroutineRule.pauseDispatcher()

        val input = ReminderDataItem("title1", "description1", "location1", 0.0, 0.0)
        saveReminderViewModel.saveReminder(input)
        assertThat(getValue(saveReminderViewModel.showLoading), `is`(true))

        mainCoroutineRule.resumeDispatcher()
        assertThat(getValue(saveReminderViewModel.showLoading), `is`(false))
    }

    @Test
    fun saveReminder_success_showSuccessMessage() {
        val input = ReminderDataItem("title1", "description1", "location1", 0.0, 0.0)
        saveReminderViewModel.saveReminder(input)
        assertThat(getValue(saveReminderViewModel.showToast), `is`(notNullValue()))
    }

    @Test
    fun saveReminder_success_navigateBack() {
        val input = ReminderDataItem("title1", "description1", "location1", 0.0, 0.0)
        saveReminderViewModel.saveReminder(input)
        assertThat(getValue(saveReminderViewModel.navigationCommand), `is`(NavigationCommand.Back))
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