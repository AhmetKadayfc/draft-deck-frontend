package com.example.draftdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.draftdeck.data.local.entity.ThesisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThesisDao {
    @Query("SELECT * FROM theses ORDER BY lastUpdated DESC")
    fun getAllTheses(): Flow<List<ThesisEntity>>

    @Query("SELECT * FROM theses WHERE studentId = :studentId ORDER BY lastUpdated DESC")
    fun getThesesByStudentId(studentId: String): Flow<List<ThesisEntity>>

    @Query("SELECT * FROM theses WHERE advisorId = :advisorId ORDER BY lastUpdated DESC")
    fun getThesesByAdvisorId(advisorId: String): Flow<List<ThesisEntity>>

    @Query("SELECT * FROM theses WHERE id = :thesisId")
    fun getThesisById(thesisId: String): Flow<ThesisEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThesis(thesis: ThesisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheses(theses: List<ThesisEntity>)

    @Query("DELETE FROM theses WHERE id = :thesisId")
    suspend fun deleteThesis(thesisId: String)

    @Query("DELETE FROM theses")
    suspend fun clearTheses()
}
