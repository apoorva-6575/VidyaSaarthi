package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.ConceptEntity

@Dao
interface ConceptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(concepts: List<ConceptEntity>)

    @Query("SELECT * FROM concepts WHERE conceptId = :id")
    suspend fun getById(id: String): ConceptEntity?

    @Query("SELECT * FROM concepts WHERE subject = :subject AND grade = :grade")
    suspend fun getForSubject(subject: String, grade: Int): List<ConceptEntity>
}
