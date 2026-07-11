package com.example.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.providers.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val currentTheme by viewModel.themeMode.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val defaultModel by viewModel.defaultModel.collectAsState()
    val defaultAspectRatio by viewModel.defaultAspectRatio.collectAsState()
    val defaultResolution by viewModel.defaultResolution.collectAsState()
    val defaultImageCount by viewModel.defaultImageCount.collectAsState()
    val autoSaveToGallery by viewModel.autoSaveToGallery.collectAsState()

    var showApiKeyVisible by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    // Expanding policy sections
    var showAbout by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme Configuration
            Text("Interface & Display", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionButton(
                            text = "System",
                            selected = currentTheme == "system",
                            onClick = { viewModel.setThemeMode("system") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            text = "Light",
                            selected = currentTheme == "light",
                            onClick = { viewModel.setThemeMode("light") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            text = "Dark",
                            selected = currentTheme == "dark",
                            onClick = { viewModel.setThemeMode("dark") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Secure API Key Section
            Text("API Credentials", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Gemini API Key", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Every request routes through your secure custom key. If left empty, the app will try to fallback to the system API key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customApiKey,
                        onValueChange = { viewModel.setApiKey(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        placeholder = { Text("Paste your AIStudio API Key") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showApiKeyVisible = !showApiKeyVisible }) {
                                Icon(
                                    imageVector = if (showApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Key Visibility"
                                )
                            }
                        },
                        visualTransformation = if (showApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Generation Defaults
            Text("Generation Defaults", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Save Images", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = autoSaveToGallery,
                            onCheckedChange = { viewModel.setAutoSaveToGallery(it) }
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    DropdownSelectorSetting(
                        label = "Default Model",
                        selected = defaultModel,
                        options = listOf("gemini-3.1-flash-image-preview", "gemini-3-pro-image-preview", "gemini-2.5-flash-image"),
                        onSelect = { viewModel.setDefaultModel(it) }
                    )

                    DropdownSelectorSetting(
                        label = "Default Aspect Ratio",
                        selected = defaultAspectRatio,
                        options = listOf("1:1", "9:16", "16:9", "4:3", "3:4", "2:3", "3:2", "5:4", "4:5", "21:9"),
                        onSelect = { viewModel.setDefaultAspectRatio(it) }
                    )

                    DropdownSelectorSetting(
                        label = "Default Copies Count",
                        selected = "$defaultImageCount copies",
                        options = listOf("1 copies", "2 copies", "4 copies", "8 copies"),
                        onSelect = { viewModel.setDefaultImageCount(it.substringBefore(" ").toInt()) }
                    )
                }
            }

            // Storage Operations
            Text("Data & Backup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Export
                    Button(
                        onClick = {
                            scope.launch {
                                val jsonStr = viewModel.exportHistory()
                                clipboardManager.setText(AnnotatedString(jsonStr))
                                Toast.makeText(context, "History JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_history_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export History (Clipboard JSON)")
                    }

                    // Import
                    Button(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_history_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import History (Clipboard JSON)")
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Clear Cache
                    Button(
                        onClick = {
                            viewModel.clearAllHistory()
                            Toast.makeText(context, "All history and image cache cleared!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_cache_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Database & Cache")
                    }
                }
            }

            // Legal & About
            Text("Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            InfoExpandableSection(
                title = "About AI Batch Image Generator",
                expanded = showAbout,
                onClick = { showAbout = !showAbout },
                content = "AI Batch Image Generator is a premium, edge-to-edge Material 3 Android client designed to execute large prompt batch configurations asynchronously. Powered by Google's state-of-the-art Generative models (including Imagen 3), it delivers lightning-fast processing, local SQLite database tracking, secure credentials handling, and granular configuration controls."
            )

            InfoExpandableSection(
                title = "Privacy Policy",
                expanded = showPrivacy,
                onClick = { showPrivacy = !showPrivacy },
                content = "Your privacy is critical to us. The AI Batch Image Generator operates completely client-side. Your customized API keys are stored securely using your device's private SharedPreferences space, and never transmitted to any secondary servers. Prompts, dates, and image generation records are kept entirely offline inside your local SQLite sandbox. The application only contacts Google API endpoints directly to process your image generation requests."
            )

            InfoExpandableSection(
                title = "Terms of Service",
                expanded = showTerms,
                onClick = { showTerms = !showTerms },
                content = "By utilizing this application, you acknowledge that all generated content is governed by Google's Generative AI Additional Terms of Service. You are solely responsible for ensuring that your prompt inputs and resulting images comply with applicable safety, ethical, and legal frameworks."
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Import Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import History") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Paste the exported JSON string below to load past entries:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = { Text("[{\"prompt\":...}]") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importJsonText.isNotBlank()) {
                                scope.launch {
                                    val success = viewModel.importHistory(importJsonText)
                                    if (success) {
                                        Toast.makeText(context, "History imported successfully!", Toast.LENGTH_SHORT).show()
                                        showImportDialog = false
                                        importJsonText = ""
                                    } else {
                                        Toast.makeText(context, "Failed to parse JSON. Please verify syntax.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ThemeOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.height(40.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DropdownSelectorSetting(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Box {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(selected, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
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
}

@Composable
fun InfoExpandableSection(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}
