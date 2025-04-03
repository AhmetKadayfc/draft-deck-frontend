package com.example.draftdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.draftdeck.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}