package com.example.providers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.models.ImageGeneration
import com.example.repositories.ImageGenerationRepository
import com.example.services.GenerationQueueManager
import com.example.services.QueueState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(context: Context) : ViewModel() {
    private val database = AppDatabase.getDatabase(context)
    private val repository = ImageGenerationRepository(context, database.imageGenerationDao())
    val queueManager = GenerationQueueManager(repository)

    private val sharedPrefs = context.getSharedPreferences("ai_batch_image_generator_prefs", Context.MODE_PRIVATE)

    // --- Settings State (Backed by SharedPreferences) ---
    val customApiKey = MutableStateFlow(sharedPrefs.getString("api_key", "") ?: "")
    val defaultModel = MutableStateFlow(sharedPrefs.getString("default_model", "gemini-3.1-flash-image-preview") ?: "gemini-3.1-flash-image-preview")
    val defaultAspectRatio = MutableStateFlow(sharedPrefs.getString("default_aspect_ratio", "1:1") ?: "1:1")
    val defaultResolution = MutableStateFlow(sharedPrefs.getString("default_resolution", "1024x1024") ?: "1024x1024")
    val defaultImageCount = MutableStateFlow(sharedPrefs.getInt("default_image_count", 1))
    val autoSaveToGallery = MutableStateFlow(sharedPrefs.getBoolean("auto_save", true))
    val themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "system") ?: "system") // system, light, dark

    // --- Generation Inputs State ---
    val promptsInput = MutableStateFlow("")
    val negativePromptInput = MutableStateFlow("")
    val selectedModel = MutableStateFlow(defaultModel.value)
    val selectedAspectRatio = MutableStateFlow(defaultAspectRatio.value)
    val selectedResolution = MutableStateFlow(defaultResolution.value)
    val selectedImageCount = MutableStateFlow(defaultImageCount.value)
    val guidanceScaleInput = MutableStateFlow(7.5f)
    val seedInput = MutableStateFlow(-1L) // -1 means random seed
    val isRandomSeed = MutableStateFlow(true)

    // --- History & Queue Flow ---
    val allGenerations: StateFlow<List<ImageGeneration>> = repository.allItemsFlow()
    val queueState: StateFlow<QueueState> = queueManager.queueState

    // --- Gallery Filter and Sort State ---
    val gallerySearchQuery = MutableStateFlow("")
    val galleryFilterModel = MutableStateFlow("All")
    val galleryFilterAspectRatio = MutableStateFlow("All")
    val galleryFilterResolution = MutableStateFlow("All")
    val galleryFilterFavoritesOnly = MutableStateFlow(false)
    val gallerySortBy = MutableStateFlow("Newest") // Newest, Oldest

    // --- Fullscreen Viewer State ---
    val activeViewerItem = MutableStateFlow<ImageGeneration?>(null)

    // --- Text Box History Stack for Prompt box (Undo/Redo) ---
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    init {
        // Keep inputs updated when default values change
        viewModelScope.launch {
            defaultModel.collect { selectedModel.value = it }
        }
        viewModelScope.launch {
            defaultAspectRatio.collect { selectedAspectRatio.value = it }
        }
        viewModelScope.launch {
            defaultResolution.collect { selectedResolution.value = it }
        }
        viewModelScope.launch {
            defaultImageCount.collect { selectedImageCount.value = it }
        }
    }

    // --- Settings Setters ---
    fun setApiKey(key: String) {
        customApiKey.value = key
        sharedPrefs.edit().putString("api_key", key).apply()
    }

    fun setDefaultModel(model: String) {
        defaultModel.value = model
        sharedPrefs.edit().putString("default_model", model).apply()
    }

    fun setDefaultAspectRatio(ratio: String) {
        defaultAspectRatio.value = ratio
        sharedPrefs.edit().putString("default_aspect_ratio", ratio).apply()
    }

    fun setDefaultResolution(res: String) {
        defaultResolution.value = res
        sharedPrefs.edit().putString("default_resolution", res).apply()
    }

    fun setDefaultImageCount(count: Int) {
        defaultImageCount.value = count
        sharedPrefs.edit().putInt("default_image_count", count).apply()
    }

    fun setAutoSaveToGallery(enabled: Boolean) {
        autoSaveToGallery.value = enabled
        sharedPrefs.edit().putBoolean("auto_save", enabled).apply()
    }

    fun setThemeMode(mode: String) {
        themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }

    // --- Undo / Redo for Prompt Box ---
    fun updatePrompts(newText: String, registerInHistory: Boolean = true) {
        if (registerInHistory && newText != promptsInput.value) {
            undoStack.add(promptsInput.value)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
        }
        promptsInput.value = newText
    }

    fun undoPrompt() {
        if (undoStack.isNotEmpty()) {
            val last = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(promptsInput.value)
            promptsInput.value = last
        }
    }

    fun redoPrompt() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(promptsInput.value)
            promptsInput.value = next
        }
    }

    // --- Database Operations ---
    fun toggleFavoriteImage(item: ImageGeneration) {
        viewModelScope.launch {
            val updated = item.copy(isFavorite = !item.isFavorite)
            repository.update(updated)
            // Synchronize active viewer if open
            if (activeViewerItem.value?.id == item.id) {
                activeViewerItem.value = updated
            }
        }
    }

    fun toggleFavoritePrompt(item: ImageGeneration) {
        viewModelScope.launch {
            repository.update(item.copy(isPromptFavorite = !item.isPromptFavorite))
        }
    }

    fun deleteItem(item: ImageGeneration) {
        viewModelScope.launch {
            if (activeViewerItem.value?.id == item.id) {
                activeViewerItem.value = null
            }
            repository.delete(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // --- Queue Interface ---
    fun startBatchGeneration() {
        viewModelScope.launch {
            val seedVal = if (isRandomSeed.value) -1L else seedInput.value
            queueManager.submitBatch(
                apiKey = customApiKey.value,
                rawPrompts = promptsInput.value,
                negativePrompt = negativePromptInput.value,
                aspectRatio = selectedAspectRatio.value,
                resolution = selectedResolution.value,
                modelUsed = selectedModel.value,
                imageCount = selectedImageCount.value,
                seed = seedVal
            )
        }
    }

    fun pauseGeneration() {
        viewModelScope.launch {
            queueManager.pauseQueue()
        }
    }

    fun resumeGeneration() {
        viewModelScope.launch {
            queueManager.resumeQueue(customApiKey.value)
        }
    }

    fun cancelGeneration() {
        viewModelScope.launch {
            queueManager.cancelQueue()
        }
    }

    fun retryFailedGeneration() {
        viewModelScope.launch {
            queueManager.retryFailed(customApiKey.value)
        }
    }

    // --- History Import/Export ---
    suspend fun exportHistory(): String {
        return repository.exportHistoryJson()
    }

    suspend fun importHistory(json: String): Boolean {
        return repository.importHistoryJson(json)
    }

    // --- Filtered Gallery / History Flows ---
    val filteredGallery: StateFlow<List<ImageGeneration>> = combine(
        allGenerations,
        gallerySearchQuery,
        galleryFilterModel,
        galleryFilterAspectRatio,
        galleryFilterResolution,
        galleryFilterFavoritesOnly,
        gallerySortBy
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val list = args[0] as List<ImageGeneration>
        val search = args[1] as String
        val model = args[2] as String
        val ratio = args[3] as String
        val res = args[4] as String
        val favOnly = args[5] as Boolean
        val sortBy = args[6] as String

        var result = list.filter { it.status == "SUCCESS" && it.localImagePath != null }

        if (search.isNotBlank()) {
            result = result.filter { it.prompt.contains(search, ignoreCase = true) }
        }
        if (model != "All") {
            result = result.filter { it.modelUsed.contains(model, ignoreCase = true) }
        }
        if (ratio != "All") {
            result = result.filter { it.aspectRatio == ratio }
        }
        if (res != "All") {
            result = result.filter { it.resolution == res }
        }
        if (favOnly) {
            result = result.filter { it.isFavorite }
        }

        result = if (sortBy == "Newest") {
            result.sortedByDescending { it.timestamp }
        } else {
            result.sortedBy { it.timestamp }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Extension: Keep helper flow
    private fun ImageGenerationRepository.allItemsFlow(): StateFlow<List<ImageGeneration>> {
        return allGenerations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
