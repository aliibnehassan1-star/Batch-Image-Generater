package com.example.services

import android.content.Context
import com.example.BuildConfig
import com.example.models.ImageGeneration
import com.example.repositories.ImageGenerationRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class QueueState(
    val batchId: String = "",
    val items: List<ImageGeneration> = emptyList(),
    val currentIndex: Int = 0, // 0-indexed, so we show (currentIndex + 1) in UI
    val totalCount: Int = 0,
    val isProcessing: Boolean = false,
    val isPaused: Boolean = false,
    val averageDurationMs: Long = 12000L, // Default estimate of 12 seconds
    val elapsedMs: Long = 0L
) {
    val progress: Float
        get() = if (totalCount > 0) currentIndex.toFloat() / totalCount else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val estimatedRemainingTimeMs: Long
        get() = if (isProcessing && !isPaused) {
            val remaining = totalCount - currentIndex
            remaining * averageDurationMs
        } else {
            0L
        }
}

class GenerationQueueManager(
    private val repository: ImageGenerationRepository
) {
    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    private var queueJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()
    private var batchStartTime: Long = 0L
    private var completedInBatchCount = 0

    /**
     * Submits a fresh batch of prompts to the queue
     */
    suspend fun submitBatch(
        apiKey: String,
        rawPrompts: String,
        negativePrompt: String,
        aspectRatio: String,
        resolution: String,
        modelUsed: String,
        imageCount: Int,
        seed: Long
    ) = mutex.withLock {
        // Stop any currently running queue
        stopQueueInternal()

        // 1. Split prompts by line and ignore empty lines
        val prompts = rawPrompts.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (prompts.isEmpty()) return

        val batchId = UUID.randomUUID().toString()
        val queueItems = mutableListOf<ImageGeneration>()

        // 2. Generate items in the database for each prompt and imageCount copy
        for (prompt in prompts) {
            for (i in 1..imageCount) {
                val item = ImageGeneration(
                    prompt = prompt,
                    negativePrompt = negativePrompt,
                    aspectRatio = aspectRatio,
                    resolution = resolution,
                    modelUsed = modelUsed,
                    seed = if (seed == -1L) (0..Long.MAX_VALUE).random() else seed,
                    status = "PENDING",
                    batchId = batchId
                )
                val insertedId = repository.insert(item)
                queueItems.add(item.copy(id = insertedId.toInt()))
            }
        }

        // 3. Initialize state
        batchStartTime = System.currentTimeMillis()
        completedInBatchCount = 0
        _queueState.value = QueueState(
            batchId = batchId,
            items = queueItems,
            currentIndex = 0,
            totalCount = queueItems.size,
            isProcessing = true,
            isPaused = false
        )

        // 4. Start processing
        startQueueProcessing(apiKey)
    }

    /**
     * Pauses the queue after the current active item finishes
     */
    suspend fun pauseQueue() = mutex.withLock {
        if (_queueState.value.isProcessing) {
            _queueState.value = _queueState.value.copy(isPaused = true)
        }
    }

    /**
     * Resumes a paused queue
     */
    suspend fun resumeQueue(apiKey: String) = mutex.withLock {
        if (_queueState.value.isProcessing && _queueState.value.isPaused) {
            _queueState.value = _queueState.value.copy(isPaused = false)
            startQueueProcessing(apiKey)
        }
    }

    /**
     * Cancels the active queue and sets any remaining PENDING items to FAILED / CANCELLED
     */
    suspend fun cancelQueue() = mutex.withLock {
        stopQueueInternal()
        val currentItems = _queueState.value.items
        val index = _queueState.value.currentIndex

        // Update database for cancelled items
        for (i in index until currentItems.size) {
            val item = currentItems[i]
            if (item.status == "PENDING" || item.status == "GENERATING") {
                val cancelledItem = item.copy(status = "FAILED", errorMessage = "Generation cancelled by user.")
                repository.update(cancelledItem)
            }
        }

        _queueState.value = QueueState() // Clear queue state
    }

    /**
     * Retries any failed prompts in the current queue batch
     */
    suspend fun retryFailed(apiKey: String) = mutex.withLock {
        val current = _queueState.value
        if (current.batchId.isEmpty()) return

        stopQueueInternal()

        // Gather failed items and reset their state in DB
        val updatedItems = current.items.map { item ->
            if (item.status == "FAILED") {
                val pendingItem = item.copy(status = "PENDING", errorMessage = null)
                repository.update(pendingItem)
                pendingItem
            } else {
                item
            }
        }

        // Find the first PENDING index to resume from
        val firstPendingIndex = updatedItems.indexOfFirst { it.status == "PENDING" }
        if (firstPendingIndex == -1) return

        batchStartTime = System.currentTimeMillis()
        completedInBatchCount = 0
        _queueState.value = current.copy(
            items = updatedItems,
            currentIndex = firstPendingIndex,
            isProcessing = true,
            isPaused = false
        )

        startQueueProcessing(apiKey)
    }

    private fun startQueueProcessing(apiKey: String) {
        queueJob?.cancel()
        queueJob = scope.launch {
            while (isActive) {
                val state = _queueState.value
                if (state.isPaused || !state.isProcessing) break
                if (state.currentIndex >= state.totalCount) {
                    // Queue finished!
                    _queueState.value = state.copy(isProcessing = false)
                    break
                }

                val currentItem = state.items[state.currentIndex]
                
                // Update item status in DB & UI to "GENERATING"
                val generatingItem = currentItem.copy(status = "GENERATING")
                repository.update(generatingItem)
                updateStateItem(state.currentIndex, generatingItem)

                val startTime = System.currentTimeMillis()
                try {
                    // Decide API Key to use (custom or default fallback)
                    val resolvedKey = apiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
                    if (resolvedKey.isBlank() || resolvedKey == "MY_GEMINI_API_KEY") {
                        throw Exception("API Key is not configured. Please enter a valid Gemini API Key in Settings.")
                    }

                    // Perform Generation API request
                    val savedLocalPath = repository.generateAndSaveImage(
                        apiKey = resolvedKey,
                        prompt = currentItem.prompt,
                        negativePrompt = currentItem.negativePrompt,
                        aspectRatio = currentItem.aspectRatio,
                        imageSize = mapResolutionToSize(currentItem.resolution),
                        modelName = currentItem.modelUsed,
                        seed = currentItem.seed
                    )

                    // Success! Update DB and UI item
                    val successItem = generatingItem.copy(
                        status = "SUCCESS",
                        localImagePath = savedLocalPath
                    )
                    repository.update(successItem)
                    updateStateItem(state.currentIndex, successItem)

                    // Update moving average duration of generations
                    val duration = System.currentTimeMillis() - startTime
                    completedInBatchCount++
                    val newAverage = ((state.averageDurationMs * (completedInBatchCount - 1)) + duration) / completedInBatchCount
                    _queueState.value = _queueState.value.copy(averageDurationMs = newAverage)

                } catch (e: Exception) {
                    e.printStackTrace()
                    // Failed! Update DB and UI item
                    val failedItem = generatingItem.copy(
                        status = "FAILED",
                        errorMessage = e.message ?: "Unknown error occurred"
                    )
                    repository.update(failedItem)
                    updateStateItem(state.currentIndex, failedItem)
                }

                // Advance index
                _queueState.value = _queueState.value.copy(
                    currentIndex = _queueState.value.currentIndex + 1,
                    elapsedMs = System.currentTimeMillis() - batchStartTime
                )
            }
        }
    }

    private fun updateStateItem(index: Int, updatedItem: ImageGeneration) {
        val currentList = _queueState.value.items.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = updatedItem
            _queueState.value = _queueState.value.copy(items = currentList)
        }
    }

    private fun stopQueueInternal() {
        queueJob?.cancel()
        queueJob = null
    }

    private fun mapResolutionToSize(resolution: String): String {
        return when {
            resolution.contains("512") -> "512px"
            resolution.contains("1024") -> "1K"
            resolution.contains("1536") -> "1.5K"
            resolution.contains("2048") -> "2K"
            else -> "1K"
        }
    }
}
