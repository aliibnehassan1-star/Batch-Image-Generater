package com.example.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.models.ImageGeneration
import com.example.providers.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateToGenerator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val generations by viewModel.allGenerations.collectAsState()
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History Logs", fontWeight = FontWeight.Bold) },
                actions = {
                    if (generations.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.testTag("history_clear_all_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Clear All History")
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
            if (generations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(20.dp)
                                    .size(40.dp)
                            )
                        }
                        Text(
                            text = "History is empty",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your image generation requests, settings, and task statuses will be logged here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToGenerator,
                            modifier = Modifier.testTag("history_go_generate_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create a Batch")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(generations, key = { it.id }) { item ->
                        HistoryItemCard(
                            item = item,
                            onRegenerate = {
                                // Load item settings back into the inputs
                                viewModel.updatePrompts(item.prompt)
                                viewModel.negativePromptInput.value = item.negativePrompt
                                viewModel.selectedModel.value = item.modelUsed
                                viewModel.selectedAspectRatio.value = item.aspectRatio
                                viewModel.selectedResolution.value = item.resolution
                                viewModel.guidanceScaleInput.value = item.guidanceScale
                                viewModel.isRandomSeed.value = false
                                viewModel.seedInput.value = item.seed

                                onNavigateToGenerator()
                                Toast.makeText(context, "Settings copied to Generator!", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deleteItem(item)
                                Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                            },
                            onToggleFavoritePrompt = {
                                viewModel.toggleFavoritePrompt(item)
                            }
                        )
                    }
                }
            }

            // Confirm Clear All Dialog
            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text("Clear All History?") },
                    text = { Text("This will permanently delete all task entries and saved image files from local storage. This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearAllHistory()
                                showClearConfirm = false
                                Toast.makeText(context, "All history cleared", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear All")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: ImageGeneration,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavoritePrompt: () -> Unit
) {
    val dateString = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date(item.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Date & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                StatusTag(status = item.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prompt text
            Text(
                text = item.prompt,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (item.status == "FAILED" && !item.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Row (Model, Resolution, Aspect Ratio, Seed)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetaBadge(text = item.modelUsed.substringAfter("gemini-").substringBefore("-image"))
                MetaBadge(text = "Aspect: ${item.aspectRatio}")
                MetaBadge(text = "Res: ${item.resolution}")
                MetaBadge(text = "Seed: ${item.seed}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row (Favorite prompt toggle, Delete, Copy / Regenerate)
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onToggleFavoritePrompt) {
                        Icon(
                            imageVector = if (item.isPromptFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorite Prompt",
                            tint = if (item.isPromptFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                    }
                }

                Button(
                    onClick = onRegenerate,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Regenerate", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusTag(status: String) {
    val (bg, fg, text) = when (status) {
        "SUCCESS" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Completed")
        "FAILED" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Failed")
        "GENERATING" -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "Generating")
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF616161), "Queued")
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MetaBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Basic FlowRow emulation using Row for simple metadata cards
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}
