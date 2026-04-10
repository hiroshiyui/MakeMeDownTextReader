/*
 * MakeMeDown Text Reader
 * Copyright (C) 2026 Hui-Hong You
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.ghostsinthelab.app.makedown.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.ghostsinthelab.app.makedown.data.DocumentType
import org.ghostsinthelab.app.makedown.data.PrivateDocument
import org.ghostsinthelab.app.makedown.data.PrivateStore
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateSpaceScreen(
    onBack: () -> Unit,
    onOpen: (Screen.Reader) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { PrivateStore.get(context) }
    val docs by store.state.collectAsState()
    val scope = rememberCoroutineScope()

    var newMenuOpen by remember { mutableStateOf(false) }
    var newFileType by remember { mutableStateOf<DocumentType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private documents") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { newMenuOpen = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New private document")
                        }
                        DropdownMenu(
                            expanded = newMenuOpen,
                            onDismissRequest = { newMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Markdown") },
                                onClick = {
                                    newMenuOpen = false
                                    newFileType = DocumentType.MARKDOWN
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("New plain text") },
                                onClick = {
                                    newMenuOpen = false
                                    newFileType = DocumentType.PLAIN_TEXT
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (docs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No private documents yet.\n\n" +
                        "Tap + above to create a Markdown or plain‑text file. " +
                        "Files placed here are stored in the app's private " +
                        "directory, are not visible to other apps, and are " +
                        "excluded from cloud backups and device transfers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                items(docs, key = { it.fileName }) { doc ->
                    PrivateDocumentRow(
                        doc = doc,
                        onClick = {
                            onOpen(
                                Screen.Reader(
                                    uri = "$PRIVATE_URI_PREFIX${doc.fileName}",
                                    displayName = doc.displayName,
                                    type = doc.type,
                                )
                            )
                        },
                        onDelete = {
                            scope.launch { store.delete(doc.fileName) }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val dialogType = newFileType
    if (dialogType != null) {
        NewPrivateFileDialog(
            type = dialogType,
            onDismiss = { newFileType = null },
            onCreate = { name ->
                newFileType = null
                scope.launch {
                    val file = store.create(name, dialogType)
                    onOpen(
                        Screen.Reader(
                            uri = "$PRIVATE_URI_PREFIX${file.name}",
                            displayName = file.name,
                            type = dialogType,
                            initialEdit = true,
                        )
                    )
                }
            },
        )
    }
}

/** URI scheme used to mark a [Screen.Reader] target as belonging to the
 *  app-private store. ReaderScreen branches on this prefix when loading
 *  and saving. Public so the home-screen entry point and ReaderScreen
 *  can both reference the same constant. */
const val PRIVATE_URI_PREFIX: String = "private://"

@Composable
private fun PrivateDocumentRow(
    doc: PrivateDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            text = when (doc.type) {
                                DocumentType.MARKDOWN -> "Markdown"
                                DocumentType.PLAIN_TEXT -> "Text"
                                DocumentType.EPUB -> "EPUB"
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(),
                )
                Text(
                    text = DateFormat
                        .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(doc.lastModified)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete private document",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NewPrivateFileDialog(
    type: DocumentType,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    val suggested = when (type) {
        DocumentType.MARKDOWN -> "untitled.md"
        DocumentType.PLAIN_TEXT -> "untitled.txt"
        DocumentType.EPUB -> "untitled"
    }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "New " + when (type) {
                    DocumentType.MARKDOWN -> "Markdown"
                    DocumentType.PLAIN_TEXT -> "plain text"
                    DocumentType.EPUB -> "document"
                },
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("File name") },
                placeholder = { Text(suggested) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.ifBlank { suggested }) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
