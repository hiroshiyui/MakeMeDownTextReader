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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.ghostsinthelab.app.makedown.data.DocumentType
import org.ghostsinthelab.app.makedown.data.RecentFile
import org.ghostsinthelab.app.makedown.data.RecentsRepository
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpen: (Screen.Reader) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val recents = RecentsRepository.get(context)
    val items by recents.state.collectAsState()
    val scope = rememberCoroutineScope()

    // Shared handler: persist RW permission, add to recents, navigate to reader.
    fun openPickedUri(uri: Uri, forcedType: DocumentType? = null, initialEdit: Boolean = false) {
        val rwFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, rwFlags) }
            .recoverCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        val (displayName, mime) = queryNameAndMime(context, uri)
        val type = forcedType ?: DocumentType.detect(mime, displayName)
        val record = RecentFile(
            uri = uri.toString(),
            displayName = displayName,
            type = type,
            lastOpenedAt = System.currentTimeMillis(),
        )
        scope.launch { recents.add(record) }
        onOpen(
            Screen.Reader(
                uri = record.uri,
                displayName = displayName,
                type = type,
                initialEdit = initialEdit,
            )
        )
    }

    val openLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentRW(),
    ) { uri: Uri? -> if (uri != null) openPickedUri(uri) }

    // Two separate CreateDocument launchers — the contract bakes the mime
    // type in at construction, so we need one per supported new-file format.
    val createMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri: Uri? ->
        if (uri != null) openPickedUri(uri, forcedType = DocumentType.MARKDOWN, initialEdit = true)
    }
    val createPlainTextLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri: Uri? ->
        if (uri != null) openPickedUri(uri, forcedType = DocumentType.PLAIN_TEXT, initialEdit = true)
    }

    var newMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MakeMeDown") },
                actions = {
                    Box {
                        IconButton(onClick = { newMenuOpen = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "New file",
                            )
                        }
                        DropdownMenu(
                            expanded = newMenuOpen,
                            onDismissRequest = { newMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Markdown") },
                                onClick = {
                                    newMenuOpen = false
                                    createMarkdownLauncher.launch("untitled.md")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("New plain text") },
                                onClick = {
                                    newMenuOpen = false
                                    createPlainTextLauncher.launch("untitled.txt")
                                },
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    openLauncher.launch(
                        arrayOf(
                            "application/epub+zip",
                            "text/markdown",
                            "text/plain",
                            "text/*",
                            "*/*",
                        )
                    )
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Open file") },
            )
        },
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No recent files.\nTap “Open file” to read an EPUB, Markdown, or plain‑text document, or use the pencil icon above to create a new one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                start = 0.dp,
                end = 0.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.uri }) { recent ->
                RecentRow(
                    recent = recent,
                    onClick = {
                        onOpen(
                            Screen.Reader(
                                uri = recent.uri,
                                displayName = recent.displayName,
                                type = recent.type,
                            )
                        )
                    },
                    onDelete = { scope.launch { recents.remove(recent.uri) } },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RecentRow(
    recent: RecentFile,
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
                text = recent.displayName,
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
                            text = when (recent.type) {
                                DocumentType.EPUB -> "EPUB"
                                DocumentType.MARKDOWN -> "Markdown"
                                DocumentType.PLAIN_TEXT -> "Text"
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(),
                )
                Text(
                    text = DateFormat
                        .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(recent.lastOpenedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove from recents",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * [ActivityResultContracts.OpenDocument] variant that additionally requests
 * FLAG_GRANT_WRITE_URI_PERMISSION so the picked document can be saved back.
 * Providers that don't grant write will still return a readable URI; the
 * caller falls back to a read-only persistable permission.
 */
private class OpenDocumentRW : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return super.createIntent(context, input).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            )
        }
    }
}

private fun queryNameAndMime(
    context: Context,
    uri: Uri,
): Pair<String, String?> {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri)
    var name: String? = null
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
    }
    if (name.isNullOrBlank()) {
        name = uri.lastPathSegment?.substringAfterLast('/') ?: "Untitled"
    }
    return name!! to mime
}
