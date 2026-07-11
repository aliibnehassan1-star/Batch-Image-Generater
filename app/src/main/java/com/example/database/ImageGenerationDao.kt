package com.example.database

import androidx.room.*
import com.example.models.ImageGeneration
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageGenerationDao {
    @Query("SELECT * FROM image_generations ORDER BY timestamp DESC")
    fun getAllGenerations(): Flow<List<ImageGeneration>>

    @Query("SELECT * FROM image_generations WHERE id = :id")
    suspend fun getGenerationById(id: Int): ImageGeneration?

    @Query("SELECT * FROM image_generations WHERE batchId = :batchId ORDER BY timestamp ASC")
    fun getGenerationsByBatch(batchId: String): Flow<List<ImageGeneration>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneration(generation: ImageGeneration): Long

    @Update
    suspend fun updateGeneration(generation: ImageGeneration)

    @Delete
    suspend fun deleteGeneration(generation: ImageGeneration)

    @Query("DELETE FROM image_generations WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM image_generations")
    suspend fun clearAll()
}
