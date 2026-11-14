package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StickyNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StickyNoteDao {
    @Query("SELECT * FROM sticky_notes ORDER BY isPinnedOverlay DESC, updatedAtMillis DESC")
    fun observeAllNotes(): Flow<List<StickyNoteEntity>>

    @Query("SELECT * FROM sticky_notes ORDER BY updatedAtMillis DESC")
    suspend fun getAllNotesOnce(): List<StickyNoteEntity>

    @Query("SELECT * FROM sticky_notes WHERE isPinnedOverlay = 1 ORDER BY updatedAtMillis DESC")
    fun observePinnedNotes(): Flow<List<StickyNoteEntity>>

    @Query("SELECT * FROM sticky_notes WHERE isPinnedOverlay = 1 ORDER BY updatedAtMillis DESC")
    suspend fun getPinnedNotesOnce(): List<StickyNoteEntity>

    @Query("SELECT * FROM sticky_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): StickyNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StickyNoteEntity): Long

    @Update
    suspend fun updateNote(note: StickyNoteEntity)

    @Query("UPDATE sticky_notes SET isPinnedOverlay = :pinned, updatedAtMillis = :updatedAt WHERE id = :id")
    suspend fun setNotePinned(id: Long, pinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sticky_notes SET isPinnedOverlay = 0")
    suspend fun unpinAllNotes()

    @Query("DELETE FROM sticky_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}
