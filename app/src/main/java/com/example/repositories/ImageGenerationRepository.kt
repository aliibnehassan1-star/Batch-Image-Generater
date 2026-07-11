package com.example.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.api.Content
import com.example.api.GenerateImageRequest
import com.example.api.GenerationConfig
import com.example.api.ImageConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.database.ImageGenerationDao
import com.example.models.ImageGeneration
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageGenerationRepository(
    private val context: Context,
    private val dao: ImageGenerationDao
) {
    val allGenerations: Flow<List<ImageGeneration>> = dao.getAllGenerations()

    fun getGenerationsByBatch(batchId: String): Flow<List<ImageGeneration>> =
        dao.getGenerationsByBatch(batchId)

    suspend fun insert(generation: ImageGeneration): Long = withContext(Dispatchers.IO) {
        dao.insertGeneration(generation)
    }

    suspend fun update(generation: ImageGeneration) = withContext(Dispatchers.IO) {
        dao.updateGeneration(generation)
    }

    suspend fun delete(generation: ImageGeneration) = withContext(Dispatchers.IO) {
        // Delete local file if it exists
        generation.localImagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        dao.deleteGeneration(generation)
    }

    suspend fun deleteById(id: Int) = withContext(Dispatchers.IO) {
        val generation = dao.getGenerationById(id)
        if (generation != null) {
            delete(generation)
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        // Clear all images from folder
        val dir = File(context.filesDir, "generations")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        dao.clearAll()
    }

    /**
     * Executes the API call to Gemini to generate the image.
     * On success, saves the base64 image data to the local disk and returns the local path.
     */
    suspend fun generateAndSaveImage(
        apiKey: String,
        prompt: String,
        negativePrompt: String,
        aspectRatio: String,
        imageSize: String,
        modelName: String,
        seed: Long
    ): String = withContext(Dispatchers.IO) {
        // 1. Build prompt content.
        // Include negative prompt in instructions or append if provided
        val fullPrompt = if (negativePrompt.isNotBlank()) {
            "$prompt. Please strictly avoid: $negativePrompt"
        } else {
            prompt
        }

        // 2. Prepare API Request
        val request = GenerateImageRequest(
            contents = listOf(Content(parts = listOf(Part(text = fullPrompt)))),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(
                    aspectRatio = aspectRatio,
                    imageSize = imageSize
                ),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        // 3. Make call
        val response = RetrofitClient.service.generateImage(modelName, apiKey, request)
        
        // 4. Parse Base64 Image
        val base64Data = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData?.data
            ?: throw Exception("No image data returned from the model.")

        // 5. Decode & Save Image locally
        val savedPath = saveBase64Image(base64Data)
            ?: throw Exception("Failed to decode and save the generated image on disk.")

        savedPath
    }

    private fun saveBase64Image(base64Data: String): String? {
        return try {
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                ?: return null

            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dir = File(context.filesDir, "generations/$dateString")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val filename = "img_${System.currentTimeMillis()}.jpg"
            val file = File(dir, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Export history as a JSON string
     */
    suspend fun exportHistoryJson(): String = withContext(Dispatchers.IO) {
        try {
            val list = dao.getAllGenerations()
            val generations = list.first()
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, ImageGeneration::class.java)
            val adapter = moshi.adapter<List<ImageGeneration>>(type)
            adapter.toJson(generations)
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }

    /**
     * Import history from a JSON string
     */
    suspend fun importHistoryJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, ImageGeneration::class.java)
            val adapter = moshi.adapter<List<ImageGeneration>>(type)
            val imported = adapter.fromJson(jsonStr) ?: return@withContext false
            imported.forEach { gen ->
                // Insert but strip out ID to let Room generate fresh IDs or retain them if conflict strategy is REPLACE
                dao.insertGeneration(gen.copy(id = 0))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
