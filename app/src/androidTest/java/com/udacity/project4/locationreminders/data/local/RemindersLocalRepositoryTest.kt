package com.udacity.project4.locationreminders.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest : KoinTest {

    private val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 0.0, 0.0)
    private val reminder2 = ReminderDTO("Title2", "Description2", "Location2", 1.1, -1.1)
    private val newReminder =
        ReminderDTO("Title new", "Description new", "Location new", 45.4, 26.1)

    private lateinit var repository: ReminderDataSource

    private lateinit var database: RemindersDatabase

    private lateinit var appContext: Application


    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = ApplicationProvider.getApplicationContext()
        // using an in-memory database because the information stored here disappears when the
        // process is killed
        database = Room.inMemoryDatabaseBuilder(
            appContext,
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { database.reminderDao() }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun closeDb() = database.close()

    @ExperimentalCoroutinesApi
    @Test
    fun getReminders_emptyRepositoryAndUninitializedCache() = runTest {
        assertThat(repository.getReminders(), instanceOf(Result.Success::class.java))
    }

    @Test
    fun getReminders() = runTest {
        val initialReminders = (repository.getReminders() as? Result.Success)?.data

        // Add reminders
        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)

        // Fetch data again
        val afterSaveReminders = (repository.getReminders() as? Result.Success)?.data

        // Verify reminders are saved
        assertThat(initialReminders, `is`(empty()))
        assertThat(afterSaveReminders?.size, `is`(2))
    }

    @Test
    fun getReminderById() = runTest {
        repository.saveReminder(newReminder)

        val loaded = (repository.getReminder(newReminder.id) as? Result.Success)?.data

        assertThat(loaded as ReminderDTO, `is`(notNullValue()))
        assertThat(loaded.title, CoreMatchers.`is`(newReminder.title))
        assertThat(loaded.description, CoreMatchers.`is`(newReminder.description))
        assertThat(loaded.location, CoreMatchers.`is`(newReminder.location))
        assertThat(loaded.latitude, CoreMatchers.`is`(newReminder.latitude))
        assertThat(loaded.longitude, CoreMatchers.`is`(newReminder.longitude))
    }

    @Test
    fun saveReminder_savesToLocalCache() = runTest {
        // Make sure newReminder is not in cache before save
        assertThat((repository.getReminders() as Result.Success).data, not(contains(newReminder)))

        // When a reminder is saved to the reminders repository
        repository.saveReminder(newReminder)

        // Then the newReminder is saved and the cache is updated
        val result = repository.getReminders() as? Result.Success
        assertThat(result?.data, contains(newReminder))
    }

    @Test
    fun deleteAllReminders() = runTest {
        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)
        val initialReminders = (repository.getReminders() as? Result.Success)?.data

        // Delete all reminders
        repository.deleteAllReminders()

        // Fetch data again
        val afterDeleteReminders = (repository.getReminders() as? Result.Success)?.data

        // Verify reminders are empty now
        assertThat(initialReminders, `is`(not(empty())))
        assertThat(afterDeleteReminders, `is`(empty()))
    }

}