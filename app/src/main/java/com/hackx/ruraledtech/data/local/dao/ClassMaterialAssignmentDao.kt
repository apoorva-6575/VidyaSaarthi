package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.ClassMaterialAssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassMaterialAssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assignment: ClassMaterialAssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assignments: List<ClassMaterialAssignmentEntity>)

    @Query("SELECT * FROM class_material_assignments WHERE classId = :classId")
    fun observeByClassId(classId: String): Flow<List<ClassMaterialAssignmentEntity>>
    
    @Query("SELECT * FROM class_material_assignments WHERE packageId = :packageId AND version = :version LIMIT 1")
    suspend fun getAssignmentByPackage(packageId: String, version: Int): ClassMaterialAssignmentEntity?
    
    @Query("SELECT * FROM class_material_assignments")
    fun observeAll(): Flow<List<ClassMaterialAssignmentEntity>>
}
