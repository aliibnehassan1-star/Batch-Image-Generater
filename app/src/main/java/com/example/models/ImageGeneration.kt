package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_generations")
data class ImageGeneration(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val negativePrompt: String = "",
    val seed: Long = -1L,
    val guidanceScale: Float = 7.5f,
    val modelUsed: String = "gemini-3.1-flash-image-preview",
    val aspectRatio: String = "1:1",
    val resolution: String = "1024x1024",
    val timestamp: Long = System.currentTimeMillis(),
    val localImagePath: String? = null,
    val status: String = "PENDING", // PENDING, GENERATING, SUCCESS, FAILED
    val errorMessage: String? = null,
    val isFavorite: Boolean = false,
    val isPromptFavorite: Boolean = false,
    val stylePreset: String = "",
    val batchId: String = ""
)
