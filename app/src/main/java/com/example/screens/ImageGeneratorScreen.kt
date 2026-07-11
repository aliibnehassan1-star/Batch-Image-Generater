package com.example.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.providers.MainViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGeneratorScreen(
    viewModel: MainViewModel,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val promptsText by viewModel.promptsInput.collectAsState()
    val negativePromptText by viewModel.negativePromptInput.collectAsState()
    val modelSelected by viewModel.selectedModel.collectAsState()
    val aspectSelected by viewModel.selectedAspectRatio.collectAsState()
    val resolutionSelected by viewModel.selectedResolution.collectAsState()
    val imageCountSelected by viewModel.selectedImageCount.collectAsState()
    val guidanceScale by viewModel.guidanceScaleInput.collectAsState()
    val seedVal by viewModel.seedInput.collectAsState()
    val isRandomSeedVal by viewModel.isRandomSeed.collectAsState()

    val queueState by viewModel.queueState.collectAsState()

    var showAdvanced by remember { mutableStateOf(false) }

    // TXT File Import Launcher
    val txtFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().use { it.readText() }
                    viewModel.updatePrompts(content)
                    Toast.makeText(context, "Prompts imported from TXT file!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error importing file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val promptLinesCount = promptsText.split("\n").filter { it.trim().isNotEmpty() }.size
    val totalOutputImageEstimate = promptLinesCount * imageCountSelected

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Generator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (promptsText.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.updatePrompts("") },
                            modifier = Modifier.testTag("generator_clear_all_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Prompt Box")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Large Multiline Prompt Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enter Prompts (One Per Line)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$promptLinesCount Prompts",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = promptsText,
                            onValueChange = { viewModel.updatePrompts(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("prompts_input_field"),
                            placeholder = {
                                Text(
                                    "Type prompts here...\n\nA futuristic space helmet\nA cute ginger cat in water\nFantasy castle sunset",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Prompt box controls (Undo, Redo, Copy, Paste, Clear, Import)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { viewModel.undoPrompt() }) {
                                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                                }
                                IconButton(onClick = { viewModel.redoPrompt() }) {
                                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(promptsText))
                                        Toast.makeText(context, "Copied prompts!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.getText()?.text?.let { viewModel.updatePrompts(it) }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                                }
                            }

                            Button(
                                onClick = { txtFilePickerLauncher.launch("text/plain") },
                                colors = ButtonDefaults.textButtonColors(),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.testTag("import_txt_button")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import TXT", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Character Counter
                        Text(
                            text = "${promptsText.length} characters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Model Selection
                DropdownSelector(
                    label = "AI Model Selection",
                    selected = modelSelected,
                    options = listOf(
                        "gemini-3.1-flash-image-preview",
                        "gemini-3-pro-image-preview",
                        "gemini-2.5-flash-image"
                    ),
                    onSelect = { viewModel.selectedModel.value = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        DropdownSelector(
                            label = "Aspect Ratio",
                            selected = aspectSelected,
                            options = listOf("1:1", "9:16", "16:9", "4:3", "3:4", "2:3", "3:2", "5:4", "4:5", "21:9"),
                            onSelect = { viewModel.selectedAspectRatio.value = it }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        DropdownSelector(
                            label = "Resolution Limit",
                            selected = resolutionSelected,
                            options = listOf("512x512", "768x768", "1024x1024", "1024x1536", "1536x1024", "2048x2048"),
                            onSelect = { viewModel.selectedResolution.value = it }
                        )
                    }
                }

                DropdownSelector(
                    label = "Images per Prompt",
                    selected = "$imageCountSelected copies",
                    options = listOf("1 copies", "2 copies", "4 copies", "8 copies"),
                    onSelect = { viewModel.selectedImageCount.value = it.substringBefore(" ").toInt() }
                )

                // Advanced Settings Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvanced = !showAdvanced }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Advanced Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Icon(
                                imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(
                            visible = showAdvanced,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Negative Prompt
                                OutlinedTextField(
                                    value = negativePromptText,
                                    onValueChange = { viewModel.negativePromptInput.value = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Negative Prompt (Strict avoidance)") },
                                    placeholder = { Text("blurry, low quality, cartoon, extra limbs") },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Guidance Scale
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Guidance Scale: $guidanceScale", style = MaterialTheme.typography.bodySmall)
                                        Text("Controls prompt adherence", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Slider(
                                        value = guidanceScale,
                                        onValueChange = { viewModel.guidanceScaleInput.value = it },
                                        valueRange = 1.0f..15.0f,
                                        steps = 28
                                    )
                                }

                                // Seed Setup
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Random Seed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("Changes generations on retry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = isRandomSeedVal,
                                        onCheckedChange = { viewModel.isRandomSeed.value = it }
                                    )
                                }

                                if (!isRandomSeedVal) {
                                    OutlinedTextField(
                                        value = if (seedVal == -1L) "" else seedVal.toString(),
                                        onValueChange = {
                                            viewModel.seedInput.value = it.toLongOrNull() ?: -1L
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Seed value") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Generate trigger button
                Button(
                    onClick = {
                        if (promptsText.isBlank()) {
                            Toast.makeText(context, "Please enter at least one prompt!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.startBatchGeneration()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("generate_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (promptLinesCount > 0) "Generate Batch ($totalOutputImageEstimate Images)" else "Generate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(80.dp)) // bottom space for queue manager overlap
            }

            // QUEUE MANAGER PANEL OVERLAY
            AnimatedVisibility(
                visible = queueState.isProcessing,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        .testTag("queue_status_panel"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!queueState.isPaused) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PauseCircle,
                                        contentDescription = "Paused",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (queueState.isPaused) "Queue Paused" else "Generating Batch...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Text(
                                text = "Prompt ${queueState.currentIndex + 1} of ${queueState.totalCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Linear Progress
                        LinearProgressIndicator(
                            progress = queueState.progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress percentage and remaining time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${queueState.progressPercent}% Completed",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            val remMs = queueState.estimatedRemainingTimeMs
                            val remText = if (remMs > 0) {
                                val mins = TimeUnit.MILLISECONDS.toMinutes(remMs)
                                val secs = TimeUnit.MILLISECONDS.toSeconds(remMs) % 60
                                "Est. Remaining: ${mins}m ${secs}s"
                            } else {
                                "Est. Remaining: Calculating..."
                            }

                            Text(
                                text = remText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Queue controls row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (queueState.isPaused) {
                                Button(
                                    onClick = { viewModel.resumeGeneration() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.pauseGeneration() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause")
                                }
                            }

                            val failedCount = queueState.items.count { it.status == "FAILED" }
                            if (failedCount > 0) {
                                Button(
                                    onClick = { viewModel.retryFailedGeneration() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Fail ($failedCount)")
                                }
                            }

                            Button(
                                onClick = { viewModel.cancelGeneration() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = selected, style = MaterialTheme.typography.bodyMedium)
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
