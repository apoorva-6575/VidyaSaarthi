package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.ClassGroupEntity
import com.hackx.ruraledtech.data.local.entities.ClassGroupLearnerEntity
import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(classGroup: ClassGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(classGroups: List<ClassGroupEntity>)

    @Query("SELECT * FROM class_groups")
    fun observeAll(): Flow<List<ClassGroupEntity>>

    @Query("SELECT * FROM class_groups WHERE teacherId = :teacherId")
    fun observeForTeacher(teacherId: String): Flow<List<ClassGroupEntity>>

    @Query("SELECT * FROM class_groups WHERE classId = :classId")
    suspend fun getById(classId: String): ClassGroupEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLearnerMapping(mapping: ClassGroupLearnerEntity)
    
    @Query("SELECT l.* FROM learners l INNER JOIN class_group_learners cgl ON l.learnerId = cgl.learnerId WHERE cgl.classId = :classId")
    fun observeLearnersForClass(classId: String): Flow<List<LearnerEntity>>

    @Query("SELECT cg.* FROM class_groups cg INNER JOIN class_group_learners cgl ON cg.classId = cgl.classId WHERE cgl.learnerId = :learnerId")
    fun observeClassesForLearner(learnerId: String): Flow<List<ClassGroupEntity>>

    @Query("SELECT COUNT(*) FROM class_group_learners WHERE classId = :classId")
    fun observeLearnerCountForClass(classId: String): Flow<Int>

    @Query("SELECT * FROM class_groups WHERE classId LIKE :prefix || '%' LIMIT 1")
    suspend fun getByCodePrefix(prefix: String): ClassGroupEntity?
}
