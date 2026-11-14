package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TaskEntity
import com.example.data.model.TaskProofEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY triggerAtMillis ASC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task_proofs ORDER BY capturedAtMillis DESC")
    fun observeAllProofs(): Flow<List<TaskProofEntity>>

    @Query("SELECT * FROM tasks ORDER BY triggerAtMillis ASC")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM task_proofs ORDER BY capturedAtMillis DESC")
    suspend fun getAllProofsOnce(): List<TaskProofEntity>

    @Query("SELECT * FROM tasks WHERE isEnabled = 1")
    suspend fun getAllEnabledTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM task_proofs WHERE taskId = :taskId ORDER BY capturedAtMillis DESC")
    suspend fun getProofsForTaskOnce(taskId: Long): List<TaskProofEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProof(proof: TaskProofEntity): Long
}
