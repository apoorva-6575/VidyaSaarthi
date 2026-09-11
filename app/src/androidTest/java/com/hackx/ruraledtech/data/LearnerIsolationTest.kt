package com.hackx.ruraledtech.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.hackx.ruraledtech.data.local.database.AppDatabase
import com.hackx.ruraledtech.data.local.entities.AttemptEntity
import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The mandatory acceptance test from PS section 45/57: two learners on one shared device
 * must never see each other's quiz attempts. This is the whole point of keying every table
 * off learnerId instead of the Android account or device identity.
 */
@RunWith(AndroidJUnit4::class)
class LearnerIsolationTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun learnerAttemptsAreIsolatedOnASharedDevice() = runBlocking {
        val now = System.currentTimeMillis()
        db.learnerDao().insert(LearnerEntity("L-ravi", "Ravi", 5, "hi", "default", now, now, now))
        db.learnerDao().insert(LearnerEntity("L-asha", "Asha", 5, "hi", "default", now, now, now))

        db.attemptDao().insert(
            AttemptEntity("A-1", "L-ravi", "Q1", "fraction_basics", listOf("a"), true, 1000, now, "D-1", "PENDING"),
        )

        val ravisAttempts = db.attemptDao().observeForConcept("L-ravi", "fraction_basics").first()
        val ashasAttempts = db.attemptDao().observeForConcept("L-asha", "fraction_basics").first()

        assertThat(ravisAttempts).hasSize(1)
        assertThat(ashasAttempts).isEmpty()
    }

    @Test
    fun learnersRetrievedIndependentlyAfterInsert() = runBlocking {
        val now = System.currentTimeMillis()
        db.learnerDao().insert(LearnerEntity("L-1", "Priya", 4, "mr", "default", now, now, now))

        val retrieved = db.learnerDao().getById("L-1")

        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.name).isEqualTo("Priya")
    }
}
