package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.QuickTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickTemplateDao {
    @Query("SELECT * FROM quick_templates ORDER BY isBuiltIn DESC, createdAtMillis DESC")
    fun observeAllTemplates(): Flow<List<QuickTemplateEntity>>

    @Query("SELECT * FROM quick_templates ORDER BY isBuiltIn DESC, createdAtMillis DESC")
    suspend fun getAllTemplatesOnce(): List<QuickTemplateEntity>

    @Query("SELECT COUNT(*) FROM quick_templates")
    suspend fun countTemplates(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: QuickTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<QuickTemplateEntity>)

    @Query("DELETE FROM quick_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)
}
