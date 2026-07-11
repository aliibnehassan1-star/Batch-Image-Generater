package com.example.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.models.ImageGeneration
import com.example.providers.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: MainViewModel,
    onNavigateToGenerator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.filteredGallery.collectAsState()
    val searchQuery by viewModel.gallerySearchQuery.collectAsState()
    val filterModel by viewModel.galleryFilterModel.collectAsState()
    val filterAspectRatio by viewModel.galleryFilterAspectRatio.collectAsState()
    val filterResolution by viewModel.galleryFilterResolution.collectAsState()
    val favoritesOnly by viewModel.galleryFilterFavoritesOnly.collectAsState()
    val sortBy by viewModel.gallerySortBy.collectAsState()

    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Gallery", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(
                            onClick = { showFilters = !showFilters },
                            modifier = Modifier.testTag("gallery_filter_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                                contentDescription = "Toggle Filters"
                            )
                        }
                    }
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.gallerySearchQuery.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("gallery_search_input"),
                    placeholder = { Text("Search by prompt...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.gallerySearchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Expanded filters panel
            AnimatedVisibility(
                visible = showFilters,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Sort & Filter Options", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        // Sort By
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sort By", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = sortBy == "Newest",
                                    onClick = { viewModel.gallerySortBy.value = "Newest" },
                                    label = { Text("Newest") }
                                )
                                FilterChip(
                                    selected = sortBy == "Oldest",
                                    onClick = { viewModel.gallerySortBy.value = "Oldest" },
                                    label = { Text("Oldest") }
                                )
                            }
                        }

                        // Favorites Only
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Favorites Only", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = favoritesOnly,
                                onCheckedChange = { viewModel.galleryFilterFavoritesOnly.value = it },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        // Model Filter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Model", style = MaterialTheme.typography.bodySmall)
                            FilterDropdown(
                                selected = filterModel,
                                options = listOf("All", "flash", "pro"),
                                onSelect = { viewModel.galleryFilterModel.value = it }
                            )
                        }

                        // Aspect Ratio Filter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Aspect Ratio", style = MaterialTheme.typography.bodySmall)
                            FilterDropdown(
                                selected = filterAspectRatio,
                                options = listOf("All", "1:1", "9:16", "16:9", "4:3", "3:4", "2:3", "3:2", "5:4", "4:5", "21:9"),
                                onSelect = { viewModel.galleryFilterAspectRatio.value = it }
                            )
                        }
                    }
                }
            }

            if (items.isEmpty()) {
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
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(20.dp)
                                    .size(40.dp)
                            )
                        }
                        Text(
                            text = "No images found",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Generated batch images will appear here once successfully completed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToGenerator,
                            modifier = Modifier.testTag("gallery_go_generate_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Go Generate")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(
                                    when (item.aspectRatio) {
                                        "16:9" -> 1.77f
                                        "9:16" -> 0.56f
                                        "4:3" -> 1.33f
                                        "3:4" -> 0.75f
                                        "21:9" -> 2.33f
                                        else -> 1.0f
                                    }
                                )
                                .combinedClickable(
                                    onClick = { viewModel.activeViewerItem.value = item },
                                    onLongClick = { viewModel.toggleFavoriteImage(item) }
                                )
                                .testTag("gallery_item_${item.id}"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(item.localImagePath?.let { File(it) })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.prompt,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Favorite Heart icon badge
                                if (item.isFavorite) {
                                    IconButton(
                                        onClick = { viewModel.toggleFavoriteImage(item) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "Favorite",
                                            tint = Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Quick overlay showing prompt
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = item.prompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers
private fun Modifier.scale(scale: Float) = this.then(
    Modifier // Layout scaling helper placeholder if needed
)

@Composable
fun FilterDropdown(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(selected, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, size = 16.dp, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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

@Composable
private fun Icon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp,
    tint: androidx.compose.ui.graphics.Color
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}
