package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.google.android.gms.tasks.Task
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        // using an in-memory database because the information stored here disappears when the
        // process is killed
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminderAndGetById() = runTest {
        // GIVEN - insert a reminder
        val reminder = ReminderDTO("title1", "desc1", "loc1", 1.1, 2.2)
        database.reminderDao().saveReminder(reminder)

        // WHEN - Get the reminder by id from the database
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // THEN - The loaded data contains the expected values
        assertThat<ReminderDTO>(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun saveReminderReplacesOnConflict() = runTest {
        // Given that a reminder is inserted
        val reminder = ReminderDTO("title1", "desc1", "loc1", 1.1, 2.2)
        database.reminderDao().saveReminder(reminder)

        // When a reminder with the same id is inserted
        val newReminder = ReminderDTO("title2", "desc2", "loc2", 3.3, 4.4, reminder.id)
        database.reminderDao().saveReminder(newReminder)

        // THEN - The loaded data contains the expected values
        val loaded = database.reminderDao().getReminderById(reminder.id)
        assertThat<ReminderDTO>(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`("title2"))
        assertThat(loaded.description, `is`("desc2"))
        assertThat(loaded.location, `is`("loc2"))
        assertThat(loaded.latitude, `is`(3.3))
        assertThat(loaded.longitude, `is`(4.4))
    }

    @Test
    fun saveReminderAndGetReminders() = runTest {
        // GIVEN - insert a reminder
        val reminder = ReminderDTO("title", "desc", "loc", 1.1, 2.2)
        database.reminderDao().saveReminder(reminder)

        // WHEN - Get reminders from the database
        val reminders = database.reminderDao().getReminders()

        // THEN - There is only 1 reminder in the database, and contains the expected values
        assertThat(reminders.size, `is`(1))
        assertThat(reminders[0].id, `is`(reminder.id))
        assertThat(reminders[0].title, `is`("title"))
        assertThat(reminders[0].description, `is`("desc"))
        assertThat(reminders[0].location, `is`("loc"))
        assertThat(reminders[0].latitude, `is`(1.1))
        assertThat(reminders[0].longitude, `is`(2.2))
    }

    @Test
    fun deleteAllRemindersAndGettingReminders() = runTest {
        // Given a reminder inserted
        database.reminderDao().saveReminder(ReminderDTO("title", "desc", "loc", 1.1, 2.2))

        // When deleting all reminder
        database.reminderDao().deleteAllReminders()

        // THEN - The list is empty
        val tasks = database.reminderDao().getReminders()
        assertThat(tasks.isEmpty(), `is`(true))
    }
}